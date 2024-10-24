package com.mcldev.comprainteligente.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier) {
    Column (
        modifier = Modifier.statusBarsPadding(), // Fix to respect the status bar's padding
    ) {
        Row (
            modifier = modifier.fillMaxWidth(), // Ensures content does not overlap the status bar,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // Ensure buttons and SearchBar are vertically aligned
        ) {
            FilledIconButton(onClick = { /* doSomething() */ }, ) {
                Icon(Icons.Filled.Settings , contentDescription = "Settings")
            }


            SearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                active = false,
                onActiveChange = {},
                enabled = true,
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.weight(1f)
            ) { }

            FilledIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Menu , contentDescription = "List of tickets")
            }

        }
        LazyColumn (modifier = Modifier.fillMaxSize().weight(5f)){
            items(5) { index ->
                Text(text = "Item: $index")
            }

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() {
    CompraInteligenteTheme {
        HomeScreen(modifier = Modifier)
    }
}