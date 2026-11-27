package com.island.phone.app.home.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RecordWidget(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.gray6, RoundedCornerShape(30.dp)),
    ) {
        val disc = maxHeight * 0.66f
        Box(Modifier.padding(start = 16.dp, top = 16.dp)) {
            HeadphoneIcon(colors.label, 12.dp)
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 28.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VinylRecord(Modifier.size(disc))
            Spacer(Modifier.width(18.dp))
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        BasicText(
                            text = "STAY",
                            style = TextStyle(
                                color = colors.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                            ),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "- Justin Bieber",
                            modifier = Modifier.weight(1f, fill = false),
                            style = TextStyle(color = colors.label, fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    BasicText(
                        text = "I need you to stay, need you to stay, hey",
                        style = TextStyle(color = colors.label, fontSize = 11.sp, lineHeight = 14.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = "我渴求你的驻足",
                        style = TextStyle(color = colors.label, fontSize = 11.sp, lineHeight = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column {
                    RecordProgress(
                        progress = 0.7f,
                        trackColor = colors.label.copy(alpha = 0.15f),
                        fillColor = colors.label,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText(
                            text = "00.00",
                            style = TextStyle(color = colors.secondaryLabel, fontSize = 10.sp),
                        )
                        BasicText(
                            text = "03.56",
                            style = TextStyle(color = colors.secondaryLabel, fontSize = 10.sp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().height(30.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkipIcon(colors.label, 16.dp, forward = false)
                        Spacer(Modifier.width(22.dp))
                        Box(
                            Modifier.size(30.dp).background(colors.label, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayTriangle(colors.background, 12.dp)
                        }
                        Spacer(Modifier.width(22.dp))
                        SkipIcon(colors.label, 16.dp, forward = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylRecord(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(color = Color(0xFF111111), radius = r, center = c)
        var groove = r * 0.97f
        while (groove > r * 0.64f) {
            drawCircle(
                color = Color(0xFF2B2B2B),
                radius = groove,
                center = c,
                style = Stroke(width = 0.6.dp.toPx()),
            )
            groove -= r * 0.045f
        }
        val label = r * 0.62f
        drawCircle(color = Color(0xFFD6D6D6), radius = label, center = c)
        drawCircle(color = Color(0xFFEFEFEF), radius = label * 0.82f, center = c)
        drawPath(path = starPath(c, label * 0.52f, label * 0.21f), color = Color(0xFF3A3A3A))
    }
}

private fun starPath(center: Offset, outer: Float, inner: Float): Path =
    Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.toRadians(-90.0 + i * 36.0)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

@Composable
private fun RecordProgress(progress: Float, trackColor: Color, fillColor: Color) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val strokeWidth = 2.dp.toPx()
        val cy = size.height / 2f
        val end = size.width
        drawLine(color = trackColor, start = Offset(0f, cy), end = Offset(end, cy), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color = fillColor, start = Offset(0f, cy), end = Offset(end * progress, cy), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun HeadphoneIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.1f, w * 0.08f),
            size = Size(w * 0.8f, w * 0.8f),
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.04f, w * 0.5f),
            size = Size(w * 0.24f, w * 0.42f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.72f, w * 0.5f),
            size = Size(w * 0.24f, w * 0.42f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        )
    }
}

@Composable
private fun PlayTriangle(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.05f)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.95f, h * 0.5f)
            close()
        }
        drawPath(path = path, color = color)
        drawPath(path = path, color = color, style = Stroke(width = w * 0.12f, join = StrokeJoin.Round))
    }
}

@Composable
private fun SkipIcon(color: Color, size: Dp, forward: Boolean) {
    Canvas(Modifier.size(size)) {
        skipShape(color, forward)
    }
}

private fun DrawScope.skipShape(color: Color, forward: Boolean) {
    val w = size.width
    val h = size.height
    val triangle = Path().apply {
        if (forward) {
            moveTo(w * 0.05f, h * 0.12f)
            lineTo(w * 0.05f, h * 0.88f)
            lineTo(w * 0.7f, h * 0.5f)
        } else {
            moveTo(w * 0.95f, h * 0.12f)
            lineTo(w * 0.95f, h * 0.88f)
            lineTo(w * 0.3f, h * 0.5f)
        }
        close()
    }
    drawPath(path = triangle, color = color)
    drawPath(path = triangle, color = color, style = Stroke(width = w * 0.1f, join = StrokeJoin.Round))
    val barX = if (forward) w * 0.88f else w * 0.12f
    drawLine(
        color = color,
        start = Offset(barX, h * 0.12f),
        end = Offset(barX, h * 0.88f),
        strokeWidth = w * 0.16f,
        cap = StrokeCap.Round,
    )
}
