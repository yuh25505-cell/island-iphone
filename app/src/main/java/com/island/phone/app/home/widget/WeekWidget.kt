package com.island.phone.app.home.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.settings.HomeWidgetTextStore
import com.island.phone.ui.image.SourceImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekNames = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

internal fun weekDateText(today: LocalDate): String =
    today.format(DateTimeFormatter.ofPattern("MM'月'dd'日'"))

internal fun weekDayText(today: LocalDate): String =
    today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.SIMPLIFIED_CHINESE)

@Composable
fun WeekWidget(
    texts: List<String>,
    avatar: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IslandTheme.colors
    val today by rememberToday()
    val leftText = texts.getOrElse(0) { "" }
    val rightText = texts.getOrElse(1) { "" }
    val dateText = texts.getOrElse(2) { "" }.ifBlank { weekDateText(today) }
    val weekText = texts.getOrElse(3) { "" }.ifBlank { weekDayText(today) }
    val start = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val blockColor = widgetBackground(colors.gray5)

    BoxWithConstraints(modifier.fillMaxSize().widgetTap(onClick)) {
        val widgetHeight = maxHeight
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val sideMax = ((maxWidth - maxHeight) / 2 - 4.dp).coerceAtLeast(40.dp)
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(colors.gray4),
                ) {
                    if (avatar.isNotBlank()) SourceImage(avatar, Modifier.fillMaxSize())
                }
                Column(
                    Modifier.align(Alignment.CenterStart).fillMaxHeight().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                ) {
                    GlyphPill(leftText, blockColor, sideMax)
                    GlyphPill(dateText, blockColor, sideMax)
                }
                Column(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    GlyphPill(rightText, blockColor, sideMax)
                    GlyphPill(weekText, blockColor, sideMax)
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(widgetHeight * 0.36f)
                    .background(blockColor, RoundedCornerShape(14.dp))
                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            ) {
                Row(Modifier.fillMaxWidth().weight(0.4f)) {
                    weekNames.forEach { name ->
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            BasicText(
                                text = name,
                                style = TextStyle(
                                    color = colors.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .background(colors.groupedCard, RoundedCornerShape(12.dp)),
                ) {
                    for (i in 0 until 7) {
                        val day = start.plusDays(i.toLong())
                        val isToday = day == today
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(if (isToday) colors.gray else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = day.dayOfMonth.toString(),
                                    style = TextStyle(
                                        color = colors.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface GlyphPart {
    class Text(val value: String) : GlyphPart
    object Headphone : GlyphPart
    object Play : GlyphPart
    object Wave : GlyphPart
}

private fun parseGlyphText(raw: String): List<GlyphPart> {
    val tokens: List<Pair<String, GlyphPart>> = listOf(
        HomeWidgetTextStore.GlyphHeadphone to GlyphPart.Headphone,
        HomeWidgetTextStore.GlyphPlay to GlyphPart.Play,
        HomeWidgetTextStore.GlyphWave to GlyphPart.Wave,
    )
    val out = mutableListOf<GlyphPart>()
    val buffer = StringBuilder()
    var i = 0
    while (i < raw.length) {
        val hit = tokens.firstOrNull { raw.startsWith(it.first, i) }
        if (hit != null) {
            if (buffer.isNotEmpty()) {
                out.add(GlyphPart.Text(buffer.toString()))
                buffer.setLength(0)
            }
            out.add(hit.second)
            i += hit.first.length
        } else {
            buffer.append(raw[i])
            i++
        }
    }
    if (buffer.isNotEmpty()) out.add(GlyphPart.Text(buffer.toString()))
    return out
}

@Composable
private fun GlyphPill(text: String, color: Color, maxWidth: Dp) {
    if (text.isBlank()) {
        Spacer(Modifier.height(28.dp))
    } else {
        InfoPill(color, maxWidth) { PillContent(text) }
    }
}

@Composable
private fun InfoPill(color: Color, maxWidth: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .height(28.dp)
            .widthIn(max = maxWidth)
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.PillContent(text: String) {
    val colors = IslandTheme.colors
    val parts = remember(text) { parseGlyphText(text) }
    parts.forEach { part ->
        when (part) {
            is GlyphPart.Text -> PillText(part.value)
            GlyphPart.Headphone -> HeadphoneGlyph(colors.label, 12.dp)
            GlyphPart.Play -> PlayGlyph(colors.label)
            GlyphPart.Wave -> WaveGlyph(colors.label)
        }
    }
}

@Composable
private fun RowScope.PillText(text: String) {
    val colors = IslandTheme.colors
    BasicText(
        text = text,
        modifier = Modifier.weight(1f, fill = false),
        style = TextStyle(
            color = colors.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeadphoneGlyph(color: Color, size: Dp) {
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
private fun PlayGlyph(color: Color) {
    Canvas(Modifier.size(width = 5.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        val play = Path().apply {
            moveTo(0f, h * 0.2f)
            lineTo(0f, h * 0.8f)
            lineTo(w, h * 0.5f)
            close()
        }
        drawPath(path = play, color = color)
    }
}

@Composable
private fun WaveGlyph(color: Color) {
    Canvas(Modifier.size(width = 31.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val heights = listOf(0.3f, 0.7f, 0.45f, 1f, 0.55f, 0.8f, 0.35f)
        val left = stroke / 2f
        val gap = (w - stroke) / (heights.size - 1)
        heights.forEachIndexed { i, ratio ->
            val x = left + gap * i
            drawLine(
                color = color,
                start = Offset(x, h * (0.5f - ratio / 2f)),
                end = Offset(x, h * (0.5f + ratio / 2f)),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
