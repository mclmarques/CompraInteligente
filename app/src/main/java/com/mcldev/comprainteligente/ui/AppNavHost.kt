package com.mcldev.comprainteligente.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mcldev.comprainteligente.ui.home_screen.HomeScreen
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVM
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreen
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreen
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreenVM
import com.mcldev.comprainteligente.ui.util.Screen
import org.koin.androidx.compose.koinViewModel


@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // --- HOME ---
        composable(Screen.Home.route) {
            val vm: HomeScreenVM = koinViewModel()

            HomeScreen(
                viewModel = vm,
                navController = navController
            )
        }

        // --- SCAN ---
        composable(Screen.Scan.route) {
            val vm: ScanScreenVM = koinViewModel()

            ScanScreen(
                viewModel = vm,
                navController = navController
            )
        }

        // --- SETTINGS ---
        composable(Screen.Settings.route) {
            val vm: SettingsScreenVM = koinViewModel()

            SettingsScreen(
                navController = navController,
                viewModel = vm
            )
        }
    }
}