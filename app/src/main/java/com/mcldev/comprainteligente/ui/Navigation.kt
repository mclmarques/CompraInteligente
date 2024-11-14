package com.mcldev.comprainteligente.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object Receipts : Screen("receipts")
    object Settings : Screen("settings")
}
