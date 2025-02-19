package com.mcldev.comprainteligente.ui.settings_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsScreenVM(
    private val context: Context
): ViewModel()  {
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    /*
    Values:
    0 -> 1 month
    1 -> 3 months
    2 -> 1 year
    3 -> 2 years
    4 -> never
    DO NOT USE OTHER VALUES before updating the CustomDropDown() composable from SettingsScreen.kt
     */
    private val _dataRetentionPeriod = MutableStateFlow(
        sharedPreferences.getInt("retention_period", 1) // Default: 3 months
    )
    val dataRetentionPeriod: StateFlow<Int> = _dataRetentionPeriod

    private val _deleteAllData = MutableStateFlow(
        sharedPreferences.getBoolean("delete_all_data", true) // Default: delete all
    )
    val deleteAllData: StateFlow<Boolean> = _deleteAllData

    fun updateDataRetentionPeriod(newPeriod: Int) {
        _dataRetentionPeriod.value = newPeriod
        sharedPreferences.edit().putInt("retention_period", newPeriod).apply()
    }

    fun updateDeleteAllData(newState: Boolean) {
        _deleteAllData.value = newState
        sharedPreferences.edit().putBoolean("delete_all_data", newState).apply()
    }

}