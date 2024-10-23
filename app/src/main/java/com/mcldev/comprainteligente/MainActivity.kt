package com.mcldev.comprainteligente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mcldev.comprainteligente.data.DataBase
import com.mcldev.comprainteligente.ui.theme.CompraInteligenteTheme

class MainActivity : ComponentActivity() {
    // Access the AppDatabase from MyApp class
    private val database: DataBase by lazy {
        Application.getDatabase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompraInteligenteTheme {
            }
        }
    }
}
