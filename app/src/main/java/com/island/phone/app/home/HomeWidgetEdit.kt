package com.island.phone.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.island.phone.core.theme.IslandTheme

@Composable
internal fun HomeWidgetRemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = IslandTheme.colors
    Box(
        modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            Modifier
                .padding(6.dp)
                .size(24.dp)
                .background(colors.label.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(10.dp)) {
                drawLine(
                    color = colors.background,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
internal fun HomeWidgetAddButton(onClick: () -> Unit) {
    val colors = IslandTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(colors.fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(15.dp)) {
                val w = 1.6.dp.toPx()
                drawRoundRect(
                    color = colors.accent,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(width = w),
                )
                drawLine(
                    color = colors.accent,
                    start = Offset(size.width * 0.3f, size.height / 2f),
                    end = Offset(size.width * 0.7f, size.height / 2f),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.accent,
                    start = Offset(size.width / 2f, size.height * 0.3f),
                    end = Offset(size.width / 2f, size.height * 0.7f),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
