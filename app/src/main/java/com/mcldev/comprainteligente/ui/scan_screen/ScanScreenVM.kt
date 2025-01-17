package com.mcldev.comprainteligente.ui.scan_screen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.googlecode.tesseract.android.TessBaseAPI
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class ScanScreenVM(private val path: String?) : ViewModel() {
    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _contents = MutableStateFlow<String?>(null)
    val contents: StateFlow<String?> = _contents


    //AI stuff (Tesserat)
    private var tessBaseAPI: TessBaseAPI? = null

    private fun performOCR(image: Bitmap): String? {
        // setup tessBaseApi
        tessBaseAPI = TessBaseAPI()
        tessBaseAPI!!.init(path, "por") // or other languages
        Log.i("Tesseract", "Tesseract engine initialized successfully")
        tessBaseAPI?.setImage(image)
        tessBaseAPI?.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO) // optional config
        val recognizedText = tessBaseAPI?.utF8Text
        tessBaseAPI?.recycle() // keep engine alive for multiple uses
        return recognizedText
    }
    /**
     * Process the image bitmap to extract text.
     */
    fun processImage(bitmap: Bitmap) {
        _processingState.value = ProcessingState.Loading
        viewModelScope.launch {
            _contents.value = performOCR(bitmap)?:"Fault"
        }

        Log.d("debug", _contents?.value?:"Fault!")
        _processingState.value = ProcessingState.Complete
    }

    fun cameraLaunchFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.CAMERA_ERROR)
    }
    fun storgeFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.DATA_SAVE_ERROR)
    }

    override fun onCleared() {
        super.onCleared()
    }
}

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Loading : ProcessingState()
    object Complete : ProcessingState()
    data class Error(val code: ErrorCodes) : ProcessingState()
}