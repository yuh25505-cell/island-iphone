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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.island.phone.app.home.widget.weekDateText
import com.island.phone.app.home.widget.weekDayText
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.files.HomeWidgetImages
import com.island.phone.data.settings.HomeWidgetTextStore
import com.island.phone.ui.dialog.IosMediaEditDialog
import com.island.phone.ui.dialog.IosTextEditDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

private const val HOME_PAGES = 2

private enum class EditTarget { DATE, QUOTE, WEEK, PHOTO }

@Composable
fun HomePager(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val store = remember { HomeWidgetTextStore(context) }
    val scope = rememberCoroutineScope()
    val dateLines by store.dateLines.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.dateLines.first() } },
    )
    val quoteLines by store.quoteLines.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.quoteLines.first() } },
    )
    val weekTexts by store.weekTexts.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.weekTexts.first() } },
    )
    val weekAvatar by store.weekAvatar.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.weekAvatar.first() } },
    )
    val photoSources by store.photoSources.collectAsStateWithLifecycle(
        initialValue = remember { runBlocking { store.photoSources.first() } },
    )
    var editorVisible by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(EditTarget.DATE) }
    val today = remember(editorVisible) { LocalDate.now() }
    val pagerState = rememberPagerState(pageCount = { HOME_PAGES })

    Box(modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = !editorVisible,
            ) { page ->
                if (page == 0) {
                    HomeFirstPage(
                        dateLines = dateLines,
                        quoteLines = quoteLines,
                        onEditDate = {
                            target = EditTarget.DATE
                            editorVisible = true
                        },
                        onEditQuote = {
                            target = EditTarget.QUOTE
                            editorVisible = true
                        },
                    )
                } else {
                    HomeSecondPage(
                        weekTexts = weekTexts,
                        weekAvatar = weekAvatar,
                        photoSources = photoSources,
                        onEditWeek = {
                            target = EditTarget.WEEK
                            editorVisible = true
                        },
                        onEditPhotos = {
                            target = EditTarget.PHOTO
                            editorVisible = true
                        },
                    )
                }
            }
            HomePageDots(pagerState.currentPage, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            HomeDock()
            Spacer(Modifier.height(10.dp))
        }

        IosTextEditDialog(
            visible = editorVisible && (target == EditTarget.DATE || target == EditTarget.QUOTE),
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

        val isWeek = target == EditTarget.WEEK
        IosMediaEditDialog(
            visible = editorVisible && (target == EditTarget.WEEK || target == EditTarget.PHOTO),
            title = if (isWeek) "周日历" else "相片",
            images = if (isWeek) listOf(weekAvatar) else photoSources,
            texts = if (isWeek) {
                listOf(
                    weekTexts.getOrElse(0) { "" },
                    weekTexts.getOrElse(1) { "" },
                    weekTexts.getOrElse(2) { "" }.ifBlank { weekDateText(today) },
                    weekTexts.getOrElse(3) { "" }.ifBlank { weekDayText(today) },
                )
            } else {
                emptyList()
            },
            textLabels = if (isWeek) listOf("左上", "右上", "日期", "星期") else emptyList(),
            maxLength = 8,
            linkHint = "图片链接",
            localHint = "相册图片",
            pickText = "相册",
            cancelText = "取消",
            saveText = "保存",
            resetText = "恢复默认",
            onImport = { uri -> HomeWidgetImages.importPicked(context, uri) },
            onSave = { imgs, txts ->
                scope.launch {
                    if (isWeek) {
                        val saved = HomeWidgetImages.commit(context, imgs, listOf(weekAvatar))
                        val list = txts.toMutableList()
                        if (list.size > 3) {
                            if (list[2].trim() == weekDateText(today)) list[2] = ""
                            if (list[3].trim() == weekDayText(today)) list[3] = ""
                        }
                        store.setWeek(saved.firstOrNull() ?: "", list)
                    } else {
                        val saved = HomeWidgetImages.commit(context, imgs, photoSources)
                        store.setPhotos(saved)
                    }
                }
                editorVisible = false
            },
            onReset = {
                scope.launch {
                    if (isWeek) {
                        HomeWidgetImages.commit(context, emptyList(), listOf(weekAvatar))
                        store.resetWeek()
                    } else {
                        HomeWidgetImages.commit(context, emptyList(), photoSources)
                        store.resetPhotos()
                    }
                }
                editorVisible = false
            },
            onDismiss = { editorVisible = false },
        )
    }
}

@Composable
private fun HomePageDots(current: Int, modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(HOME_PAGES) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(colors.label.copy(alpha = if (index == current) 0.85f else 0.25f), CircleShape),
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
