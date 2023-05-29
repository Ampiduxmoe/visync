package com.example.visync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.example.visync.ui.theme.VisyncTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.visync.ui.VisyncApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisyncTheme {
                val windowSize = calculateWindowSizeClass(this)

                VisyncApp(
                    windowSize = windowSize
                )
            }
        }
    }
}