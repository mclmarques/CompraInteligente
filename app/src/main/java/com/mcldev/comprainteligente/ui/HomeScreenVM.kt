package com.mcldev.comprainteligente.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcldev.comprainteligente.data.Product
import com.mcldev.comprainteligente.data.ProductDao
import com.mcldev.comprainteligente.data.SupermarketDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    fun onSearchTextChange(text: String) {
        _searchText.value  = text
        performSearch(text)
    }

    fun performSearch(query: String) {
        viewModelScope.launch {
            // Simulate debounce logic (wait for a pause in typing)
            delay(300)

            if (query.isNotEmpty()) {
                _isSearching.value = true
                // Fetch results from database
                val results = productDao.getProductsByName(query)
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