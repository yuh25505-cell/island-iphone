package com.island.phone.app.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.island.phone.app.home.widget.DateWidget
import com.island.phone.app.home.widget.QuoteWidget
import com.island.phone.app.home.widget.WordWidget

@Composable
fun HomeFirstPage(
    dateLines: List<String>,
    quoteLines: List<String>,
    onEditDate: () -> Unit,
    onEditQuote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        val m = homeGridMetrics(maxWidth, maxHeight)
        val cellW = m.cellW
        val cellH = m.cellH
        val edge = m.edge
        val iconSize = m.iconSize
        val quoteSide = minOf(cellW * 2 - HomeWidgetInset * 2, cellH * 2 - HomeLabelArea - HomeWidgetGap)

        HomeFirstPageLayout.items.forEach { item ->
            when (item) {
                is HomeWidgetItem -> {
                    val w = if (item.square) quoteSide else cellW * item.columnSpan - HomeWidgetInset * 2
                    val h = if (item.square) quoteSide else cellH * item.rowSpan - HomeWidgetGap
                    Box(
                        Modifier
                            .offset(x = cellW * item.column + HomeWidgetInset, y = edge + cellH * item.row)
                            .size(width = w, height = h),
                    ) {
                        when (item.kind) {
                            HomeWidgetKind.DATE -> DateWidget(lines = dateLines, onClick = onEditDate)
                            HomeWidgetKind.QUOTE -> QuoteWidget(lines = quoteLines, onClick = onEditQuote)
                            HomeWidgetKind.WORD -> WordWidget()
                            HomeWidgetKind.WEEK, HomeWidgetKind.PHOTOS -> Unit
                        }
                    }
                }
                is HomeAppItem -> {
                    val top = if (item.row in 2..3) {
                        cellH * 2 + (quoteSide - iconSize) * (item.row - 2)
                    } else {
                        cellH * item.row
                    }
                    Column(
                        Modifier
                            .offset(x = cellW * item.column, y = edge + top)
                            .size(width = cellW, height = iconSize + HomeLabelArea),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HomeAppIcon(name = item.iconName, size = iconSize)
                        HomeItemLabel(item.label)
                    }
                }
            }
        }
    }
}
