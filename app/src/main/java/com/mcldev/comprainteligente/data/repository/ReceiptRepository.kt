package com.mcldev.comprainteligente.data.repository

import android.content.Context
import android.net.Uri
import com.mcldev.comprainteligente.data.model.ScannedProduct
import com.mcldev.comprainteligente.ui.scan_screen.ProcessingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ReceiptRepository {
    val products: StateFlow<List<ScannedProduct>>
    val supermarketName: StateFlow<String?>
    
    fun processImage(context: Context, uri: Uri): Flow<ProcessingState>
    suspend fun saveProducts(products: List<ScannedProduct>, supermarketName: String?): Result<Unit>
    fun clearResults()
    fun updateProduct(productId: String, name: String?, price: Float?)
    fun deleteProduct(productId: String)
    fun updateSupermarket(name: String)
}
