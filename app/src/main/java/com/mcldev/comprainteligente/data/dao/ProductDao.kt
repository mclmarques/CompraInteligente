package com.mcldev.comprainteligente.data.dao
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mcldev.comprainteligente.data.entities.Product

@Dao
interface ProductDao {
    @Upsert
    suspend fun upsertProduct(product: Product)

    @Query("SELECT * FROM product WHERE name LIKE '%' || :productName || '%'")
    fun getProductsByName(productName: String): List<Product>

    @Query("DELETE FROM product WHERE date < :expirationTime")
    suspend fun timeBasedDelete(expirationTime: Long)

    @Query("SELECT * FROM PRODUCT")
    suspend fun getAllProducts(): List<Product>


}