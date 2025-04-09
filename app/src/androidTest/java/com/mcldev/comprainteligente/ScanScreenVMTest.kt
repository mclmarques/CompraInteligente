package com.mcldev.comprainteligente

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mcldev.comprainteligente.data.database.DataBase
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.scan_screen.ProcessingState
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class ScanScreenVMTest {

    private lateinit var viewModel: ScanScreenVM
    private lateinit var db: DataBase
    private lateinit var productDao: ProductDao
    private lateinit var supermarketDao: SupermarketDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DataBase::class.java
        ).allowMainThreadQueries().build()

        productDao = db.productDao()
        supermarketDao = db.supermarketDao()

        viewModel = ScanScreenVM(
            path = null, // not testing OCR
            productDao = productDao,
            supermarketDao = supermarketDao
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveProducts_shouldInsertProductsAndSupermarket() = runBlocking {
        viewModel.updateSupermarket("MyTestSupermarket")

        // Directly manipulate internal state for testing
        viewModel.products.value.apply {
            clear()
            addAll(listOf("Bread", "Cheese"))
        }
        viewModel.prices.value.apply {
            clear()
            addAll(listOf(4.99f, 7.50f))
        }

        viewModel.saveProducts()
        delay(200)

        val products = productDao.getAllProducts()
        val supermarket = supermarketDao.getSupermarketByName("MyTestSupermarket")

        assertEquals(2, products.size)
        assertEquals("Bread", products[0].name)
        assertNotNull(supermarket)
    }


    @Test
    fun saveProducts_withNullSupermarket_shouldTriggerOcrFault() = runBlocking {
        viewModel.saveProducts()
        delay(200)

        val state = viewModel.processingState.value
        assertTrue(state is ProcessingState.Error)
        assertEquals(ErrorCodes.TEXT_EXTRACTION_ERROR, (state as ProcessingState.Error).code)
    }

    @Test
    fun saveProducts_shouldUpdateSupermarketAveragePrice() = runBlocking {
        // Step 1: Insert supermarket and 1 linked product
        val supermarket = Supermarket(name = "AverageTest", averagePrice = 10f)
        supermarketDao.upsertSupermarket(supermarket)
        val inserted = supermarketDao.getSupermarketByName("AverageTest")!!

        val initialProduct = Product(
            name = "Banana",
            price = 10f,
            supermarketId = inserted.id,
            date = System.currentTimeMillis()
        )
        productDao.upsertProduct(initialProduct)

        // Step 2: Add new products through the ViewModel
        viewModel.updateSupermarket("AverageTest")

        viewModel.products.value.addAll(listOf("Apple", "Pear"))
        viewModel.prices.value.addAll(listOf(6f, 4f))

        viewModel.saveProducts()
        delay(200)

        // Step 3: Assert updated average
        val updated = supermarketDao.getSupermarketByName("AverageTest")!!
        val expectedAvg = ((10f * 1) + 6f + 4f) / (1 + 2)
        assertEquals(expectedAvg, updated.averagePrice, 0.001f)
    }


    @Test
    fun deleteItem_shouldRemoveProductAndPrice() {
        // Seed products and prices
        viewModel.products.value.apply {
            clear()
            addAll(listOf("ToRemove", "Keep"))
        }
        viewModel.prices.value.apply {
            clear()
            addAll(listOf(5f, 10f))
        }

        viewModel.deleteItem(0)

        val products = viewModel.products.value
        val prices = viewModel.prices.value

        assertEquals(1, products.size)
        assertEquals("Keep", products.first())
        assertEquals(10f, prices.first())
    }


    @Test
    fun updateProduct_shouldUpdateCorrectIndex() {
        // Initialize
        viewModel.products.value.apply {
            clear()
            add("Old")
        }
        viewModel.prices.value.apply {
            clear()
            add(1.0f)
        }

        viewModel.updateProduct(0, "Updated", null)
        assertEquals("Updated", viewModel.products.value[0])

        viewModel.updateProduct(0, null, 2.5f)
        assertEquals(2.5f, viewModel.prices.value[0])
    }

}
