package com.island.phone.core.ui.glass

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.island.phone.core.ui.theme.GlassTokens
import com.island.phone.core.ui.theme.IslandTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val backdrop = LocalBackdrop.current
    val glass = IslandTheme.glass
    val settings = GlassSettings.get(LocalContext.current)
    val lensOn = settings.lensActive &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        backdrop != null && backdrop.blurSupported
    val lensLayer = if (lensOn) rememberGraphicsLayer() else null
    val lens = remember { GlassLens() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shadowStrength = settings.shadow
    Box(
        modifier = modifier
            .shadow(
                elevation * shadowStrength,
                shape,
                clip = false,
                ambientColor = glass.shadow.scaleAlpha(shadowStrength),
                spotColor = glass.shadow.scaleAlpha(shadowStrength)
            )
            .clip(shape)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind { drawGlass(shape, backdrop, origin, glass, settings, lensLayer, lens) },
        content = content
    )
}

private fun Color.scaleAlpha(factor: Float): Color = copy(alpha = (alpha * factor).coerceIn(0f, 1f))

private fun DrawScope.drawGlass(
    shape: Shape,
    backdrop: BackdropState?,
    origin: Offset,
    glass: GlassTokens,
    settings: GlassSettings,
    lensLayer: GraphicsLayer?,
    lens: GlassLens,
) {
    if (settings.blur < 0f || settings.saturation < 0f) return
    val outline = shape.createOutline(size, layoutDirection, this)
    val blurOn = backdrop != null && backdrop.blurSupported
    val minDim = min(size.width, size.height)
    val half = minDim / 2f
    val radius = when (outline) {
        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is Outline.Rectangle -> 0f
        is Outline.Generic -> half
    }.coerceAtMost(half)
    val wide = ((minDim * 0.3f).coerceIn(12.dp.toPx(), 40.dp.toPx()) *
        (max(settings.edge, 0.5f) / 2.7f))
    val zone = min(wide, max(radius, 8.dp.toPx())).coerceIn(1f, max(half, 1f))
    if (backdrop != null && blurOn) {
        val delta = origin - backdrop.origin
        if (lensLayer != null) {
            val w = max(size.width.roundToInt(), 1)
            val h = max(size.height.roundToInt(), 1)
            lensLayer.record(this, layoutDirection, IntSize(w, h)) {
                translate(-delta.x, -delta.y) { drawLayer(backdrop.blurred) }
            }
            lens.apply(
                lensLayer,
                size.width,
                size.height,
                radius,
                zone,
                1f + settings.refraction * 0.5f,
                settings.thickness * zone * 1.6f,
                settings.dispersion,
                settings.magnify,
                settings.highlight
            )
            drawLayer(lensLayer)
        } else {
            translate(-delta.x, -delta.y) { drawLayer(backdrop.blurred) }
        }
    }
    drawRect((if (blurOn) glass.tint else glass.tintFallback).scaleAlpha(settings.tint))
    if (lensLayer == null) {
        val glowEdge = glass.rimLight.scaleAlpha(settings.highlight * 0.07f)
        val glowMid = glass.rimLight.scaleAlpha(settings.highlight * 0.02f)
        val glow = Brush.linearGradient(
            0f to glowEdge,
            0.35f to glowMid,
            0.65f to glowMid,
            1f to glowEdge,
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        for (i in 0 until 5) {
            drawOutline(outline, glow, style = Stroke(zone * (1.6f - i * 0.3f)))
        }
    }
    val rimPx = 1.2.dp.toPx()
    val rim = Brush.linearGradient(
        0f to glass.rimLight.scaleAlpha(settings.highlight),
        0.3f to glass.rimDark,
        0.7f to glass.rimDark,
        1f to glass.rimLight.scaleAlpha(settings.highlight),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    drawOutline(outline, rim, style = Stroke(rimPx * 2f))
}
