package com.mcldev.comprainteligente.ui.scan_screen

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mcldev.comprainteligente.R
import com.mcldev.comprainteligente.ui.util.ErrorCodes

/**
 * @param viewModel: an instance of ScanScreenVM
 * @param navController: an instance of the navController used to navigate back to the home screen
 * This composable renders the UI of the scan screen.
 * It works by first, launching the google document scanner utility and later on passing it's contents
 * so the viewmodel can process them and finally display the results or possible errors
 * All the process is done using Strings and Floats, and once the user confirms the selection of products,
 * a method of the viewmodel converts the strings and prices to actual Product objects and saves them
 */
//TODO: Maybe add a FAB to add products that weren't picked up by the scanner
@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
) {
    val context = LocalContext.current
    val processingState by viewModel.processingState.collectAsState()
    val products by viewModel.products.collectAsState()
    val prices by viewModel.prices.collectAsState()
    val supermarket by viewModel.supermarket.collectAsState()

    //Scanner stuff
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) {
                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                if (result != null) {
                    result.pages?.let { pages ->
                        viewModel.processImage(context = context, uri = pages[0].imageUri)
                    }
                } else {
                    viewModel.ocrFault()
                }
            } else {
                viewModel.cameraLaunchFault()
            }
        })
    //Launches scanner upon opening this screen
    LaunchedEffect(Unit) {
        viewModel.prepareScanner().getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                viewModel.cameraLaunchFault()

            }

    }
    //Call the needed composable depending on the current state
    when (processingState) {
        ProcessingState.Complete -> ListOfItems(
            products = products,
            prices = prices,
            supermarket = supermarket ?: stringResource(R.string.supermarket_not_found),
            updateProduct = { newName, newPrice, position ->
                viewModel.updateProduct(position, newName, newPrice)
            },
            saveProducts = {
                viewModel.saveProducts()
                navController.popBackStack()
            },
            deleteProduct = { position ->
                viewModel.deleteItem(position)
            },
            updateSupermarket = { newSupermarket ->
                viewModel.updateSupermarket(newSupermarket)
            },
            cancel = {navController.popBackStack()}
        )

        is ProcessingState.Error -> {
            val errorCode = (processingState as ProcessingState.Error).code
            ErrorScreen(
                navController = navController,
                errCode = errorCode
            )
        }

        ProcessingState.Idle -> LoadingScreen()
        ProcessingState.Loading -> LoadingScreen()
    }
}

/**
 * @param products: list of products to show.
 * @param prices: list of the prices
 * @param updateProduct: how to update one of the scanned item (price or product description).
 * @param saveProducts: lambda to save the products into the DB
 * @param deleteProduct: lambda that discard a specific item
 * this method also guarantees:
 * Supermarket name is not empty
 * All products have a name
 * All products have a valid price***
 * Note: as the checks and updates are performed when the focus is lost, it is possible to enter valid a value, for example, 10.0,
 * and change it to -10.0 and just hide the keyboard, as this don't change the focus. However, as the values are also updated on focus change,
 * the value is never updated, so the old valid value will be saved instead (in this case, 10.0)
 */
@Composable
fun ListOfItems(
    products: List<String>,
    prices: List<Float>,
    supermarket: String,
    updateSupermarket: (newName: String) -> Unit,
    updateProduct: (product: String?, price: Float?, position: Int) -> Unit,
    saveProducts: () -> Unit,
    deleteProduct: (position: Int) -> Unit,
    cancel: () -> Unit
) {
    var isSupermarketValid by remember { mutableStateOf(supermarket.isNotEmpty()) }

    // Derived state: Ensures all products are non-empty and prices are > 0.0
    val validProducts by remember(products, prices) {
        derivedStateOf {
            products.all { it.isNotEmpty() } && prices.all { it > 0.0f }
        }
    }

    Scaffold(
        floatingActionButton = {
            Row {
                Button(
                    onClick = saveProducts,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.70f)),
                    enabled = isSupermarketValid && validProducts // Uses computed validation
                ) {
                    Icon(
                        painter = painterResource(R.drawable.confirm_24),
                        contentDescription = "Confirm"
                    )
                }

                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = cancel,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.70f))
                ) {
                    Icon(painter = painterResource(R.drawable.cancel_24), stringResource(R.string.cancel_cd))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            var supermarketName by remember(supermarket) { mutableStateOf(supermarket) }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supermercado: ")
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    modifier = Modifier.weight(0.7f),
                    value = supermarketName,
                    onValueChange = {
                        supermarketName = it
                        isSupermarketValid = supermarketName.isNotEmpty()
                        if (isSupermarketValid) updateSupermarket(supermarketName)
                    },
                    isError = !isSupermarketValid,
                    supportingText = {
                        if (!isSupermarketValid) {
                            Text(
                                stringResource(R.string.supermarket_not_found),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true
                )
            }

            LazyColumn {
                items(products.size) { item ->
                    var productName by remember(products[item]) { mutableStateOf(products[item]) }
                    var productPrice by remember(prices[item]) { mutableStateOf(prices[item].toString()) }
                    val focusManager = LocalFocusManager.current
                    val currencyTransformation = VisualTransformation { text ->
                        TransformedText(
                            text = AnnotatedString("$text R$"),
                            offsetMapping = object : OffsetMapping {
                                override fun originalToTransformed(offset: Int) = offset
                                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(text.length)
                            }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .onFocusEvent { focusState ->
                                        if (!focusState.isFocused) {
                                            updateProduct(productName, null, item)
                                        }
                                    },
                                value = productName,
                                onValueChange = {
                                    productName = it
                                },
                                isError = productName.isEmpty(),
                                supportingText = {
                                    if (productName.isEmpty()) {
                                        Text(
                                            stringResource(R.string.product_without_name),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardActions = KeyboardActions({
                                    focusManager.clearFocus()
                                }),
                            )

                            Spacer(Modifier.width(32.dp))

                            TextField(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .onFocusEvent { focusState ->
                                        if (!focusState.isFocused) {
                                            productPrice = productPrice.replace(",", ".")
                                            updateProduct(null, productPrice.toFloatOrNull() ?: 0.0f, item)
                                        }
                                    },
                                value = productPrice,
                                onValueChange = {

                                    productPrice = it
                                },
                                isError = productPrice.toFloatOrNull() == null || productPrice.toFloat() == 0.0f,
                                supportingText = {
                                    if (productPrice.toFloatOrNull() == null || productPrice.toFloat() == 0.0f) {
                                        Text(
                                            stringResource(R.string.product_wrong_price),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions({
                                    focusManager.clearFocus()
                                }),
                                visualTransformation = currencyTransformation,

                            )

                            Spacer(Modifier.width(16.dp))

                            Button(
                                onClick = { deleteProduct(item) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(0.75f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    painterResource(R.drawable.trash_24),
                                    contentDescription = "Delete item"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * @param navController: navController instance used to go back to the home screen
 * @param errCode: used to determine what went wrong. Check util package to see more about errors
 * Simple composable that display the main error and possible fixes. It also has a back button at the
 * top to go back to the home screen
 */
@Composable
fun ErrorScreen(
    navController: NavHostController,
    errCode: ErrorCodes
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        //.padding(32.dp), // Add padding to avoid edge-to-edge text
        //verticalArrangement = Arrangement.Center
    ) {
        // Back button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Error title
            val title = stringResource(errCode.titleResId)
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                ),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message (if available)
            errCode.messageResId?.let { messageResId ->
                val message = stringResource(messageResId)
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
            if (errCode.errCode == 1) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally) // Center the button horizontally
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

/**
 * Basic loading screen that displays a circular infinite loading
 * In future releases will be revamped to include progress loading bar
 */

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Back button

            Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .fillMaxHeight()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.processing))

        }
        }
}


