package com.mcldev.comprainteligente.ui

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mcldev.comprainteligente.ui.util.checkAndRequestPermission


@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // States for permission and captured image
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Context for permission check
    val context =  LocalContext.current

    // Permission and Camera launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            viewModel.processImage(bitmap)
        }
    }

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
            cameraLauncher.launch(null)

        } else {
            // Show permission request UI when permission is not granted
            PermissionRequestUI(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onBack = {navController.popBackStack()}
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
                onClick = {navController.popBackStack()},
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
fun PermissionRequestUI(onRequestPermission: () -> Unit, onBack: () -> Unit) {
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
            // Warning text
            Text(
                text = "Camera permission is required to scan the receipt.",
                color = MaterialTheme.colorScheme.error, // Use error color for warning tone
                style = MaterialTheme.typography.titleLarge.copy( // Bold and large text
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp) // Add spacing below the text
            )

            // Permission button
            Button(onClick = { onRequestPermission() }) {
                Text("Grant Permission")
            }
        }
    }
}

