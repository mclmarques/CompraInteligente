package com.mcldev.comprainteligente.ui.util

import kotlinx.serialization.Serializable


@Serializable
sealed class Screen(val route: String) {
    @Serializable
    object Home : Screen("home")
    @Serializable
    object Scan : Screen("scan")
    @Serializable
    object Receipts : Screen("receipts")
    @Serializable
    object Settings : Screen("settings")
}
