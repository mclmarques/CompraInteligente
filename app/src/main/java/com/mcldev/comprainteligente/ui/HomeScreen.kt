package com.mcldev.comprainteligente.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


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
    Column (
        modifier = Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true })
    {

        SearchBar(
            modifier = Modifier
                .semantics { traversalIndex = 0f }
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
                        Text(
                            text = product.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Define what happens when a search result is clicked
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "No results found",
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                )
            }
        }
        LazyColumn (
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(searchResults) { product ->
                Row {
                    Text(text = "TODO: Implement method translate SupermarketID into it's name")
                    Spacer(modifier = Modifier.width(32.dp))
                    //TODO: Add a method to compare the price against the other products
                    Text(text = product.price.toString() + " " + product.unit)
                }

            }
        }
    }
}