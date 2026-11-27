package com.island.phone.core.nav

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.island.phone.core.theme.IslandTheme
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun IslandNavHost(
    navigator: IslandNavigator,
    content: @Composable (Route) -> Unit,
) {
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val colors = IslandTheme.colors

    PredictiveBackHandler(enabled = navigator.canPop) { progress ->
        try {
            progress.collect { event -> dragProgress = event.progress }
            navigator.pop()
            dragProgress = 0f
        } catch (e: CancellationException) {
            dragProgress = 0f
            throw e
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val widthPx = constraints.maxWidth.toFloat()
        val stack = navigator.stack
        val topIndex = stack.lastIndex

        if (topIndex > 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = -widthPx * 0.3f * (1f - dragProgress) },
            ) {
                content(stack[topIndex - 1])
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - dragProgress }
                    .background(colors.scrim),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = widthPx * dragProgress }
                .background(colors.background),
        ) {
            content(stack[topIndex])
        }
    }
}
