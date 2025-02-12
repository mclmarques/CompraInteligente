package com.mcldev.comprainteligente

import android.app.Application
import androidx.room.Room
import com.mcldev.comprainteligente.data.DataBase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }
}