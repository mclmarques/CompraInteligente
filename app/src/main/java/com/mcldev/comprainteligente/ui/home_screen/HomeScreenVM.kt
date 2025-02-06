package com.mcldev.comprainteligente.ui.home_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcldev.comprainteligente.data.Product
import com.mcldev.comprainteligente.data.ProductDao
import com.mcldev.comprainteligente.data.Supermarket
import com.mcldev.comprainteligente.data.SupermarketDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenVM(
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao
): ViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<searchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _supermarkets = MutableStateFlow<List<Supermarket>>(emptyList())
    val supermarkets: StateFlow<List<Supermarket>> = _supermarkets

    var selectionMode = mutableStateOf(false)
        private set

    private val _selectedItems = mutableStateListOf<Supermarket>()
    val selectedItems: List<Supermarket> get() = _selectedItems

    init {
        getSupermarkets()
    }

    fun toggleSelectionMode() {
        selectionMode.value = !selectionMode.value
        if (!selectionMode.value) clearSelection()
    }

    private fun getSupermarkets() {
        viewModelScope.launch(Dispatchers.IO) {  // Run in background thread
            val result = supermarketDao.getAllSupermarkets()
            _supermarkets.value = result  // Update StateFlow on the main thread
        }
    }

    fun toggleItemSelection(supermarket: Supermarket) {
        if (_selectedItems.contains(supermarket)) {
            _selectedItems.remove(supermarket)
        } else {
            _selectedItems.add(supermarket)
        }
    }

    fun clearSelection() {
        _selectedItems.clear()
    }

    fun deleteSelectedItems() {
        viewModelScope.launch (Dispatchers.IO) {
            _selectedItems.forEach { supermarketDao.deleteSupermarket(it) }
            clearSelection()
            toggleSelectionMode()
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value  = text
        performSearch(text)
    }

    fun performSearch(query: String) {
        val results: MutableList<searchResult> = mutableListOf()
        viewModelScope.launch (Dispatchers.IO) {
            delay(300)
            if (query.isNotEmpty()) {
                _isSearching.value = true
                // Fetch results from database
                val products = productDao.getProductsByName(query)
                var supermarket: Supermarket?
                var supermarketName: String?
                for (product in products) {
                    if(product.supermarketId != null) {
                        supermarket = supermarketDao.getSupermarketById(product.supermarketId)
                        supermarketName = supermarket?.name ?: "Supermercado sem nome"
                        results.add(searchResult(product.name,product.price,supermarketName))

                    }
                }
                _searchResults.value = results
                _isSearching.value = false
            } else {
                _searchResults.value = emptyList() // Clear results when query is empty
            }
        }
    }

    fun onToogleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            onSearchTextChange("")
        }
    }

}

data class searchResult(
    val productName: String,
    val price: Float,
    val supermarketName: String
)