package com.mcldev.comprainteligente.ui.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun CameraPermissionHandler(
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
    onDialogConfirmation: () -> Unit = {}, // Optional confirmation action
    onPermissionResult: (Boolean) -> Unit // Callback to send the permission result to the ViewModel
) {
    val context = LocalContext.current
    val permission = Manifest.permission.CAMERA
    var showDialog by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) } // Ensure we only launch once

    // Request permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted) // Notify ViewModel of the result
        if (!isGranted) showDialog = true
    }

    // Check if permission is already granted
    if (!permissionChecked) {
        permissionChecked = true
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            onPermissionResult(true)
        } else {
            launcher.launch(permission)
        }
    }

    // Show dialog if permission is denied
    if (showDialog) {
        CameraPermissionDialog(
            onDismissRequest = { showDialog = false },
            onConfirmation = {
                onDialogConfirmation()
                showDialog = false
                // Guide the user to settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            dialogTitle = dialogTitle,
            dialogText = dialogText,
            icon = icon
        )
    }
}
