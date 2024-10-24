package com.mcldev.comprainteligente.ui


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcldev.comprainteligente.data.ProductDao
import com.mcldev.comprainteligente.data.SupermarketDao

class HomeScreenVmFactory (
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao
): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenVM::class.java)) {
            return HomeScreenVM(productDao, supermarketDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}