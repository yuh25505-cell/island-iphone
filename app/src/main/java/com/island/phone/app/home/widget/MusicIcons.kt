package com.island.phone.app.home.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

enum class MusicIconKind { QUEUE, REPEAT, PREVIOUS, PLAY, NEXT, HEART, MUTE }

@Composable
fun MusicIcon(kind: MusicIconKind, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        when (kind) {
            MusicIconKind.QUEUE -> drawQueue(color)
            MusicIconKind.REPEAT -> drawRepeat(color)
            MusicIconKind.PREVIOUS -> drawPrevious(color)
            MusicIconKind.PLAY -> drawPlay(color)
            MusicIconKind.NEXT -> drawNext(color)
            MusicIconKind.HEART -> drawHeart(color)
            MusicIconKind.MUTE -> drawMute(color)
        }
    }
}

private fun triangle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Path =
    Path().apply {
        moveTo(ax, ay)
        lineTo(bx, by)
        lineTo(cx, cy)
        close()
    }

private fun DrawScope.softTriangle(path: Path, color: Color, round: Float) {
    drawPath(path = path, color = color)
    drawPath(path = path, color = color, style = Stroke(width = round, join = StrokeJoin.Round))
}

private fun DrawScope.drawQueue(c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.1f
    drawLine(color = c, start = Offset(w * 0.06f, h * 0.22f), end = Offset(w * 0.94f, h * 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color = c, start = Offset(w * 0.06f, h * 0.5f), end = Offset(w * 0.94f, h * 0.5f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color = c, start = Offset(w * 0.06f, h * 0.78f), end = Offset(w * 0.52f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
    softTriangle(triangle(w * 0.66f, h * 0.62f, w * 0.66f, h * 0.94f, w * 0.94f, h * 0.78f), c, w * 0.05f)
}

private fun DrawScope.drawRepeat(c: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        color = c,
        topLeft = Offset(w * 0.1f, h * 0.22f),
        size = Size(w * 0.8f, h * 0.56f),
        cornerRadius = CornerRadius(h * 0.2f, h * 0.2f),
        style = Stroke(width = w * 0.09f),
    )
    softTriangle(triangle(w * 0.6f, h * 0.06f, w * 0.6f, h * 0.38f, w * 0.84f, h * 0.22f), c, w * 0.05f)
    softTriangle(triangle(w * 0.4f, h * 0.62f, w * 0.4f, h * 0.94f, w * 0.16f, h * 0.78f), c, w * 0.05f)
}

private fun DrawScope.drawPrevious(c: Color) {
    val w = size.width
    val h = size.height
    softTriangle(triangle(w * 0.52f, h * 0.22f, w * 0.52f, h * 0.78f, w * 0.1f, h * 0.5f), c.copy(alpha = 0.35f), w * 0.08f)
    softTriangle(triangle(w * 0.9f, h * 0.18f, w * 0.9f, h * 0.82f, w * 0.38f, h * 0.5f), c, w * 0.08f)
}

private fun DrawScope.drawNext(c: Color) {
    val w = size.width
    val h = size.height
    softTriangle(triangle(w * 0.48f, h * 0.22f, w * 0.48f, h * 0.78f, w * 0.9f, h * 0.5f), c.copy(alpha = 0.35f), w * 0.08f)
    softTriangle(triangle(w * 0.1f, h * 0.18f, w * 0.1f, h * 0.82f, w * 0.62f, h * 0.5f), c, w * 0.08f)
}

private fun DrawScope.drawPlay(c: Color) {
    val w = size.width
    val h = size.height
    softTriangle(triangle(w * 0.25f, h * 0.14f, w * 0.25f, h * 0.86f, w * 0.88f, h * 0.5f), c, w * 0.1f)
}

private fun DrawScope.drawHeart(c: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.88f)
        cubicTo(w * 0.08f, h * 0.58f, w * 0.02f, h * 0.3f, w * 0.22f, h * 0.17f)
        cubicTo(w * 0.36f, h * 0.08f, w * 0.5f, h * 0.17f, w * 0.5f, h * 0.3f)
        cubicTo(w * 0.5f, h * 0.17f, w * 0.64f, h * 0.08f, w * 0.78f, h * 0.17f)
        cubicTo(w * 0.98f, h * 0.3f, w * 0.92f, h * 0.58f, w * 0.5f, h * 0.88f)
        close()
    }
    drawPath(path = path, color = c, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
}

private fun DrawScope.drawMute(c: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.08f
    val speaker = Path().apply {
        moveTo(w * 0.06f, h * 0.38f)
        lineTo(w * 0.26f, h * 0.38f)
        lineTo(w * 0.5f, h * 0.18f)
        lineTo(w * 0.5f, h * 0.82f)
        lineTo(w * 0.26f, h * 0.62f)
        lineTo(w * 0.06f, h * 0.62f)
        close()
    }
    drawPath(path = speaker, color = c, style = Stroke(width = sw, join = StrokeJoin.Round))
    drawLine(color = c, start = Offset(w * 0.66f, h * 0.4f), end = Offset(w * 0.92f, h * 0.6f), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color = c, start = Offset(w * 0.66f, h * 0.6f), end = Offset(w * 0.92f, h * 0.4f), strokeWidth = sw, cap = StrokeCap.Round)
}
