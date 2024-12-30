package com.mcldev.comprainteligente.ui.scan_screen

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ScanScreenVM() : ViewModel() {

    // Extracted text state
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

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
