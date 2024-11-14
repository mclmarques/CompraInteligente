package com.mcldev.comprainteligente.ui

import androidx.lifecycle.ViewModel
import com.mcldev.comprainteligente.data.ProductDao
import com.mcldev.comprainteligente.data.SupermarketDao
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class ScanScreenVM (
/*    private val productDao: ProductDao,
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
                }
                .addOnFailureListener {
                    // TODO: Handle error
                }
        }
    }

}