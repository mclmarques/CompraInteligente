package com.mcldev.comprainteligente.ui.home_screen

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.entities.Supermarket
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewModel for managing the home screen state and user interactions.
 *
 * This ViewModel handles:
 * - Supermarket management (fetching, deleting)
 * - Product search functionality
 * - Selection mode (off by default), for when the user wants to delete a supermarket. NOTE: deleting a supermarket deletes all products that are linked to it
 *
 * @param productDao DAO for product-related database operations.
 * @param supermarketDao DAO for supermarket-related database operations.
 */
class HomeScreenVM(
    private val productDao: ProductDao,
    private val supermarketDao: SupermarketDao
) : ViewModel() {
    //search
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private var isSearching = false

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    //supermarkets
    private val _supermarkets = MutableStateFlow<List<Supermarket>>(emptyList())
    val supermarkets: StateFlow<List<Supermarket>> = _supermarkets

    //selection mode
    private val _selectionMode = MutableStateFlow<Boolean>(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedItems = mutableStateListOf<Supermarket>()
    val selectedItems: List<Supermarket> get() = _selectedItems

    //Locale stuff (As the first version is launched on Brazil, the locale is optimized to that country Locale). On future releases with support to more countries this is likely to change
    val brazilLocale = Locale("pt", "BR")
    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(brazilLocale)

    //collects flow from the db to react to changes (like scanning a receipt or removing a supermarket)
    init {
        viewModelScope.launch(Dispatchers.Main) {
            supermarketDao.getAllSupermarkets().collect { supermarketsList ->
                _supermarkets.value = supermarketsList
            }
        }
    }

    //selection mode stuff
    fun toggleSelectionMode() {
        _selectionMode.value = !selectionMode.value
        if (!selectionMode.value) clearSelection()
    }

    /*
    If selection is empty it auto disables selection mode.
    This only happens after the user selects at least 1 supermarket, and then unselect it
     */
    fun toggleItemSelection(supermarket: Supermarket) {
        if (_selectedItems.contains(supermarket)) {
            _selectedItems.remove(supermarket)
            if (selectedItems.isEmpty()) toggleSelectionMode()
        } else {
            _selectedItems.add(supermarket)
        }
    }

    fun clearSelection() {
        _selectedItems.clear()
    }

    /*
    The delete operations happens in the background and this action auto disables the selection mode
     */
    fun deleteSelectedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            val itemsToDelete = _selectedItems.toList() // Copy to avoid concurrent modification
            itemsToDelete.forEach { supermarketDao.deleteSupermarket(it) }

            withContext(Dispatchers.Main) {
                clearSelection()
                toggleSelectionMode()
            }
        }
    }


    //Search
    fun onSearchTextChange(text: String) {
        _searchText.value = text
        performSearch(text)
    }

    /**
     * Performs a search for products based on the query string.
     *
     * This function:
     * - Checks if the query is empty (clears results if so)
     * - Fetches products matching the name
     * - Maps each product to its corresponding supermarket
     * - Updates the search results list asynchronously
     * - uses a 500 delay to give time to the user to type and don't overload the phone with search queries
     *
     * @param query The user input search query.
     */
    private fun performSearch(query: String) {
        val results: MutableList<SearchResult> = mutableListOf()
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            if (query.isNotEmpty()) {
                isSearching = true
                // Fetch results from database
                val products = productDao.getProductsByName(query)
                var supermarket: Supermarket?
                var supermarketName: String?
                //map each product to it's corresponding supermarket
                for (product in products) {
                    supermarket = supermarketDao.getSupermarketById(product.supermarketId)
                    //Only show complete search items. Though as supermarket name is mandatory on the db, this is an edge case
                    if (supermarket != null) {
                        supermarketName = supermarket.name
                        results.add(SearchResult(product.name, product.price, supermarketName, date = product.date))
                    }
                }
                _searchResults.value = results
                isSearching = false
            } else {
                _searchResults.value = emptyList() // Clear results when query is empty
            }
        }
    }

}

/*
Custom data type to handle search results more efficiently and make the code more readable.
Otherwise for each search result, there would be necessary to make a db search to convert the supermarket id into it's name
 */
data class SearchResult(
    val productName: String,
    val price: Float,
    val supermarketName: String,
    val date: Long
)