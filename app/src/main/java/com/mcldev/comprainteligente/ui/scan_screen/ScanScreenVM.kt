package com.mcldev.comprainteligente.ui.scan_screen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_BASE
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.mcldev.comprainteligente.data.model.ScannedProduct
import com.mcldev.comprainteligente.data.repository.ReceiptRepository
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanScreenVM(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    val products: StateFlow<List<ScannedProduct>> = receiptRepository.products
    val supermarket: StateFlow<String?> = receiptRepository.supermarketName

    fun prepareScanner(): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_BASE)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setGalleryImportAllowed(true)
            .build()
        return GmsDocumentScanning.getClient(options)
    }

    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            receiptRepository.processImage(context, uri).collect { state ->
                _processingState.value = state
            }
        }
    }

    fun deleteItem(productId: String) {
        receiptRepository.deleteProduct(productId)
    }

    fun saveProducts() {
        viewModelScope.launch {
            val result = receiptRepository.saveProducts(products.value, supermarket.value)
            if (result.isSuccess) {
                _processingState.value = ProcessingState.Complete
            } else {
                _processingState.value = ProcessingState.Error(ErrorCodes.DATA_SAVE_ERROR)
            }
        }
    }

    fun updateProduct(productId: String, name: String?, price: Float?) {
        receiptRepository.updateProduct(productId, name, price)
    }

    fun updateSupermarket(name: String) {
        receiptRepository.updateSupermarket(name)
    }

    fun cameraLaunchFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.CAMERA_ERROR)
    }

    fun storageFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.UNSUPPORTED_DEVICE_ERROR_2)
    }

    fun ocrFault() {
        _processingState.value = ProcessingState.Error(ErrorCodes.TEXT_EXTRACTION_ERROR)
    }
}
