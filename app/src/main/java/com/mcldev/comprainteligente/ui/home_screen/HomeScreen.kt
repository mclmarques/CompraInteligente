package com.mcldev.comprainteligente.ui.home_screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcldev.comprainteligente.data.Supermarket
import com.mcldev.comprainteligente.ui.util.Screen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeScreenVM,
    navController: NavHostController
) {
    val searchText by viewModel.searchText.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val padding by animateDpAsState(targetValue = if (expanded) 0.dp else 8.dp)
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMenu by remember { mutableStateOf(false) } // For menu visibility
    val supermarkets by viewModel.supermarkets.collectAsState()
    val selectionMode = viewModel.selectionMode.value
    val selectedItems = viewModel.selectedItems
    // Rotation Animation
    val rotationAngle by animateFloatAsState(
        targetValue = if (selectionMode) 0f else 180f, // Rotate to 0 for Delete, 180 for Add
        label = "Rotation Animation"
    )
    //val activity = LocalContext.current as? Activity

    BackHandler {
        if (selectionMode) {
            viewModel.toggleSelectionMode()
        }
        else {
            //activity?.finish()
        }
    }

    Scaffold (floatingActionButton = {
        FloatingActionButton(
            onClick = { navController.navigate(Screen.Scan.route) },
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }){ innerPadding ->
        Column (modifier = modifier){
            SearchBar(
                modifier = Modifier
                    .padding(horizontal = padding)
                    .fillMaxWidth(),
                inputField = {
                    SearchBarDefaults.InputField(
                        onSearch = {
                            //expanded = false
                            keyboardController?.hide()

                        },
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = it
                        },
                        placeholder = { Text("Buscar") },
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
                                            // Navigate to Settings screen
                                        }
                                    )
                                    DropdownMenuItem(
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
                                    )
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = product.productName)
                                    Text(text = String.format("%.2f R$", product.price))
                                }
                                Text(text = product.supermarketName, modifier = Modifier.padding(8.dp))
                            }
                            
                        }
                    }
                } else {
                    Text(
                        text = "No results found",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                    )
                }
            }
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
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = supermarket.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = String.format("%.2f R$", supermarket.averagePrice ?: 0f),                                style = MaterialTheme.typography.bodyLarge,
                                color = priceColor
                            )
                        }
                    }
                }
            }
        }
    }
}