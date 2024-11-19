package com.mcldev.comprainteligente.ui.util


import android.content.Context
import androidx.activity.result.ActivityResultLauncher
/**
 * Checks if a permission is granted. If not, requests the permission.
 *
 * @param context The context for checking the permission status.
 * @param permission The permission to check/request (e.g., Manifest.permission.CAMERA).
 * @param permissionLauncher The launcher for requesting the permission.
 * @return True if the permission is already granted; false otherwise.
 */
fun checkAndRequestPermission(
    context: Context,
    permission: String,
    permissionLauncher: ActivityResultLauncher<String>
): Boolean {
    val isGranted = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(permission)
    if (!isGranted) {
        permissionLauncher.launch(permission) // Request the permission
    }
    return isGranted
}