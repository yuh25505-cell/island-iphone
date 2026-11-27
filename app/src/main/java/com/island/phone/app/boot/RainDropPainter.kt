package com.island.phone.app.boot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

object BootTimeline {
    const val FALL_START = 300f
    const val IMPACT = 1050f
    const val SLIDE_START = 1500f
    const val SLIDE_END = 3100f
    const val TEXT1_START = 2900f
    const val TEXT1_END = 4100f
    const val TEXT2_START = 3900f
    const val TEXT2_END = 5100f
    const val TOTAL = 6600
}

internal fun progressOf(t: Float, from: Float, to: Float): Float =
    ((t - from) / (to - from)).coerceIn(0f, 1f)

private fun easeOut(u: Float): Float {
    val v = 1f - u
    return 1f - v * v * v
}

private fun easeIn(u: Float): Float = u * u

private fun smooth(u: Float): Float = u * u * (3f - 2f * u)

private fun slideFraction(t: Float): Float = when {
    t < BootTimeline.SLIDE_START -> 0f
    t < 2050f -> 0.18f * smooth(progressOf(t, BootTimeline.SLIDE_START, 2050f))
    t < 2300f -> 0.18f
    else -> 0.18f + 0.82f * smooth(progressOf(t, 2300f, BootTimeline.SLIDE_END))
}

fun DrawScope.drawRainIntro(timeMs: Float, water: Color, highlight: Color) {
    val dp = 1.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height * 0.5f
    val slideDistance = size.height * 0.2f
    val wobbleWidth = 2.2f * dp

    if (timeMs >= BootTimeline.IMPACT) {
        val rings = listOf(0f to 96f, 180f to 62f)
        for ((delay, maxRadius) in rings) {
            val u = progressOf(timeMs, BootTimeline.IMPACT + delay, BootTimeline.IMPACT + delay + 1200f)
            if (u > 0f && u < 1f) {
                drawCircle(
                    color = water.copy(alpha = 0.5f * (1f - u)),
                    radius = maxRadius * dp * easeOut(u),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f * dp),
                )
            }
        }
    }

    val slide = slideFraction(timeMs)

    if (slide > 0f) {
        val trailAlpha = 1f - progressOf(timeMs, 2300f, 3300f)
        if (trailAlpha > 0f) {
            val trail = Path()
            val steps = 28
            for (i in 0..steps) {
                val f = slide * i / steps
                val x = cx + sin(f * 16f) * wobbleWidth
                val y = cy + slideDistance * f
                if (i == 0) trail.moveTo(x, y) else trail.lineTo(x, y)
            }
            drawPath(
                path = trail,
                color = water.copy(alpha = 0.22f * trailAlpha),
                style = Stroke(width = 2.6f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = trail,
                color = highlight.copy(alpha = highlight.alpha * 0.45f * trailAlpha),
                style = Stroke(width = 0.8f * dp, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }

    if (timeMs >= BootTimeline.IMPACT) {
        val beadAlpha = 1f - progressOf(timeMs, 3000f, 3500f)
        if (beadAlpha > 0f) {
            val grow = easeOut(progressOf(timeMs, BootTimeline.IMPACT, BootTimeline.IMPACT + 250f))
            val radius = (3f + 4f * grow) * dp * (1f - 0.3f * slide)
            val x = cx + sin(slide * 16f) * wobbleWidth
            val y = cy + slideDistance * slide
            drawBead(x, y, radius, water, highlight, beadAlpha)
        }
    }

    if (timeMs >= BootTimeline.FALL_START && timeMs < BootTimeline.IMPACT) {
        val u = progressOf(timeMs, BootTimeline.FALL_START, BootTimeline.IMPACT)
        val y = -40f * dp + (cy + 40f * dp) * easeIn(u)
        val height = (14f + 18f * u) * dp
        val alpha = progressOf(timeMs, BootTimeline.FALL_START, BootTimeline.FALL_START + 150f)
        drawFallingDrop(cx, y, 5f * dp, height, water, highlight, alpha)
    }
}

private fun DrawScope.drawBead(
    x: Float,
    y: Float,
    radius: Float,
    water: Color,
    highlight: Color,
    alpha: Float,
) {
    val center = Offset(x, y)
    drawCircle(color = water.copy(alpha = 0.22f * alpha), radius = radius, center = center)
    drawCircle(
        color = water.copy(alpha = 0.65f * alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
    )
    drawCircle(
        color = highlight.copy(alpha = highlight.alpha * alpha),
        radius = radius * 0.28f,
        center = Offset(x - radius * 0.35f, y - radius * 0.35f),
    )
}

private fun DrawScope.drawFallingDrop(
    cx: Float,
    y: Float,
    r: Float,
    h: Float,
    water: Color,
    highlight: Color,
    alpha: Float,
) {
    val path = Path().apply {
        moveTo(cx, y - h)
        cubicTo(cx + r * 0.2f, y - h * 0.35f, cx + r, y - r * 0.9f, cx + r, y)
        arcTo(Rect(cx - r, y - r, cx + r, y + r), 0f, 180f, false)
        cubicTo(cx - r, y - r * 0.9f, cx - r * 0.2f, y - h * 0.35f, cx, y - h)
        close()
    }
    drawPath(path = path, color = water.copy(alpha = 0.28f * alpha))
    drawPath(
        path = path,
        color = water.copy(alpha = 0.7f * alpha),
        style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round),
    )
    drawCircle(
        color = highlight.copy(alpha = highlight.alpha * alpha),
        radius = r * 0.28f,
        center = Offset(cx - r * 0.35f, y - r * 0.2f),
    )
}
