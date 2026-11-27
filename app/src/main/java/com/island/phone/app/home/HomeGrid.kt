package com.island.phone.app.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

internal val HomeLabelArea = 22.dp
internal val HomeWidgetGap = 15.dp
internal val HomeWidgetInset = 4.dp
internal val HomeEdgeGap = 12.dp

@Immutable
internal class HomeGridMetrics(
    val cellW: Dp,
    val cellH: Dp,
    val edge: Dp,
    val iconSize: Dp,
)

internal fun homeGridMetrics(width: Dp, height: Dp): HomeGridMetrics {
    val cellW = width / HOME_COLUMNS
    val idealCellH = (cellW * 2 - HomeWidgetInset * 2 + HomeLabelArea + HomeWidgetGap) / 2
    val cellH = minOf(idealCellH, (height - HomeEdgeGap * 2 + HomeWidgetGap) / HOME_ROWS)
    val edge = maxOf(HomeEdgeGap, (height - cellH * HOME_ROWS + HomeWidgetGap) / 2)
    val iconSize = minOf(60.dp, cellW - 14.dp, cellH - HomeLabelArea)
    return HomeGridMetrics(cellW = cellW, cellH = cellH, edge = edge, iconSize = iconSize)
}

@Composable
internal fun HomeItemLabel(text: String) {
    val colors = IslandTheme.colors
    Box(Modifier.fillMaxWidth().height(HomeLabelArea), contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = TextStyle(
                color = colors.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
