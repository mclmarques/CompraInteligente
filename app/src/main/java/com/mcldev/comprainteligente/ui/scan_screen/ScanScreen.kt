package com.mcldev.comprainteligente.ui.scan_screen

import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mcldev.comprainteligente.ui.util.Screen
import com.mcldev.comprainteligente.ui.util.checkAndRequestPermission
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale


@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier,
    activityRefence: Activity
) {
    // States for permission and captured image
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Context for permission check
    val context =  LocalContext.current

    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    // Permission launcher updated to check if permission is permanently denied
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        permissionPermanentlyDenied = !isGranted && !shouldShowRequestPermissionRationale(
            activityRefence,
            Manifest.permission.CAMERA
        ) // Detect if permanently denied
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            viewModel.processImage(bitmap)
        }
    }

    //State to avoid launching the camera multiple times
    var isCameraLaunched by remember { mutableStateOf(false) }

    // Check camera permission when the screen is displayed
    LaunchedEffect(Unit) {
        cameraPermissionGranted = checkAndRequestPermission(
            context = context,
            permission = Manifest.permission.CAMERA,
            permissionLauncher = permissionLauncher
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
        ,
    ) {
        if (cameraPermissionGranted) {
            if (!isCameraLaunched) {
                isCameraLaunched = true
                cameraLauncher.launch(null)
            }

        } else {
            // Show permission request UI when permission is not granted
            /*
            TODO: when the user comes back from the settings app, the app needs to refreshy and re-check if the permissions was granted or not.
            As of rightnow the when you come back from settings the app doesn't react to the change and still propmts the user to grant
            the permission
             */
            PermissionRequestUI(
                permissionPermanentlyDenied = permissionPermanentlyDenied, // Pass new state
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenSettings = {
                    // Opens the app settings when user clicks on Open Settings button
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onBack = { navController.navigate(Screen.Home.route) }
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp, // Add shadow/elevation for better visibility
            modifier = Modifier
                .padding(8.dp) // Add padding around the button for spacing
        ) {
            IconButton(
                onClick = {navController.navigate(Screen.Home.route)},
                modifier = Modifier.size(40.dp) // Adjust size to match the circular shape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Back arrow icon
                    contentDescription = "Go Back",
                    tint = MaterialTheme.colorScheme.primary // Match the theme
                )
            }
        }
    }
}


@Composable
fun PermissionRequestUI(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit, // Callback to open settings,
    permissionPermanentlyDenied: Boolean, // New parameter for permission state
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()) // Respect status bar height
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp, // Add shadow/elevation for better visibility
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp) // Add padding around the button for spacing
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.size(40.dp) // Adjust size to match the circular shape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Back arrow icon
                    contentDescription = "Go Back",
                    tint = MaterialTheme.colorScheme.primary // Match the theme
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Add margins on the sides
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permissionPermanentlyDenied) {
                // Message shown when permission is permanently denied
                Text(
                    text = "Camera permission is required to scan the receipt. Please enable it in the app settings.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Button to open app settings
                Button(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            } else {
                // Message and button shown when permission can still be requested
                Text(
                    text = "Camera permission is required to scan the receipt.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

