package com.mcldev.comprainteligente.ui.scan_screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.googlecode.tesseract.android.TessBaseAPI
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/*
TODO:
1. Configuration changes re call the scanner, even after the process been completed
2. Remove black screen while processing and use the proper screen
3. post-process the text
 */
class ScanScreenVM(private val path: String?) : ViewModel() {
    private var imageUri: Uri? = null

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _contents = MutableStateFlow<String?>(null)
    val contents: StateFlow<String?> = _contents


    fun prepareScanner(): GmsDocumentScanner {
        //Scanner stuff
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
        return GmsDocumentScanning.getClient(options)
    }

    /**
     * TOOD: Fix the issue causing the processImage emthod not wait for the loadAndSaveBitmap method to end
     */
    fun processImage(context: Context, uri: Uri) {
        var rawText: String? = null
        _processingState.value = ProcessingState.Loading
        if(uri.path != null) {
            imageUri = uri
            var bitmap: Bitmap? = null
            viewModelScope.launch() {
                viewModelScope.launch(Dispatchers.IO) {
                    bitmap = loadAndSaveBitmap(
                        uri = uri,
                        context = context
                    )
                }.join()
                if (bitmap != null) {
                    viewModelScope.launch {
                        rawText = performOCR(bitmap!!)
                        rawText = rawText?.let { postProcessOCRText(it) }
                    }.join()
                    _contents.value = rawText
                }
                else {
                    ocrFault()
                    Log.e("debug", "Fault extracting the image!")
                }
            }
        } else Log.e("debug", _contents.value?:"Fault! Image URI was null")

    }

    private suspend fun loadAndSaveBitmap(uri: Uri, context: Context): Bitmap? {
        return withContext(Dispatchers.IO) {
            val bitmap: Bitmap?
            val inputStream = context.contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Save the bitmap to a file
            val saveUri = context.createImageFile()
            try {
                context.contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Log.i("debug", "Image saved successfully at $saveUri")
            } catch (e: IOException) {
                Log.e("debug", "Error saving image: ${e.message}")
            }

            bitmap // Return the loaded Bitmap
        }
    }


    /**
     * @param image: the bitmap to extract the text from
     * @return: extracted text from the image
     * The method uses the tesserat API to extract the text. IT ASSUMES THE IMAGE IS ALREADY PREPROCESSED
     */
    private suspend fun performOCR(image: Bitmap): String? {
        return withContext(Dispatchers.Default) {
            val tessBaseAPI: TessBaseAPI?
            // setup tessBaseApi
            tessBaseAPI = TessBaseAPI()
            tessBaseAPI.init(path, "por") // or other languages
            Log.i("Tesseract", "Tesseract engine initialized successfully")
            tessBaseAPI.setImage(image)
            tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
            val recognizedText = tessBaseAPI.utF8Text
            Log.i("OCR", recognizedText)
            tessBaseAPI.recycle() // keep engine alive for multiple uses
            recognizedText
        }
    }

    private suspend fun postProcessOCRText(ocrText: String): String {
        return withContext(Dispatchers.Default) {
            // Split the text into lines
            val lines = ocrText.lines()
            println("Lines extracted from OCR text: $lines")

            // Extract supermarket name (assume it's the second line)
            val supermarketName = lines[1]
            println("Supermarket name extracted: $supermarketName")

            // Extract date and time (formatted as dd/MM/yy hh:mm:ss)
            val dateRegex = Regex("""\d{2}/\d{2}/\d{2} \d{2}:\d{2}:\d{2}""")
            val date = dateRegex.find(lines[5])
            if (date != null) {
                println("Date extracted: ${date.value}")
            }

            val productRegex = Regex(""""(\w{13}|\w{5}|\w{8}|\w{14}|\w{15})\s(\w+\s?){1,5}""")
            val priceRegex = Regex("""(\d{1,3}[.,]\d{2})""")
            for(line in 7 .. (lines.size - 2) step 2) {
                Log.i("post-process", "Found product: " + (productRegex.find(lines[line])?.value ?:"Fault! Null product"))
                Log.i("post-process", "Found price: " + (productRegex.find(lines[(line + 1)])?.value ?:"Fault! Null price"))
            }
            ""
        }
    }


    /**
     * Method to trigger a camera error launch and make the UI show the appropriate err code
     */
    fun cameraLaunchFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.CAMERA_ERROR)
    }
    /**
     * Method to trigger a storage error launch and make the UI show the appropriate err code
     */
    fun storageFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.DATA_SAVE_ERROR)
    }

    fun ocrFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR)
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
    return FileProvider.getUriForFile(
        this,
        "${applicationContext.packageName}.provider",
        newFile
    )
}

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Loading : ProcessingState()
    data object Complete : ProcessingState()
    data class Error(val code: ErrorCodes) : ProcessingState()
}