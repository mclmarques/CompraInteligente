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

/**
 * Unit tests for [ScanScreenVM], focused on verifying:
 * - Product and supermarket insertion in the Room in-memory database.
 * - Proper handling of missing supermarket scenarios.
 * - Updates to supermarket average prices.
 * - Correct deletion and update of items in the ViewModel.
 *
 * This test class uses:
 * - In-memory Room database for isolation (no disk writes).
 * - Kotlin coroutines and `runBlocking` for testing suspend functions.
 * - JUnit4 lifecycle methods (`@Before` / `@After`) for setup and teardown.
 *
 * **Test Scenarios:**
 *
 * 1. **[saveProducts_shouldInsertProductsAndSupermarket]**
 *    - Inserts products and a supermarket into the in-memory database.
 *    - Verifies:
 *      - All products are correctly persisted.
 *      - Supermarket is created in the database.
 *
 * 2. **[saveProducts_withNullSupermarket_shouldTriggerOcrFault]**
 *    - Calls `saveProducts()` without selecting a supermarket.
 *    - Expects:
 *      - `ProcessingState.Error` with code [ErrorCodes.TEXT_EXTRACTION_ERROR].
 *
 * 3. **[saveProducts_shouldUpdateSupermarketAveragePrice]**
 *    - Pre-seeds a supermarket with an initial product and average price.
 *    - Adds new products via the ViewModel.
 *    - Verifies that the supermarket's `averagePrice` is updated correctly.
 *
 * 4. **[deleteItem_shouldRemoveProductAndPrice]**
 *    - Populates the ViewModel with 2 products and prices.
 *    - Deletes the first one and verifies:
 *      - Product and price lists are updated.
 *      - Remaining entries are correct.
 *
 * 5. **[updateProduct_shouldUpdateCorrectIndex]**
 *    - Initializes the ViewModel with a single product.
 *    - Updates the product name and price separately.
 *    - Ensures that the correct indices are updated in the internal lists.
 */

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
