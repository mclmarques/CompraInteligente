package com.mcldev.comprainteligente
import androidx.room.Room
import com.mcldev.comprainteligente.data.DataBase
import com.mcldev.comprainteligente.data.createTessFolder
import com.mcldev.comprainteligente.ui.home_screen.HomeScreenVM
import com.mcldev.comprainteligente.ui.scan_screen.ScanScreenVM
import com.mcldev.comprainteligente.ui.settings_screen.SettingsScreenVM
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Provide Room Database instance
    single {
        Room.databaseBuilder(
            androidContext(),
            DataBase::class.java,
            "product_supermarket_db"
        ).build()
    }

    // Provide DAOs
    single { get<DataBase>().productDao() }
    single { get<DataBase>().supermarketDao() }

    // Provide any additional dependencies
    single { createTessFolder(context = androidContext()) }

    // Provide ViewModels
    viewModel { HomeScreenVM(get(), get()) }
    viewModel { ScanScreenVM(get(), get(), get()) }
    viewModel { SettingsScreenVM(androidContext()) }
}
