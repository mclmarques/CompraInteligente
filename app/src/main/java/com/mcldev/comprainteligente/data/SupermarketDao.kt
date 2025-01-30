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
    fun getAllSupermarkets(): List<Supermarket>

    @Query("SELECT * FROM supermarket WHERE id = :supermarketId LIMIT 1")
    fun getSupermarketById(supermarketId: Int): Supermarket?

    @Query("SELECT * FROM supermarket WHERE name = :supermarketName")
    fun getSupermarketByName(supermarketName: String): Supermarket?
}