package com.island.phone.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.app.home.widget.DateWidget
import com.island.phone.app.home.widget.MusicWidget
import com.island.phone.app.home.widget.QuoteWidget
import com.island.phone.core.theme.IslandTheme

private val LabelArea = 22.dp

@Composable
fun HomeFirstPage(modifier: Modifier = Modifier) {
    Column(modifier.statusBarsPadding().navigationBarsPadding()) {
        Spacer(Modifier.height(52.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            val cellW = maxWidth / HOME_COLUMNS
            val cellH = maxHeight / HOME_ROWS
            HomeFirstPageLayout.items.forEach { item ->
                Box(
                    Modifier
                        .offset(x = cellW * item.column, y = cellH * item.row)
                        .size(width = cellW * item.columnSpan, height = cellH * item.rowSpan),
                ) {
                    HomeItemView(item, cellW, cellH)
                }
            }
        }
        HomePageDots(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(14.dp))
        HomeDock()
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun HomeItemView(item: HomeItem, cellW: Dp, cellH: Dp) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (item) {
            is HomeAppItem -> {
                val iconSize = minOf(60.dp, cellW - 14.dp, cellH - LabelArea - 2.dp)
                Spacer(Modifier.height(2.dp))
                HomeAppIcon(name = item.iconName, size = iconSize)
            }
            is HomeWidgetItem -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                ) {
                    when (item.kind) {
                        HomeWidgetKind.DATE -> DateWidget()
                        HomeWidgetKind.QUOTE -> QuoteWidget()
                        HomeWidgetKind.MUSIC -> MusicWidget()
                    }
                }
            }
        }
        HomeItemLabel(item.label)
    }
}

@Composable
private fun HomeItemLabel(text: String) {
    val colors = IslandTheme.colors
    Box(Modifier.fillMaxWidth().height(LabelArea), contentAlignment = Alignment.Center) {
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

@Composable
private fun HomePageDots(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    Row(
        modifier
            .background(colors.gray5, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(Color.White.copy(alpha = if (index == 0) 1f else 0.5f), CircleShape),
            )
        }
    }
}

@Composable
private fun HomeDock() {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val cellW = maxWidth / HOME_COLUMNS
        val iconSize = minOf(60.dp, cellW - 14.dp)
        Row(Modifier.fillMaxWidth()) {
            HomeFirstPageLayout.dock.forEach { item ->
                Box(Modifier.width(cellW), contentAlignment = Alignment.Center) {
                    HomeAppIcon(name = item.iconName, size = iconSize)
                }
            }
        }
    }
}
