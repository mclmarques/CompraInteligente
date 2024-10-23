package com.mcldev.comprainteligente.data
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProductDao {
    @Upsert
    suspend fun upsertProduct(product: Product)

    @Query("SELECT * FROM product WHERE name = :productName")
    suspend fun getProductsByName(productName: String): List<Product>

    @Query("SELECT * FROM product WHERE supermarketId = :supermarketId")
    suspend fun getProductsBySupermarket(supermarketId: Int): List<Product>

    @Upsert
    suspend fun upsertSupermarket(supermarket: Supermarket)

    @Query("SELECT * FROM supermarket WHERE name = :supermarketName")
    suspend fun getSupermarketByName(supermarketName: String): Supermarket?
}