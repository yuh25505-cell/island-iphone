package com.island.phone.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.island.phone.R
import com.island.phone.core.theme.IslandTheme
import com.island.phone.core.theme.ThemeMode
import com.island.phone.ui.dialog.IosActionSheet
import com.island.phone.ui.dialog.SheetAction

@Composable
fun HomeScreen(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    val colors = IslandTheme.colors
    // 日间/夜间/跟随系统的选择弹窗和按钮保留着，主屏幕不再显示按钮，等外观 App 做好后改从那里打开
    var sheetVisible by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        HomePager(Modifier.fillMaxSize())

        IosActionSheet(
            visible = sheetVisible,
            actions = listOf(
                SheetAction(
                    title = stringResource(R.string.theme_follow_system),
                    selected = mode == ThemeMode.FOLLOW_SYSTEM,
                ) {
                    onModeChange(ThemeMode.FOLLOW_SYSTEM)
                    sheetVisible = false
                },
                SheetAction(
                    title = stringResource(R.string.theme_light),
                    selected = mode == ThemeMode.LIGHT,
                ) {
                    onModeChange(ThemeMode.LIGHT)
                    sheetVisible = false
                },
                SheetAction(
                    title = stringResource(R.string.theme_dark),
                    selected = mode == ThemeMode.DARK,
                ) {
                    onModeChange(ThemeMode.DARK)
                    sheetVisible = false
                },
            ),
            cancelText = stringResource(R.string.cancel),
            onDismiss = { sheetVisible = false },
        )
    }
}

@Suppress("unused")
@Composable
private fun ThemeButton(modifier: Modifier, onClick: () -> Unit) {
    val color = IslandTheme.colors.accent
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        modifier
            .size(44.dp)
            .alpha(if (pressed) 0.4f else 1f)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val stroke = 1.8.dp.toPx()
            val radius = size.minDimension / 2f - stroke / 2f
            drawCircle(color = color, radius = radius, style = Stroke(width = stroke))
            drawArc(
                color = color,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(radius * 2f, radius * 2f),
            )
        }
    }
}
