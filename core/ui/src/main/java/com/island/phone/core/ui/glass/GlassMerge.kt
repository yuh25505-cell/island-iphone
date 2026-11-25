package com.island.phone.core.ui.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.island.phone.core.ui.theme.GlassTokens
import com.island.phone.core.ui.theme.IslandTheme
import kotlin.math.max
import kotlin.math.min

data class GlassBlob(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val radius: Dp,
)

private class Blob(
    val cx: Float,
    val cy: Float,
    val hw: Float,
    val hh: Float,
    val r: Float,
) {
    fun collapsed() = Blob(cx, cy, 0f, 0f, 0f)
}

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun lerpBlobs(from: List<Blob>, to: List<Blob>, t: Float): List<Blob> {
    val n = max(from.size, to.size)
    return List(n) { i ->
        val a = from.getOrNull(i) ?: to[i].collapsed()
        val b = to.getOrNull(i) ?: from[i].collapsed()
        Blob(
            lerpF(a.cx, b.cx, t),
            lerpF(a.cy, b.cy, t),
            max(lerpF(a.hw, b.hw, t), 0f),
            max(lerpF(a.hh, b.hh, t), 0f),
            max(lerpF(a.r, b.r, t), 0f)
        )
    }
}

@Composable
fun GlassMerge(
    blobs: List<GlassBlob>,
    modifier: Modifier = Modifier,
    smooth: Dp = 28.dp,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val backdrop = LocalBackdrop.current
    val glass = IslandTheme.glass
    val settings = GlassSettings.get(LocalContext.current)
    val lensOn = settings.lensActive &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        backdrop != null && backdrop.blurSupported
    val lensLayer = if (lensOn) rememberGraphicsLayer() else null
    val lens = remember { GlassLens() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val target = remember(blobs, density) {
        blobs.take(4).map { b ->
            with(density) {
                Blob(
                    b.x.toPx(),
                    b.y.toPx(),
                    b.width.toPx() / 2f,
                    b.height.toPx() / 2f,
                    b.radius.toPx()
                )
            }
        }
    }
    var shown by remember { mutableStateOf(target) }
    LaunchedEffect(target, animate) {
        if (!animate) {
            shown = target
            return@LaunchedEffect
        }
        val from = shown
        val progress = Animatable(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f)) {
            shown = lerpBlobs(from, target, value)
        }
        shown = target
    }
    val smoothPx = with(density) { smooth.toPx() }
    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                drawMerge(shown, smoothPx, backdrop, origin, glass, settings, lensLayer, lens)
            },
        content = content
    )
}

private fun DrawScope.drawMerge(
    blobs: List<Blob>,
    smooth: Float,
    backdrop: BackdropState?,
    origin: Offset,
    glass: GlassTokens,
    settings: GlassSettings,
    lensLayer: androidx.compose.ui.graphics.layer.GraphicsLayer?,
    lens: GlassLens,
) {
    if (blobs.isEmpty()) return
    val cx0 = size.width / 2f
    val cy0 = size.height / 2f
    val shapes = FloatArray(16)
    val radii = FloatArray(4)
    var minDim = Float.MAX_VALUE
    var minRadius = Float.MAX_VALUE
    blobs.forEachIndexed { i, b ->
        shapes[i * 4] = cx0 + b.cx
        shapes[i * 4 + 1] = cy0 + b.cy
        shapes[i * 4 + 2] = b.hw
        shapes[i * 4 + 3] = b.hh
        val r = min(b.r, min(b.hw, b.hh))
        radii[i] = r
        if (b.hw > 0.5f) {
            minDim = min(minDim, min(b.hw, b.hh) * 2f)
            minRadius = min(minRadius, r)
        }
    }
    if (minDim == Float.MAX_VALUE) return
    if (backdrop != null && backdrop.blurSupported && lensLayer != null) {
        drawLensGlass(
            backdrop, origin, lensLayer, lens, glass, settings,
            shapes, radii, blobs.size, smooth, minDim, minRadius
        )
        return
    }
    val rimPx = 1.2.dp.toPx()
    blobs.forEach { b ->
        if (b.hw > 0.5f) {
            val r = min(b.r, min(b.hw, b.hh))
            val topLeft = Offset(cx0 + b.cx - b.hw, cy0 + b.cy - b.hh)
            val box = Size(b.hw * 2f, b.hh * 2f)
            drawRoundRect(
                glass.tintFallback.scaleAlpha(settings.tint),
                topLeft = topLeft,
                size = box,
                cornerRadius = CornerRadius(r)
            )
            drawRoundRect(
                glass.rimLight.scaleAlpha(settings.highlight * 0.6f),
                topLeft = topLeft,
                size = box,
                cornerRadius = CornerRadius(r),
                style = Stroke(rimPx)
            )
        }
    }
}
