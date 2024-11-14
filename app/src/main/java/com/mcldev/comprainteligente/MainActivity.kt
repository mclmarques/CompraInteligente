package com.mcldev.comprainteligente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mcldev.comprainteligente.data.DataBase
import com.mcldev.comprainteligente.ui.HomeScreen
import com.mcldev.comprainteligente.ui.HomeScreenVM
import com.mcldev.comprainteligente.ui.HomeScreenVmFactory
import com.mcldev.comprainteligente.ui.Screen
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme

class MainActivity : ComponentActivity() {
    // Access the AppDatabase from MyApp class
    private val database: DataBase by lazy {
        Application.getDatabase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Create the ViewModel with the DAOs
        val homeScreenViewModel = ViewModelProvider(
            this,
            HomeScreenVmFactory(database.productDao(), database.supermarketDao())
        ).get(HomeScreenVM::class.java)
        /*lifecycleScope.launch {
            database.productDao().upsertProduct(Product(
                id = 3,
                name = "Laranja",
                price = 4.78,
                unit = "R$/Kg",
                supermarketId = 1))

        }*/

        setContent {
            CompraInteligenteTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) { HomeScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController)}
                    //composable(Screen.Scan.route) { ScanScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                    //composable(Screen.Receipts.route) { ReceiptsScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                    //composable(Screen.Settings.route) { SettingsScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                }
            }
        }
    }
}
