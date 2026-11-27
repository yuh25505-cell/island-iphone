package com.island.phone.app.home.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Composable
fun MusicWidget(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.gray6, RoundedCornerShape(30.dp)),
    ) {
        val k = (maxHeight.value / 166f).coerceIn(0.8f, 1f)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = (12 * k).dp),
        ) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "차차차",
                        style = TextStyle(
                            color = colors.label,
                            fontSize = (22 * k).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = "Vince(feat. G-DRAGON)",
                        style = TextStyle(
                            color = colors.secondaryLabel,
                            fontSize = (15 * k).sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = "•••",
                        style = TextStyle(
                            color = colors.label,
                            fontSize = (16 * k).sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .background(colors.gray5, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "★",
                        style = TextStyle(color = colors.gray, fontSize = (28 * k).sp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(
                    text = "0:52",
                    style = TextStyle(color = colors.label, fontSize = (14 * k).sp),
                )
                BasicText(
                    text = "-2:28",
                    style = TextStyle(color = colors.label, fontSize = (14 * k).sp),
                )
            }
            MusicProgress(
                progress = 0.22f,
                trackColor = colors.label.copy(alpha = 0.15f),
                fillColor = colors.label,
            )
            Spacer(Modifier.height((6 * k).dp))
            Row(
                Modifier.fillMaxWidth().height((28 * k).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MusicIcon(MusicIconKind.QUEUE, colors.label, (22 * k).dp)
                MusicIcon(MusicIconKind.REPEAT, colors.label, (22 * k).dp)
                MusicIcon(MusicIconKind.PREVIOUS, colors.label, (28 * k).dp)
                MusicIcon(MusicIconKind.PLAY, colors.label, (26 * k).dp)
                MusicIcon(MusicIconKind.NEXT, colors.label, (28 * k).dp)
                MusicIcon(MusicIconKind.HEART, colors.label, (22 * k).dp)
                MusicIcon(MusicIconKind.MUTE, colors.label, (22 * k).dp)
            }
        }
    }
}

@Composable
private fun MusicProgress(progress: Float, trackColor: Color, fillColor: Color) {
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val strokeWidth = 4.dp.toPx()
        val cy = size.height / 2f
        val start = strokeWidth / 2f
        val end = size.width - strokeWidth / 2f
        val x = start + (end - start) * progress
        drawLine(
            color = trackColor,
            start = Offset(start, cy),
            end = Offset(end, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = fillColor,
            start = Offset(start, cy),
            end = Offset(x, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = fillColor, radius = 6.dp.toPx(), center = Offset(x, cy))
    }
}
