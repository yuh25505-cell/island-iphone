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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.island.phone.app.home.widget.DateWidget
import com.island.phone.app.home.widget.MusicWidget
import com.island.phone.app.home.widget.QuoteWidget
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.settings.HomeWidgetTextStore
import com.island.phone.ui.dialog.IosTextEditSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val LabelArea = 22.dp
private val WidgetGap = 10.dp
private val WidgetInset = 4.dp
private val EdgeGap = 12.dp

private enum class EditTarget { DATE, QUOTE }

@Composable
fun HomeFirstPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val store = remember { HomeWidgetTextStore(context) }
    val scope = rememberCoroutineScope()
    val dateLines by store.dateLines.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.dateLines.first() } },
    )
    val quoteLines by store.quoteLines.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.quoteLines.first() } },
    )
    var editorVisible by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(EditTarget.DATE) }

    Box(modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                val cellW = maxWidth / HOME_COLUMNS
                val idealCellH = (cellW * 2 - WidgetInset * 2 + LabelArea + WidgetGap) / 2
                val cellH = minOf(idealCellH, (maxHeight - EdgeGap * 2 + WidgetGap) / HOME_ROWS)
                val edge = maxOf(EdgeGap, (maxHeight - cellH * HOME_ROWS + WidgetGap) / 2)
                val iconSize = minOf(60.dp, cellW - 14.dp, cellH - LabelArea)
                val quoteSide = minOf(cellW * 2 - WidgetInset * 2, cellH * 2 - LabelArea - WidgetGap)

                HomeFirstPageLayout.items.forEach { item ->
                    when (item) {
                        is HomeWidgetItem -> {
                            val w = if (item.square) quoteSide else cellW * item.columnSpan - WidgetInset * 2
                            val h = if (item.square) quoteSide else cellH * item.rowSpan - WidgetGap
                            Box(
                                Modifier
                                    .offset(x = cellW * item.column + WidgetInset, y = edge + cellH * item.row)
                                    .size(width = w, height = h),
                            ) {
                                when (item.kind) {
                                    HomeWidgetKind.DATE -> DateWidget(
                                        lines = dateLines,
                                        onClick = {
                                            target = EditTarget.DATE
                                            editorVisible = true
                                        },
                                    )
                                    HomeWidgetKind.QUOTE -> QuoteWidget(
                                        lines = quoteLines,
                                        onClick = {
                                            target = EditTarget.QUOTE
                                            editorVisible = true
                                        },
                                    )
                                    HomeWidgetKind.MUSIC -> MusicWidget()
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
                                    .size(width = cellW, height = iconSize + LabelArea),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                HomeAppIcon(name = item.iconName, size = iconSize)
                                HomeItemLabel(item.label)
                            }
                        }
                    }
                }
            }
            HomePageDots(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            HomeDock()
            Spacer(Modifier.height(10.dp))
        }

        IosTextEditSheet(
            visible = editorVisible,
            title = if (target == EditTarget.DATE) "日期文案" else "语录文案",
            values = if (target == EditTarget.DATE) dateLines else quoteLines,
            maxLength = 30,
            cancelText = "取消",
            saveText = "保存",
            resetText = "恢复默认",
            onSave = { lines ->
                scope.launch {
                    if (target == EditTarget.DATE) store.setDateLines(lines) else store.setQuoteLines(lines)
                }
                editorVisible = false
            },
            onReset = {
                scope.launch {
                    if (target == EditTarget.DATE) store.resetDateLines() else store.resetQuoteLines()
                }
                editorVisible = false
            },
            onDismiss = { editorVisible = false },
        )
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
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(colors.label.copy(alpha = if (index == 0) 0.85f else 0.25f), CircleShape),
            )
        }
    }
}

@Composable
private fun HomeDock() {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val cellW = maxWidth / HOME_COLUMNS
        val iconSize: Dp = minOf(60.dp, cellW - 14.dp)
        Row(Modifier.fillMaxWidth()) {
            HomeFirstPageLayout.dock.forEach { item ->
                Box(Modifier.width(cellW), contentAlignment = Alignment.Center) {
                    HomeAppIcon(name = item.iconName, size = iconSize)
                }
            }
        }
    }
}
