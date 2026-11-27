package com.island.phone.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalIslandColors = staticCompositionLocalOf { LightIslandColors }

object IslandTheme {
    val colors: IslandColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIslandColors.current
}

@Composable
fun IslandTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val colors = if (mode.isDark()) DarkIslandColors else LightIslandColors
    CompositionLocalProvider(LocalIslandColors provides colors, content = content)
}
