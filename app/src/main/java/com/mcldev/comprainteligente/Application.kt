package com.mcldev.comprainteligente

import android.app.Application
import androidx.room.Room
import com.mcldev.comprainteligente.data.DataBase

class Application : Application() {
    companion object {
        private var dbInstance: DataBase ? = null

        fun getDatabase(): DataBase {
            return dbInstance!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the Room database here
        dbInstance = Room.databaseBuilder(
            applicationContext,
            DataBase::class.java,
            "product_supermarket_db"
        ).build()
    }
}