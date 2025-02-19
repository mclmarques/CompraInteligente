package com.mcldev.comprainteligente

import android.app.Application
import androidx.room.Room
import com.mcldev.comprainteligente.data.DataBase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.work.*
import com.mcldev.comprainteligente.data.DataCleanupWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
        scheduleDataCleanup()
    }

    private fun scheduleDataCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Não roda se a bateria estiver baixa
            .setRequiresDeviceIdle(true)    // Só executa quando o dispositivo não estiver em uso
            .build()

        val cleanupWorkRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            7, TimeUnit.DAYS // Executa uma vez por semana
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "data_cleanup_work",
            ExistingPeriodicWorkPolicy.UPDATE, // Atualiza o agendamento se já existir
            cleanupWorkRequest
        )
    }
}