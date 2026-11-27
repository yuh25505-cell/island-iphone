package com.island.phone.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

enum class ThemeMode(val key: String) {
    FOLLOW_SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: FOLLOW_SYSTEM
    }
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
