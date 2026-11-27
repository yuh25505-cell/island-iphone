package com.island.phone.app.home.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.island.phone.core.theme.IslandTheme

internal val WidgetLightBackground = Color(0xFFF4F4F4)

@Composable
internal fun widgetBackground(darkColor: Color): Color =
    if (IslandTheme.colors.isDark) darkColor else WidgetLightBackground
