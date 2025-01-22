package com.mcldev.comprainteligente.ui.scan_screen

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.mcldev.comprainteligente.R
import com.mcldev.comprainteligente.ui.util.ErrorCodes


@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val processingState by viewModel.processingState.collectAsState()
    val contents by viewModel.contents.collectAsState()
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
                    Log.e("debug", "Scanner returnned null")
                }
            } else {
                viewModel.cameraLaunchFault()
                Log.e("debug", "Scanner failed to open")
            }
        })
    LaunchedEffect(Unit) {
        viewModel.prepareScanner().getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                viewModel.cameraLaunchFault()
                Log.e("debug", "Scanner failed to open")

            }

    }
    if(processingState is ProcessingState.Error){
        val errorCode = (processingState as ProcessingState.Error).code
        ErrorScreen(
            navController = navController,
            errCode = errorCode
        )
    }
    else {
        when(contents) {
            null -> {
                LoadingScreen()
            }
            else -> {
                ListOfItems(
                    navController = navController,
                    contents = contents!!
                )
            }
        }
    }
}

@Composable
fun ListOfItems(
    navController: NavHostController,
    contents: String
) {
    Text(text = contents)
}

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

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Back button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            IconButton(
                onClick = { /*navController.popBackStack()*/ },
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


