package com.mcldev.comprainteligente.ui.scan_screen

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScanScreenVM(
    private val context: Context
) : ViewModel() {

    // Camera permission state
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission

    // Extracted text state
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    /**
     * Checks and requests camera permission. Uses an ActivityResultLauncher to request the permission.
     */
    fun checkAndRequestCameraPermission(launcher: ActivityResultLauncher<String>) {
        val permission = android.Manifest.permission.CAMERA

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            _hasCameraPermission.update { true } // Permission granted
        } else {
            // Request the permission
            launcher.launch(permission)
        }
    }

    /**
     * Updates the camera permission state based on the result of the permission request.
     */
    fun onCameraPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.update { isGranted }
    }

    /**
     * Process the image bitmap to extract text.
     */
    fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                _extractedText.update { visionText.text } // Update state with recognized text
                Log.d("TextRecognition", "Extracted Text: ${visionText.text}")
            }
            .addOnFailureListener {
                Log.e("TextRecognition", "Error analyzing the image with ML", it)
            }
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}
