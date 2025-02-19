package com.mcldev.comprainteligente

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.work.*
import com.mcldev.comprainteligente.data.repository.DataCleanupWorker
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
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()

        val cleanupWorkRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            7, TimeUnit.DAYS
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "data_cleanup_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupWorkRequest
        )
    }
}