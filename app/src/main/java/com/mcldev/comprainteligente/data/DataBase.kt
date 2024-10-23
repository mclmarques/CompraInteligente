package com.mcldev.comprainteligente.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Product::class, Supermarket::class], version = 1)
@TypeConverters(Converter::class)
abstract class DataBase : RoomDatabase(){
    abstract fun productDao(): ProductDao
    abstract fun supermarketDao(): SupermarketDao
}