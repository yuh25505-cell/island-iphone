package com.island.phone.app.boot

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.R
import com.island.phone.core.theme.IslandTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BootScreen(onFinished: () -> Unit) {
    val colors = IslandTheme.colors
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    val total = if (reduceMotion) 1500 else BootTimeline.TOTAL
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    var skipped by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)

    val timeMs: () -> Float = {
        if (reduceMotion) BootTimeline.TEXT2_END + 1000f else progress.value * BootTimeline.TOTAL
    }

    LaunchedEffect(Unit) {
        val play = launch { progress.animateTo(1f, tween(total, easing = LinearEasing)) }
        val watcher = launch {
            snapshotFlow { skipped }.first { it }
            play.cancel()
        }
        play.join()
        watcher.cancel()
        exit.animateTo(1f, tween(if (skipped) 280 else 500))
        finish()
    }

    BackHandler { skipped = true }

    val water = colors.accent
    val highlight = Color.White.copy(alpha = if (colors.isDark) 0.7f else 0.95f)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures { skipped = true } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRainIntro(timeMs(), water, highlight)
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BootLine(
                text = stringResource(R.string.boot_line_1),
                color = colors.label,
                from = BootTimeline.TEXT1_START,
                to = BootTimeline.TEXT1_END,
                timeMs = timeMs,
            )
            Spacer(Modifier.height(14.dp))
            BootLine(
                text = stringResource(R.string.boot_line_2),
                color = colors.secondaryLabel,
                from = BootTimeline.TEXT2_START,
                to = BootTimeline.TEXT2_END,
                timeMs = timeMs,
            )
        }
    }
}

@Composable
private fun BootLine(
    text: String,
    color: Color,
    from: Float,
    to: Float,
    timeMs: () -> Float,
) {
    BasicText(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.graphicsLayer {
            val p = progressOf(timeMs(), from, to)
            alpha = p
            translationY = (1f - p) * 8.dp.toPx()
        },
    )
}
