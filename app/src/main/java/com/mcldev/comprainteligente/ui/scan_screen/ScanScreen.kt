package com.mcldev.comprainteligente.ui.scan_screen

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.io.File


@Composable
fun ScanScreen(
    viewModel: ScanScreenVM = viewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // States for image capture

    // Camera launcher for capturing images
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isCameraError by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            imageUri?.let { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { viewModel.processImage(it) }
                navController.popBackStack() // Navigate back after a successful capture
            }
        } else {
            isCameraError = true
        }
    }

    LaunchedEffect(Unit) {
        try {
            val uri = context.createImageFile()
            imageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            isCameraError = true
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
        // Error message
        if (isCameraError) {
            Text(
                text = "Camera app is not available or failed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

fun Context.createImageFile(): Uri {
    val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var index = sharedPreferences.getInt("last_receipt_index", 0)
    index++

    val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    if (storageDir != null && !storageDir.exists()) {
        storageDir.mkdirs() // Ensure the directory exists
    }
    val newFile = File(storageDir, "receipt$index.jpg")

    // Save the new index
    sharedPreferences.edit().putInt("last_receipt_index", index).apply()
    return androidx.core.content.FileProvider.getUriForFile(
        this,
        "${applicationContext.packageName}.provider",
        newFile
    )
}


