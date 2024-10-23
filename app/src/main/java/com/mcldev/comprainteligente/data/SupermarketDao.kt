package com.mcldev.comprainteligente.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Upsert

@Dao
interface SupermarketDao {
    @Upsert
    suspend fun upsertSupermarket(supermarket: Supermarket)

    @Delete
    suspend fun deleteSupermarket(supermarket: Supermarket)

    @Query("SELECT * FROM supermarket")
    suspend fun getAllSupermarkets(): List<Supermarket>

    @Query("SELECT * FROM supermarket WHERE id = :supermarketId LIMIT 1")
    suspend fun getSupermarketById(supermarketId: Int): Supermarket?
}