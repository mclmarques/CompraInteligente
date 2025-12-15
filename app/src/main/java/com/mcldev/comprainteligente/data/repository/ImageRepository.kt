package com.mcldev.comprainteligente.data.repository


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

                // 2. Create a local file to save the copy
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
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Helper to create a file in the app's cache or files directory.
     * Returns a Uri pointing to the new empty file.
     * Runs on the same thread it is called
     */
    private fun createImageFile(): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir = context.getExternalFilesDir(null) // or context.filesDir
            val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            )
            Uri.fromFile(image)
        } catch (ex: IOException) {
            null
        }
    }
}