package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.RecentFileRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SheetViewModel
import com.example.ui.viewmodel.SheetViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SheetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database and ViewModel with Context
        val database = AppDatabase.getDatabase(this)
        val repository = RecentFileRepository(database.recentFileDao())
        val viewModelFactory = SheetViewModelFactory(repository, this.applicationContext)
        viewModel = ViewModelProvider(this, viewModelFactory)[SheetViewModel::class.java]

        setContent {
            val themePref by viewModel.themePreference.collectAsState()
            val isDarkTheme = when (themePref) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var currentScreen by remember { mutableStateOf("dashboard") }

                // Handle back-press inside the App
                if (currentScreen == "viewer") {
                    BackHandler {
                        currentScreen = "dashboard"
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // High-quality screen transitions
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "dashboard" -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToViewer = { currentScreen = "viewer" }
                                )
                            }
                            "viewer" -> {
                                ViewerScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = "dashboard" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
