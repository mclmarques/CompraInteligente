package com.mcldev.comprainteligente.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mcldev.comprainteligente.data.dao.ProductDao
import com.mcldev.comprainteligente.data.dao.SupermarketDao
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class DataCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {
    private val productDao: ProductDao by inject()
    private val supermarketDao: SupermarketDao by inject()


    override suspend fun doWork(): Result {
        val retentionPeriod = getRetentionPeriodFromPreferences()
        Log.d("DataCleanupWorker", "Retention period: $retentionPeriod")
        if (retentionPeriod == Long.MAX_VALUE) return Result.success() // Never delete any data

        val cutoffDate = calculateCutoffDate(retentionPeriod)

        val deleteAllData = getDeleteAllDataPreference()
        if (deleteAllData) {
            val listProducts = productDao.getAllProducts()
            for(product in listProducts) {
                productDao.timeBasedDelete(cutoffDate)
            }
            supermarketDao.deleteEmptySupermarkets()
            //Delete images
            val storageDir: File? = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null || !storageDir.exists()) {
                Log.d("DataCleanupWorker", "Image directory not found, skipping deletion.")
                return Result.failure()
            }

            val imageFiles = storageDir.listFiles { file ->
                file.name.startsWith("receipt") && file.extension == "jpg"
            } ?: return Result.failure()

            for (file in imageFiles) {
                if (file.lastModified() < cutoffDate) {
                    val deleted = file.delete()
                    Log.d("DataCleanupWorker", "Deleting image: ${file.name}, Success: $deleted")
                }
            }
        } else {
            val storageDir: File? = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null || !storageDir.exists()) {
                Log.d("DataCleanupWorker", "Image directory not found, skipping deletion.")
                return Result.failure()
            }

            val imageFiles = storageDir.listFiles { file ->
                file.name.startsWith("receipt") && file.extension == "jpg"
            } ?: return Result.failure()

            for (file in imageFiles) {
                if (file.lastModified() < cutoffDate) {
                    val deleted = file.delete()
                    Log.d("DataCleanupWorker", "Deleting image: ${file.name}, Success: $deleted")
                }
            }
        }
        return Result.success()
    }

    private fun getRetentionPeriodFromPreferences(): Long {
        val sharedPreferences = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val periodIndex = sharedPreferences.getInt("retention_period", 1) // if fails, use default value of 3 months
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
        return sharedPreferences.getBoolean("delete_all_data", true) // Default: clear all data
    }

}
