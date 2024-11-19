package com.mcldev.comprainteligente.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch


class ScanScreenVM (
    private val context: Context
    /*private val productDao: ProductDao,
      private val supermarketDao: SupermarketDao*/
): ViewModel() {
    var extractedText: String = ""
        private set

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Process the captured image bitmap
    fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        viewModelScope.launch {
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    extractedText = visionText.text // Extract and save the recognized text
                    Log.d("TextRecognition", "Extracted Text: $extractedText") // Log the extracted text
                }
                .addOnFailureListener {
                    Log.d("debug", "Error analysing the image with ML")
                }
        }
    }

}