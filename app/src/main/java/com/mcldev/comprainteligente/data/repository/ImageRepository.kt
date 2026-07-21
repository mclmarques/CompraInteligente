package com.mcldev.comprainteligente.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ImageRepository(private val context: Context) {

    /**
     * Loads a bitmap from a URI and saves a copy to the app's internal storage.
     * Returns the Bitmap if successful, null otherwise.
     * Already runs on the separate IO thread
     */
    suspend fun loadAndSaveBitmap(uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver

                // 1. Decode the bitmap from the source URI
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // 2. Create a local file to save the copy using the indexed naming
                val saveUri = createImageFile()

                // 3. Save the bitmap to that local file
                if (saveUri != null && bitmap != null) {
                    contentResolver.openOutputStream(saveUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    bitmap // Return the loaded bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Helper to create a file in the app's pictures directory with indexed naming.
     * Returns a Uri pointing to the new empty file.
     */
    fun createImageFile(): Uri? {
        return try {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var index = sharedPreferences.getInt("last_receipt_index", 0)
            index++

            val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs()
            }
            val newFile = File(storageDir, "receipt$index.jpg")

            // Save the new index
            sharedPreferences.edit().putInt("last_receipt_index", index).apply()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                newFile
            )
        } catch (ex: IOException) {
            null
        }
    }
}
