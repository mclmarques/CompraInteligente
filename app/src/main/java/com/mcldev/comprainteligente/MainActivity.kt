package com.mcldev.comprainteligente

import android.app.ActivityManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme
import com.mcldev.comprainteligente.ui.util.Screen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.tasks.Task
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime

class MainActivity : ComponentActivity() {
    private val database: DataBase by lazy {
        Application.getDatabase()
    }
    //ML stuff
    val initializeTask: Task<Void> by lazy { TfLite.initialize(this) }
    private lateinit var interpreter: InterpreterApi
    val useGpuTask = TfLiteGpu.isGpuDelegateAvailable(context)


    fun initializeAi() {
        initializeTask.addOnSuccessListener {
            val interpreterOption =
                InterpreterApi.Options().setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
            interpreter = InterpreterApi.create(
                modelBuffer,
                interpreterOption
            )}
            .addOnFailureListener { e ->
                Log.e("Interpreter", "Cannot initialize interpreter", e)
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Create the ViewModel with the DAOs
        val homeScreenViewModel = ViewModelProvider(
            this,
            HomeScreenVmFactory(database.productDao(), database.supermarketDao())
        ).get(HomeScreenVM::class.java)

        val scanScreenVM = ScanScreenVM()

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
                    // Check how much RAM is installed on the phone before launching the app
                    val actManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    val memInfo = ActivityManager.MemoryInfo()
                    actManager.getMemoryInfo(memInfo)
                    val totalMemory= memInfo.totalMem.toDouble()/(1024*1024*1024)
                    if(totalMemory < 2 ) {
                        AlertDialog(
                            onDismissRequest = {},
                            onConfirmation = {},
                            dialogTitle = stringResource(R.string.err_unsupported_title),
                            dialogText = stringResource(R.string.err_unsupported),
                            icon = R.drawable.warning_ic
                        )
                    }
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

    @Composable
    fun AlertDialog(
        onDismissRequest: () -> Unit,
        onConfirmation: () -> Unit,
        dialogTitle: String,
        dialogText: String,
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
                    text = dialogTitle,
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
                Text(
                    text = dialogText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp) // Reduce padding slightly
                )
            },
            onDismissRequest = {
                onDismissRequest()
            },
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