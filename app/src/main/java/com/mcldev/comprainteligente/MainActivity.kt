package com.mcldev.comprainteligente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mcldev.comprainteligente.data.DataBase
import com.mcldev.comprainteligente.ui.home_screen.HomeScreen
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVM
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVmFactory
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreen
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.util.Screen
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme

class MainActivity : ComponentActivity() {
    private val database: DataBase by lazy {
        Application.getDatabase()
    }
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Create the ViewModel with the DAOs
        val homeScreenViewModel = ViewModelProvider(
            this,
            HomeScreenVmFactory(database.productDao(), database.supermarketDao())
        ).get(HomeScreenVM::class.java)

        val scanScreenVM = ScanScreenVM(
            context = this
        )

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
                Surface {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) { HomeScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                        composable(Screen.Scan.route) { ScanScreen(modifier = Modifier, viewModel = scanScreenVM, navController = navController) }
                        //composable(Screen.Receipts.route) { ReceiptsScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                        //composable(Screen.Settings.route) { SettingsScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                    }
                }
            }
        }
    }
}
