package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.gemini.GeminiMarketingService
import com.example.data.repository.MarketingRepository
import com.example.ui.MainScaffold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MarketingViewModel
import com.example.ui.viewmodel.MarketingViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MarketingViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val geminiService = GeminiMarketingService()
        val repository = MarketingRepository(
            database = database,
            geminiService = geminiService
        )
        MarketingViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScaffold(viewModel = viewModel)
            }
        }
    }
}

