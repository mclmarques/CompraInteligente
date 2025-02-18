package com.mcldev.comprainteligente.ui.home_screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcldev.comprainteligente.R
import com.mcldev.comprainteligente.data.Supermarket
import com.mcldev.comprainteligente.ui.util.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen made of a scaffold containing a search bar at the top, lazy column filling the screen showing the average prices
 * of the supermarkets and a dynamic FAB that is used to add products by scanning a receipt or delete the selected supermarkets
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeScreenVM,
    navController: NavHostController
) {
    //search
    val searchText by viewModel.searchText.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val padding by animateDpAsState(targetValue = if (expanded) 0.dp else 8.dp)
    val keyboardController = LocalSoftwareKeyboardController.current

    //little menu (3 dots). Disabled for the release of v1.0 but expected to be enabled in future releases
    var showMenu by remember { mutableStateOf(false) }

    //Supermarkets shown in the lazy column
    val supermarkets by viewModel.supermarkets.collectAsState()

    //selection mode stuff + animation for the dynamic FAB
    val selectionMode = viewModel.selectionMode.value

    // Rotation Animation
    val rotationAngle by animateFloatAsState(
        targetValue = if (selectionMode) 0f else 180f, // Rotate to 0 for Delete, 180 for Add
        label = "Rotation Animation"
    )
    //Locale stuff (As the first version is launched on Brazil if is optimized to that country Locale). On future releases with support to more countries this is likely to change
    val brazilLocale = Locale("pt", "BR")
    val currencyFormatter = NumberFormat.getCurrencyInstance(brazilLocale)
    /*
    This is used to "restore the default back gesture" which normally exists the app, but as I modify it
    so when selection mode is enabled it would cancel the selection and go back to normal mode, this is needed
    for it to have the default behaviour
     */
    val activity = LocalActivity.current

    BackHandler {
        if (selectionMode) {
            viewModel.toggleSelectionMode()
        } else {
            activity?.finish()
        }
    }

    Scaffold(floatingActionButton = {
        FloatingActionButton(
            onClick = {
                if (selectionMode) {
                    viewModel.deleteSelectedItems()
                } else {
                    navController.navigate(Screen.Scan.route)
                }
            },
            modifier = Modifier.padding(8.dp),
            containerColor = if (selectionMode) Color.Red else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (selectionMode) Icons.Default.Delete else Icons.Default.Add,
                contentDescription = if (selectionMode) stringResource(R.string.delete_supermarket_cd) else stringResource(
                    R.string.scan_receipt_cd
                ),
                modifier = Modifier.graphicsLayer(
                    rotationZ = rotationAngle
                ),
                tint = Color.Black
            )
        }
    },
        modifier = modifier.clickable { if (selectionMode) viewModel.toggleSelectionMode() }
    ) { innerPadding ->
        Column(modifier = modifier) {
            SearchBar(
                modifier = Modifier
                    .padding(horizontal = padding)
                    .fillMaxWidth(),
                inputField = {
                    SearchBarDefaults.InputField(
                        onSearch = {
                            keyboardController?.hide()

                        },
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = it
                        },
                        placeholder = { Text(stringResource(R.string.search)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.clickable {showMenu = !showMenu})

                            AnimatedVisibility(
                                visible = showMenu,
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut() + slideOutVertically { it / 2 }
                            ) {
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        text = { Text("Settings") },
                                        onClick = {
                                            showMenu = false
                                            navController.navigate(Screen.Settings.route)
                                        }
                                    )
                                    //TODO: in future releases, implement the receipts screen and enable this code to reach that screen
                                    /*DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Receipts"
                                            )
                                        },
                                        text = { Text("Receipts") },
                                        onClick = {
                                            showMenu = false
                                            // Navigate to Receipts screen
                                        }
                                    )*/
                                }
                            }
                        },
                        query = searchText,
                        onQueryChange = { viewModel.onSearchTextChange(it) }
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(text = product.productName)
                                        Row (verticalAlignment = Alignment.CenterVertically){
                                            Text(text = product.supermarketName)
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(product.date)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                    }
                                    Text(text = currencyFormatter.format(product.price))
                                }
                            }

                        }
                    }
                } else {
                    Text(
                        text = "No results found",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }
            }
            /*
            Show all the supermarkets on the db and their averages.
            A color scheme was added, were the cheapest is green and the most expensive red. In between values have no color difference
             */
            LazyColumn(modifier = modifier.padding(innerPadding)) {
                val minPrice = supermarkets.minOfOrNull { it.averagePrice } ?: 0.0
                val maxPrice = supermarkets.maxOfOrNull { it.averagePrice } ?: 0.0


                items(supermarkets) { supermarket: Supermarket ->
                    val priceColor = when (supermarket.averagePrice) {
                        minPrice -> Color(0xFF008000)
                        maxPrice -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurface

                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleItemSelection(supermarket)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        viewModel.toggleSelectionMode()
                                        viewModel.toggleItemSelection(supermarket)
                                    } else {
                                        viewModel.selectionMode.value = false
                                        viewModel.clearSelection()
                                    }


                                }
                            ),
                        colors = CardDefaults.cardColors(
                            if (viewModel.selectedItems.contains(supermarket)) Color.LightGray else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = supermarket.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currencyFormatter.format(supermarket.averagePrice),
                                style = MaterialTheme.typography.bodyLarge,
                                color = priceColor
                            )
                        }
                    }
                }
            }
        }
    }
}