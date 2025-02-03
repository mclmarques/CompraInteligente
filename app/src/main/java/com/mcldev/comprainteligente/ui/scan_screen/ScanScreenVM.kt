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
import com.mcldev.comprainteligente.data.Product
import com.mcldev.comprainteligente.data.ProductDao
import com.mcldev.comprainteligente.data.Supermarket
import com.mcldev.comprainteligente.data.SupermarketDao
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * @param path: path to load the data of tesseract
 * The Viewmodel is design to handle as much of the logic as possible and to main a clean architecture
 * It is organized like this:
 * Internal global variables
 * Method to intialize and perform OCR
 * Methods to work with data using the DAOs
 * Error methods to update the UI and display the err and possible solutions
 * Internal methods which in essence are the OCR operation, OCR helper methods and post-processing OCR
 */
class ScanScreenVM(
    private val path: String?,
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao
) : ViewModel() {
    private var imageUri: Uri? = null

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    private val _products = MutableStateFlow(mutableListOf<String>())
    val products = _products.asStateFlow()

    private val _prices = MutableStateFlow(mutableListOf<Float>())
    val prices = _prices.asStateFlow()

    private val _supermarket = MutableStateFlow<String?>(null)
    val supermarket = _supermarket.asStateFlow()


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
     * @param uri: URI of the photo to perform the OCR operation on
     * This methods using a coroutine will perform the OCR operation and update the ProcessingState once completed
     * It doesn't return anything because the data is already passed to the postProcessOCRText method that will auto save the data
     * into the internal golbal varaible.
     *
     *
     */
    fun processImage(context: Context, uri: Uri) {
        var rawText: String?
        _processingState.value = ProcessingState.Loading
        if (uri.path != null) {
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
                        rawText?.let { postProcessOCRText(it) }
                        _processingState.value = ProcessingState.Complete
                    }.join()
                } else {
                    ocrFault()
                }
            }
        } else storageFault()
    }

    //Data management (save, modify or edit products)
    fun deleteItem(position: Int) {
        if (position in _products.value.indices) {
            _products.value = _products.value.toMutableList().apply { removeAt(position) }
            _prices.value = _prices.value.toMutableList().apply { removeAt(position) }
        }
    }

    fun saveProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            var supermarketEntity: Supermarket?
            if (supermarket.value != null) {
                supermarketEntity = supermarketDao.getSupermarketByName(supermarketName = _supermarket.value!!)
                if (supermarketEntity != null) {
                    for (item in products.value.indices) {
                        val product = Product(
                            name = products.value[item],
                            price = prices.value[item],
                            supermarketId = supermarketEntity.id
                        )
                        productDao.upsertProduct(product)
                    }
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        supermarketDao.upsertSupermarket(Supermarket(name = supermarket.value!!))
                        supermarketEntity = supermarketDao.getSupermarketByName(supermarket.value!!)
                    }.join()
                    if(supermarketEntity != null) {
                        for (item in products.value.indices) {
                            val product = Product(
                                name = products.value[item],
                                price = prices.value[item],
                                supermarketId = supermarketEntity!!.id
                            )
                            productDao.upsertProduct(product)
                        }
                    }
                    storageFault()

                }
            }
            ocrFault()
        }
    }

    fun updateProduct(position: Int, newProduct: String? = null, newPrice: Float? = null) {
        if (position in _products.value.indices) {
            if(newProduct != null) {
                _products.value = _products.value.toMutableList().apply { this[position] = newProduct }
            }
            else if(newPrice != null) {
                _prices.value = _prices.value.toMutableList().apply { this[position] = newPrice }

            }
        }
    }

    fun updateSuprmarket(newSupermarket: String) {
        _supermarket.value = newSupermarket
    }


    //Error methods
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

    //Internal helper methods of the viewmodel


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
            } catch (e: IOException) {
                storageFault()
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
            tessBaseAPI.setImage(image)
            tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
            val recognizedText = tessBaseAPI.utF8Text
            tessBaseAPI.stop()
            tessBaseAPI.recycle()
            recognizedText
        }
    }

    private suspend fun postProcessOCRText(ocrText: String) {
        Log.i("post-process", "Post processing started!")
        viewModelScope.launch(Dispatchers.Default) {
            val lines = ocrText.lines()
            _supermarket.value = lines[1]
            val productRegex = Regex("""\d{6,14}\s+((\w+\s?){1,5})""") // Product code + up to 5 words
            val priceRegex = Regex("""(\d{1,3}[.,]\d{2})""")

            //Work internally on a copy
            val updatedProducts = _products.value.toMutableList()
            val updatedPrices = _prices.value.toMutableList()

            for (i in lines.indices) {
                val productMatch = productRegex.find(lines[i])
                if (productMatch != null) {
                    val productDescription = productMatch.groupValues[1].trim()

                    // Ensure the product isn't already added
                    if (!updatedProducts.contains(productDescription)) {
                        updatedProducts.add(productDescription)

                        // Find the price
                        val priceMatch = if (i + 1 < lines.size) priceRegex.find(lines[i + 1]) else null
                        val priceString = priceMatch?.value?.replace(",", ".")
                        val price = priceString?.toFloatOrNull() ?: 0.0f

                        updatedPrices.add(price)
                    }
                }
            }
            _products.value = updatedProducts
            _prices.value = updatedPrices
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