package com.mcldev.comprainteligente.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Upsert
import com.mcldev.comprainteligente.data.entities.Supermarket
import kotlinx.coroutines.flow.Flow

@Dao
interface SupermarketDao {
    @Upsert
    suspend fun upsertSupermarket(supermarket: Supermarket)

    @Delete
    suspend fun deleteSupermarket(supermarket: Supermarket)

    @Query("SELECT * FROM supermarket")
    fun getAllSupermarkets(): Flow<List<Supermarket>>

    @Query("SELECT * FROM supermarket WHERE id = :supermarketId LIMIT 1")
    fun getSupermarketById(supermarketId: Int): Supermarket?

    @Query("SELECT * FROM supermarket WHERE name = :supermarketName")
    fun getSupermarketByName(supermarketName: String): Supermarket?

    @Query("SELECT COUNT(*) FROM product WHERE supermarketId = :supermarketId")
    suspend fun getProductCount(supermarketId: Int): Int

    @Query("DELETE FROM supermarket WHERE id IN (SELECT id FROM supermarket WHERE id NOT IN (SELECT DISTINCT supermarketId FROM product))")
    suspend fun deleteEmptySupermarkets()
}