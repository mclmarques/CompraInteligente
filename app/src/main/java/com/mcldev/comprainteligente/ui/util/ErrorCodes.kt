package com.mcldev.comprainteligente.ui.util

import com.mcldev.comprainteligente.R

enum class ErrorCodes (val errCode: Int, val titleResId: Int, val messageResId: Int?) {
    CAMERA_ERROR(1, R.string.err_launch_camera_title, R.string.err_launch_camera),
    TEXT_EXTRACTION_ERROR(2, R.string.err_text_extraction_title, R.string.err_text_extraction_title),
    DATA_SAVE_ERROR(3, R.string.err_saving_data_title, R.string.err_saving_data ),
    UNSUPPORTED_DEVICE_ERROR_1(4, R.string.err_unsupported_title, R.string.err_unsupported ),
    UNSUPPORTED_DEVICE_ERROR_2(5, R.string.err_storage_title, R.string.err_storage );

//    GOOGLE_ACCOUNT_ERROR(6, null);
//    CLOUD_SAVE_ERROR(7, null),
}