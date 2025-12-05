package com.mcldev.comprainteligente

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
import com.mcldev.comprainteligente.ui.AppNavigation
import com.mcldev.comprainteligente.ui.home_screen.HomeScreen
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVM
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreen
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreen
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreenVM
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import com.mcldev.comprainteligente.ui.util.Screen
import com.mcldev.comprainteligente.util.StartupChecker
import com.mcldev.comprainteligente.util.StartupResult
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel
import kotlin.system.exitProcess
import android.os.Process
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompraInteligenteTheme {
                Surface {
                    val startupChecker: StartupChecker by inject()
                    val result = startupChecker.checkDevice()
                    when (result) {
                        is StartupResult.Error -> {
                            AlertDialog(
                                onConfirmation = {
                                    finishAndRemoveTask()
                                    Process.killProcess(Process.myPid())
                                    exitProcess(0)
                                },
                                errCode = result.code,
                                icon = R.drawable.warning_ic
                            )
                        }

                        is StartupResult.Success -> {
                            AppNavigation()
                        }
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
