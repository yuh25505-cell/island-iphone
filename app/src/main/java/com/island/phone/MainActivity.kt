package com.island.phone

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.island.phone.core.theme.IslandTheme
import com.island.phone.core.theme.isDark
import com.island.phone.data.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        val initialMode = runBlocking { store.themeMode.first() }

        setContent {
            val mode by store.themeMode.collectAsStateWithLifecycle(initialValue = initialMode)
            val dark = mode.isDark()

            DisposableEffect(dark) {
                val barStyle = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose { }
            }

            IslandTheme(mode) {
                IslandApp(
                    mode = mode,
                    onModeChange = { newMode ->
                        lifecycleScope.launch { store.setThemeMode(newMode) }
                    },
                )
            }
        }
    }
}
