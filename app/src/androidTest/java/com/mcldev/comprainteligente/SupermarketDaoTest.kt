package com.mcldev.comprainteligente

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.database.DataBase
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The idea here is to perform basic checks around the backend. Here is tested the following items:
 * Upsert a supermarket
 * Search a supermarket by it's ID
 * Delete empty supermarkets
 * Count number of produtcs linked to a supermarket
 *
 * In general edge cases or wrong values aren't tested here as they should be checked by the ViewModel (ScanScreenVM.kt)
 */
@RunWith(AndroidJUnit4::class)
class SupermarketDaoTest {

    private lateinit var db: DataBase
    private lateinit var supermarketDao: SupermarketDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DataBase::class.java
        ).allowMainThreadQueries().build()

        supermarketDao = db.supermarketDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertSupermarket_andQueryById_shouldReturnCorrectData() = runTest {
        val market = Supermarket(name = "Super Mart", averagePrice = 10f)
        supermarketDao.upsertSupermarket(market)

        val result = supermarketDao.getSupermarketByName("Super Mart")
        assertNotNull(result)
        assertEquals("Super Mart", result?.name)
    }

    @Test
    fun deleteEmptySupermarkets_shouldRemoveOrphanedEntries() = runTest {
        val orphaned = Supermarket(name = "Orphan Mart", averagePrice = 1f)
        supermarketDao.upsertSupermarket(orphaned)

        supermarketDao.deleteEmptySupermarkets()

        val result = supermarketDao.getSupermarketByName("Orphan Mart")
        assertNull(result)
    }

    @Test
    fun getProductCount_shouldReturnCorrectNumber() = runTest {
        val supermarket = Supermarket(name = "Count Mart", averagePrice = 5.0f)
        supermarketDao.upsertSupermarket(supermarket)
        val s = supermarketDao.getSupermarketByName("Count Mart")!!

        val productDao = db.productDao()
        repeat(3) {
            productDao.upsertProduct(
                Product(
                    name = "Product $it",
                    price = 1.0f,
                    supermarketId = s.id,
                    date = System.currentTimeMillis()
                )
            )
        }

        val count = supermarketDao.getProductCount(s.id)
        assertEquals(3, count)
    }
}