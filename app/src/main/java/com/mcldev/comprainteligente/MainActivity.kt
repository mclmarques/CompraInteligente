package com.mcldev.comprainteligente

import android.app.ActivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mcldev.comprainteligente.ui.home_screen.HomeScreen
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVM
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreen
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreen
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreenVM
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import com.mcldev.comprainteligente.ui.util.Screen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompraInteligenteTheme {
                Surface {
                    // Check RAM amount and initializes Tesseract folder
                    val actManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    val memInfo = ActivityManager.MemoryInfo()
                    actManager.getMemoryInfo(memInfo)
                    val totalMemory = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
                    val path by inject<String>()

                    if (totalMemory < 2) {
                        AlertDialog(
                            onConfirmation = {finish()},
                            errCode =  ErrorCodes.UNSUPPORTED_DEVICE_ERROR_1,
                            icon = R.drawable.warning_ic
                        )
                    }
                    else  if(path.isEmpty()) {
                        AlertDialog(
                            onConfirmation = {finish()},
                            errCode =  ErrorCodes.UNSUPPORTED_DEVICE_ERROR_2,
                            icon = R.drawable.warning_ic
                        )
                    } else {
                        //Launches the app
                        val homeScreenVM = getViewModel<HomeScreenVM>()
                        val scanScreenVM = getViewModel<ScanScreenVM>()
                        val settingsScreenVM = getViewModel<SettingsScreenVM>()
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    modifier = Modifier,
                                    viewModel = homeScreenVM,
                                    navController = navController
                                )
                            }
                            composable(Screen.Scan.route) {
                                ScanScreen(
                                    viewModel = scanScreenVM,
                                    navController = navController
                                )
                            }
                            //composable(Screen.Receipts.route) { ReceiptsScreen(modifier = Modifier, viewModel = homeScreenViewModel, navController = navController) }
                            composable(Screen.Settings.route) { SettingsScreen(navController, settingsScreenVM) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Displays an error alert dialog with a warning icon, a title, and an optional message.
     *
     * This composable presents an error dialog based on the provided `ErrorCodes` enum.
     * It includes a title, an optional error message, and a confirmation button labeled "Exit."
     *
     * @param onConfirmation Callback invoked when the user confirms the dialog.
     * @param errCode The error code representing the message and title to be displayed. See util -> error_codes.md for more info
     * @param icon The resource ID of the icon to be displayed in the dialog.
     */
    @Composable
    fun AlertDialog(
        onConfirmation: () -> Unit,
        errCode: ErrorCodes,
        icon: Int,
    ) {
        AlertDialog(

            icon = {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "Warning icon",
                    modifier = Modifier.size(48.dp) // Make the icon bigger
                )
            },
            title = {
                Text(
                    text = stringResource(errCode.titleResId),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                // Error message (if available)
                errCode.messageResId?.let { messageResId ->
                    val message = stringResource(messageResId)
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmation()
                    }
                ) {
                    Text("Exit") // Change the text to "Exit"
                }
            },
        )
    }
}