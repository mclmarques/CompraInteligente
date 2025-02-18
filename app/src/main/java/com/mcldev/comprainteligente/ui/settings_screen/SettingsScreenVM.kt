package com.mcldev.comprainteligente.ui.settings_screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsScreenVM: ViewModel()  {
    /*
    Values:
    0 -> 1 month
    1 -> 3 months
    2 -> 1 year
    3 -> 2 years
    4 -> never
    DO NOT USE OTHER VALUES before updating the CustomDropDown() composable from SettingsScreen.kt
     */
    private val _dataRetentionPeriod = MutableStateFlow(1)
    val dataRetentionPeriod: StateFlow<Int> = _dataRetentionPeriod

    private val _deleteAllData = MutableStateFlow(true)
    val deleteAllData: StateFlow<Boolean> = _deleteAllData

    fun updateDeleteAllData(newState: Boolean){
        _deleteAllData.value = newState
    }

}