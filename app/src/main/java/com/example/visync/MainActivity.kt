package com.example.visync

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.example.visync.ui.theme.VisyncTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.visync.ui.AppWrapper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        val defaultSystemBarsBehavior = windowInsetsController.systemBarsBehavior
        val fullScreenSystemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val layoutParams = window.attributes
        val defaultDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 28) layoutParams.layoutInDisplayCutoutMode else 0
        val applyCutout = true

        val immersiveModeToggler = ImmersiveModeToggler(isInitiallyImmersive = false) { navbarsVisible, statusBarsVisible ->
                // You can hide the caption bar even when the other system bars are visible.
                // To account for this, explicitly check the visibility of navigationBars()
                // and statusBars() rather than checking the visibility of systemBars().
                if (navbarsVisible || statusBarsVisible) {
                    Log.d("fullscreen", "toggling on")
                    // Hide both the status bar and the navigation bar.
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                    windowInsetsController.systemBarsBehavior = fullScreenSystemBarsBehavior
                    if (Build.VERSION.SDK_INT >= 28 && applyCutout) {
                        Log.d("fullscreen", "changing display cutout")
                        layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        window.attributes = layoutParams
                    }
                } else {
                    Log.d("fullscreen", "toggling off")
                    // Show both the status bar and the navigation bar.
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                    windowInsetsController.systemBarsBehavior = defaultSystemBarsBehavior
                    if (Build.VERSION.SDK_INT >= 28 && applyCutout) {
                        layoutParams.layoutInDisplayCutoutMode = defaultDisplayCutoutMode
                        window.attributes = layoutParams
                    }
                }
        }
//        fullscreenToggler.toggle(true, true)
        setContent {
            val isSystemDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(isSystemDarkTheme) }

            VisyncTheme(darkTheme = isDarkTheme) {
                val windowSize = calculateWindowSizeClass(this)
                AppWrapper(
                    windowSize = windowSize,
                    isDarkTheme = isDarkTheme,
                    setDarkTheme = { isDarkTheme = it },
                    immersiveModeToggler = immersiveModeToggler,
                )
            }
        }
    }
}

class ImmersiveModeToggler(
    isInitiallyImmersive: Boolean,
    val toggle: (Boolean, Boolean) -> Unit,
) {
    private var isImmersive = isInitiallyImmersive
    fun toggle() {
        toggle(!isImmersive, !isImmersive)
        isImmersive = !isImmersive
    }

    fun toggle(toImmersive: Boolean) {
        toggle(toImmersive, toImmersive)
        isImmersive = toImmersive
    }
}