package com.mcldev.comprainteligente.ui.scan_screen

import com.mcldev.comprainteligente.ui.util.ErrorCodes

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data object Loading : ProcessingState()
    data object Complete : ProcessingState()
    data class Error(val code: ErrorCodes) : ProcessingState()
}
