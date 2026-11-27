package com.island.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.island.phone.app.home.HomeScreen
import com.island.phone.core.nav.IslandNavHost
import com.island.phone.core.nav.IslandNavigator
import com.island.phone.core.nav.Route
import com.island.phone.core.theme.ThemeMode

@Composable
fun IslandApp(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    val navigator = remember { IslandNavigator(Route.Home) }
    IslandNavHost(navigator) { route ->
        when (route) {
            Route.Home -> HomeScreen(mode = mode, onModeChange = onModeChange)
        }
    }
}
