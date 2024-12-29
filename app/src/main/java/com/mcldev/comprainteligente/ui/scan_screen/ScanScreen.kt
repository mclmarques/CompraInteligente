package com.mcldev.comprainteligente.ui.scan_screen

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mcldev.comprainteligente.ui.util.CameraPermissionHandler
import com.mcldev.comprainteligente.ui.util.checkAndRequestPermission
import java.io.File


@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // States for image capture
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Camera launcher for capturing images
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            imageUri.value?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                imageBitmap = bitmap
                viewModel.processImage(bitmap)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Back button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    // Permission handling and camera access
    CameraPermissionHandler(
        dialogTitle = "Camera Permission Needed",
        dialogText = "Camera access is required to scan the receipt. Please grant permission.",
        icon = Icons.Default.Info,
        onDialogConfirmation = { /* Optional: Action if dialog is confirmed */ },
        onPermissionResult = { isGranted ->
            if (isGranted) {
                // Launch camera if permission is granted
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(context.cacheDir, "temp_image.jpg") // Temp file for image capture
                )
                imageUri.value = uri
                cameraLauncher.launch(uri)
            }
        }
    )
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

