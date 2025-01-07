package com.mcldev.comprainteligente.ui.scan_screen

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ScanScreenVM() : ViewModel() {
    // Extracted text state
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    /**
     * Process the image bitmap to extract text.
     */
    fun processImage(bitmap: Bitmap) {
        _processingState.value = ProcessingState.Loading
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                _extractedText.update { visionText.text } // Update state with recognized text
                Log.d("TextRecognition", "Extracted Text: ${visionText.text}")
            }
            .addOnFailureListener {
                _processingState.value = ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR)
            }
    }

    fun cameraLaunchFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.CAMERA_ERROR)
    }
    fun storgeFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.DATA_SAVE_ERROR)

    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Loading : ProcessingState()
    object Complete : ProcessingState()
    data class Error(val code: ErrorCodes) : ProcessingState()
}