package com.mcldev.comprainteligente

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.database.DataBase
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * The idea here is to perform basic checks around the backend. Here is tested the following items:
 * Upsert a product
 * Search by name a product
 * Time based delete method (only the method, the worker and other components are NOT tested)
 * Upsert an existing product
 * Try to add a product linked to non existing supermarket
 * In general edge cases or wrong values aren't tested here as they should be checked by the ViewModel (ScanScreenVM.kt)
 */
@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: DataBase
    private lateinit var productDao: ProductDao
    private lateinit var supermarketDao: SupermarketDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DataBase::class.java
        ).allowMainThreadQueries().build()

        productDao = db.productDao()
        supermarketDao = db.supermarketDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertProduct_andRetrieveByName_shouldReturnCorrectResult() = runBlocking {
        val supermarket = Supermarket(name = "Test Market", averagePrice = 0.0f)
        supermarketDao.upsertSupermarket(supermarket)

        val insertedSupermarket = supermarketDao.getSupermarketByName("Test Market")!!
        val product = Product(
            name = "Milk",
            price = 3.5f,
            unit = "L",
            supermarketId = insertedSupermarket.id,
            date = System.currentTimeMillis()
        )
        productDao.upsertProduct(product)

        val result = productDao.getProductsByName("Milk")
        Assert.assertEquals(1, result.size)
        Assert.assertEquals("Milk", result[0].name)
    }

    @Test
    fun timeBasedDelete_shouldRemoveOldEntries() = runBlocking {
        val supermarket = Supermarket(name = "Oldies", averagePrice = 0.0f)
        supermarketDao.upsertSupermarket(supermarket)
        val s = supermarketDao.getSupermarketByName("Oldies")!!

        val oldProduct = Product(
            name = "Expired",
            price = 1.0f,
            unit = "g",
            supermarketId = s.id,
            date = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(400)
        )
        val freshProduct = oldProduct.copy(name = "Fresh", date = System.currentTimeMillis())

        productDao.upsertProduct(oldProduct)
        productDao.upsertProduct(freshProduct)

        productDao.timeBasedDelete(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365))

        val allProducts = productDao.getAllProducts()
        Assert.assertEquals(1, allProducts.size)
        Assert.assertEquals("Fresh", allProducts.first().name)
    }
    @Test
    fun upsertExistingProduct_shouldUpdateFields() = runBlocking {
        val supermarket = Supermarket(name = "Update Mart", averagePrice = 0.0f)
        supermarketDao.upsertSupermarket(supermarket)
        val s = supermarketDao.getSupermarketByName("Update Mart")!!

        // First insert
        val product = Product(
            name = "Rice",
            price = 5.0f,
            unit = "kg",
            supermarketId = s.id,
            date = System.currentTimeMillis()
        )
        productDao.upsertProduct(product)

        // Fetch inserted product to get its generated ID
        val initial = productDao.getProductsByName("Rice").first()

        // Modify and re-upsert
        val updated = initial.copy(price = 6.0f, unit = "g")
        productDao.upsertProduct(updated)

        val result = productDao.getProductsByName("Rice").first()
        assertEquals(initial.id, result.id)
        assertEquals(6.0f, result.price)
        assertEquals("g", result.unit)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun insertProductWithInvalidSupermarket_shouldThrowException() = runBlocking {
        val product = Product(
            name = "Ghost",
            price = 1.0f,
            unit = "unit",
            supermarketId = 9999, // doesn't exist
            date = System.currentTimeMillis()
        )

        // This should fail due to foreign key constraint
        productDao.upsertProduct(product)
    }
}