package com.mcldev.comprainteligente.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SupermarketDao {
    @Upsert
    suspend fun upsertSupermarket(supermarket: Supermarket)

    @Delete
    suspend fun deleteSupermarket(supermarket: Supermarket)

    @Query("SELECT * FROM supermarket")
    fun getAllSupermarkets(): Flow<List<Supermarket>> //used this to get live updates on the homescreen

    @Query("SELECT * FROM supermarket WHERE id = :supermarketId LIMIT 1")
    fun getSupermarketById(supermarketId: Int): Supermarket?

    @Query("SELECT * FROM supermarket WHERE name = :supermarketName")
    fun getSupermarketByName(supermarketName: String): Supermarket?

}