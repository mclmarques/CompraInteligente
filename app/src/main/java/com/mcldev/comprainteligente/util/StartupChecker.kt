package com.mcldev.comprainteligente.util

import android.app.ActivityManager
import android.content.Context
import com.mcldev.comprainteligente.ui.util.ErrorCodes
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class StartupChecker(
    private val context: Context,
): KoinComponent {
    fun checkDevice(): StartupResult {
        //check RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMemory = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)

        if (totalMemory < 22) {
            return StartupResult.Error(ErrorCodes.UNSUPPORTED_DEVICE_ERROR_1)
        }

        //Check Tesseract
        val path by inject<String>()
        val tesseractDir = File(path)
        if (!tesseractDir.exists()) {
            return StartupResult.Error(ErrorCodes.UNSUPPORTED_DEVICE_ERROR_2)
        }


        return StartupResult.Success
    }
}

sealed class StartupResult {
    object Success : StartupResult()
    class Error(val code: ErrorCodes) : StartupResult()
}
