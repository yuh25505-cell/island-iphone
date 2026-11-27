package com.island.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.island.phone.app.boot.BootScreen
import com.island.phone.app.home.HomeScreen
import com.island.phone.app.lock.LockScreen
import com.island.phone.core.nav.IslandNavHost
import com.island.phone.core.nav.IslandNavigator
import com.island.phone.core.nav.Route
import com.island.phone.core.theme.ThemeMode

@Composable
fun IslandApp(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    val navigator = remember { IslandNavigator(Route.Home) }
    var booting by rememberSaveable { mutableStateOf(true) }
    var locked by rememberSaveable { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        IslandNavHost(navigator) { route ->
            when (route) {
                Route.Home -> HomeScreen(mode = mode, onModeChange = onModeChange)
            }
        }
        if (locked) {
            LockScreen(onUnlocked = { locked = false })
        }
        if (booting) {
            BootScreen(onFinished = { booting = false })
        }
    }
}
