package com.island.phone.app.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.island.phone.app.home.widget.PhotoWidget
import com.island.phone.app.home.widget.WeekWidget

@Composable
fun HomeSecondPage(
    weekTexts: List<String>,
    weekAvatar: String,
    photoSources: List<String>,
    onEditWeek: () -> Unit,
    onEditPhotos: () -> Unit,
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

        HomeSecondPageLayout.items.forEach { item ->
            when (item) {
                is HomeWidgetItem -> {
                    Box(
                        Modifier
                            .offset(x = cellW * item.column + HomeWidgetInset, y = edge + cellH * item.row)
                            .size(
                                width = cellW * item.columnSpan - HomeWidgetInset * 2,
                                height = cellH * item.rowSpan - HomeWidgetGap,
                            ),
                    ) {
                        when (item.kind) {
                            HomeWidgetKind.WEEK -> WeekWidget(
                                texts = weekTexts,
                                avatar = weekAvatar,
                                onClick = onEditWeek,
                            )
                            HomeWidgetKind.PHOTOS -> PhotoWidget(
                                sources = photoSources,
                                onClick = onEditPhotos,
                            )
                            HomeWidgetKind.DATE, HomeWidgetKind.QUOTE, HomeWidgetKind.WORD -> Unit
                        }
                    }
                }
                is HomeAppItem -> {
                    Column(
                        Modifier
                            .offset(x = cellW * item.column, y = edge + cellH * item.row)
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
