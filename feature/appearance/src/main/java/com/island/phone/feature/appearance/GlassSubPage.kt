package com.island.phone.feature.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.ui.appearance.AppearanceStore
import com.island.phone.core.ui.glass.GlassBlob
import com.island.phone.core.ui.glass.GlassMerge
import com.island.phone.core.ui.glass.GlassParam
import com.island.phone.core.ui.glass.GlassSettings
import com.island.phone.core.ui.glass.GlassSurface
import com.island.phone.core.ui.glass.GlassUnit
import com.island.phone.core.ui.glass.OwnBackdrop
import com.island.phone.core.ui.text.BasicText
import com.island.phone.core.ui.theme.IslandTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun GlassSubPage(store: AppearanceStore, onClose: () -> Unit) {
    val glass = GlassSettings.get(LocalContext.current)
    val labelColor = IslandTheme.icon.label
    SubPageScaffold(store = store, title = "液态玻璃", onClose = onClose) {
        GlassPreview()
        Spacer(Modifier.height(16.dp))
        SettingCard(
            title = "玻璃",
            titleColor = labelColor,
            action = {
                MinusButton(color = labelColor, enabled = !glass.isDefault) { glass.reset() }
            }
        ) {
            GlassParam.entries.forEach { param ->
                GlassSliderRow(
                    title = param.title,
                    valueText = formatValue(param, glass.valueOf(param)),
                    value = glass.valueOf(param),
                    min = param.min,
                    max = param.max,
                    color = labelColor
                ) { glass.set(param, it) }
            }
        }
    }
}

private fun formatValue(param: GlassParam, value: Float): String = when (param.unit) {
    GlassUnit.INT -> value.roundToInt().toString()
    GlassUnit.ONE_DECIMAL -> String.format(Locale.US, "%.1f", value)
    GlassUnit.PERCENT -> "${(value * 100f).roundToInt()}%"
}

@Composable
private fun GlassPreview() {
    var merged by remember { mutableStateOf(false) }
    val blobs = if (merged) {
        listOf(
            GlassBlob((-24).dp, 0.dp, 104.dp, 80.dp, 40.dp),
            GlassBlob(24.dp, 0.dp, 104.dp, 80.dp, 40.dp)
        )
    } else {
        listOf(
            GlassBlob((-66).dp, 0.dp, 104.dp, 80.dp, 40.dp),
            GlassBlob(66.dp, 0.dp, 104.dp, 80.dp, 40.dp)
        )
    }
    OwnBackdrop(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectTapGestures { merged = !merged }
            },
        source = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF7A59), Color(0xFF7B61FF), Color(0xFF2EC4B6)),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        val step = size.width / 14f
                        var x = 0f
                        while (x < size.width) {
                            drawRect(
                                Color.White.copy(alpha = 0.5f),
                                topLeft = Offset(x, 0f),
                                size = Size(step * 0.35f, size.height)
                            )
                            x += step
                        }
                        drawCircle(
                            Color(0xFFFFE066),
                            radius = size.height * 0.22f,
                            center = Offset(size.width * 0.22f, size.height * 0.35f)
                        )
                        drawCircle(
                            Color(0xFF1B1B3A),
                            radius = size.height * 0.16f,
                            center = Offset(size.width * 0.8f, size.height * 0.7f)
                        )
                    }
            )
        }
    ) {
        GlassMerge(blobs = blobs, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GlassSliderRow(
    title: String,
    valueText: String,
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = title,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            )
            BasicText(
                text = valueText,
                style = TextStyle(color = color.copy(alpha = 0.6f), fontSize = 15.sp)
            )
        }
        GlassSlider(value = value, min = min, max = max, color = color, onChange = onChange)
    }
}

@Composable
private fun GlassSlider(
    value: Float,
    min: Float,
    max: Float,
    color: Color,
    onChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val thumbPx = with(density) { 26.dp.toPx() }
    var widthPx by remember { mutableStateOf(0f) }
    val latest by rememberUpdatedState(onChange)
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val travel = (widthPx - thumbPx).coerceAtLeast(0f)
    val thumbX = fraction * travel

    fun valueAt(x: Float): Float {
        val usable = (widthPx - thumbPx).coerceAtLeast(1f)
        val f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        return min + f * (max - min)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset -> latest(valueAt(offset.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    latest(valueAt(change.position.x))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .width(with(density) { (thumbX + thumbPx / 2f).toDp() })
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.55f))
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbX.roundToInt(), 0) }
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
