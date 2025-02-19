package com.mcldev.comprainteligente.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import com.mcldev.comprainteligente.data.entities.Product
import com.mcldev.comprainteligente.data.entities.Supermarket

@Database(entities = [Product::class, Supermarket::class], version = 1)
@TypeConverters(Converter::class)
abstract class DataBase : RoomDatabase(){
    abstract fun productDao(): ProductDao
    abstract fun supermarketDao(): SupermarketDao
}