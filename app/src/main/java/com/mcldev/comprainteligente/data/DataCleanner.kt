package com.mcldev.comprainteligente.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class DataCleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ProductDao // Injetado via Koin
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val retentionPeriod = getRetentionPeriodFromPreferences()
        if (retentionPeriod == Long.MAX_VALUE) return Result.success() // "Nunca" excluir dados

        val cutoffDate = calculateCutoffDate(retentionPeriod)

        val deleteAllData = getDeleteAllDataPreference()
        if (deleteAllData) {
            val listProducts = repository.getAllProducts()
            for(product in listProducts) {
                repository.timeBasedDelete(cutoffDate)
            }
        } else {
            //TODO: repository.deleteOldImages(cutoffDate) // Apenas remove imagens antigas
        }

        return Result.success()
    }

    private fun getRetentionPeriodFromPreferences(): Long {
        val sharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val periodIndex = sharedPreferences.getInt("retention_period", 1) // Padrão: 3 meses

        return when (periodIndex) {
            0 -> TimeUnit.DAYS.toMillis(30)    // 1 month
            1 -> TimeUnit.DAYS.toMillis(90)    // 3 month
            2 -> TimeUnit.DAYS.toMillis(365)   // 1 year
            3 -> TimeUnit.DAYS.toMillis(730)   // 2 years
            4 -> Long.MAX_VALUE                // Never exclude
            else -> TimeUnit.DAYS.toMillis(90) // Default safety value
        }
    }

    private fun calculateCutoffDate(retentionPeriod: Long): Long {
        val now = System.currentTimeMillis()
        return now - retentionPeriod
    }

    private fun getDeleteAllDataPreference(): Boolean {
        val sharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("delete_all_data", true) // Padrão: deletar tudo
    }

}
