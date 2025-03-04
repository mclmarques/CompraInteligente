package com.mcldev.comprainteligente.data.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * This methods creates the /tessdata directory and populate it with the data provided from the
 * assets folder
 * @return: String containing the PATH to the tesseract data folder or NULL if it fails
 */
fun createTessFolder(context: Context): String? {
    val path = context.getExternalFilesDir(null)?.absolutePath + "/tesseract"
    val tessDataDirectory = File("$path/tessdata")
    if (!tessDataDirectory.exists() && !tessDataDirectory.mkdirs()) {
        //Log.e("Tesseract", "Failed to create Tesseract directory")
        return null
    }
    val tessDataFile = File(tessDataDirectory, "por.traineddata")
    if (!tessDataFile.exists()) {
        try {
            val inputStream = context.assets.open("tessdata/por.traineddata")
            val outputStream = FileOutputStream(tessDataFile)
            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)
            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            inputStream.close()
            outputStream.close()
           //Log.i("Tesseract", "por.traineddata copied to device")
        } catch (e: Exception) {
            //Log.e("Tesseract", "Error copying por.traineddata: ", e)
            return null
        }
    }
    return path
}