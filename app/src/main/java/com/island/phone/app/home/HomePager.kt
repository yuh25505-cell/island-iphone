package com.island.phone.app.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.app.home.widget.weekDateText
import com.island.phone.app.home.widget.weekDayText
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.files.HomeWidgetImages
import com.island.phone.data.settings.HomeLayoutStore
import com.island.phone.data.settings.HomeWidgetTextStore
import com.island.phone.ui.dialog.IosActionSheet
import com.island.phone.ui.dialog.IosMediaEditDialog
import com.island.phone.ui.dialog.IosTextEditDialog
import com.island.phone.ui.dialog.SheetAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

@Composable
fun HomePager(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    val context = LocalContext.current.applicationContext
    val store = remember { HomeWidgetTextStore(context) }
    val layoutStore = remember { HomeLayoutStore(context) }
    val scope = rememberCoroutineScope()
    var dateLines by remember { mutableStateOf(HomeWidgetTextStore.DefaultDateLines) }
    var quoteLines by remember { mutableStateOf(HomeWidgetTextStore.DefaultQuoteLines) }
    var weekTexts by remember { mutableStateOf(HomeWidgetTextStore.DefaultWeekTexts) }
    var weekAvatar by remember { mutableStateOf("") }
    var photoSources by remember { mutableStateOf(HomeWidgetTextStore.DefaultPhotoSources) }
    var photoCaptions by remember { mutableStateOf(HomeWidgetTextStore.DefaultPhotoCaptions) }
    val layout = remember { HomeLayoutState.load(runBlocking { layoutStore.placements.first() }) }
    val geo = remember { HomeGeometry() }
    geo.density = LocalDensity.current.density

    var editorVisible by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(EditTarget.DATE) }
    var editCopy by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var lastDragId by remember { mutableStateOf<String?>(null) }
    var dragFromDock by remember { mutableStateOf(false) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    var dragGrab by remember { mutableStateOf(Offset.Zero) }
    var dragSize by remember { mutableStateOf(Offset.Zero) }
    var edgeDir by remember { mutableIntStateOf(0) }
    var openFolder by remember { mutableStateOf<Int?>(null) }
    var lastFolder by remember { mutableIntStateOf(0) }
    var folderMenu by remember { mutableStateOf(false) }
    var renameVisible by remember { mutableStateOf(false) }
    var movingApp by remember { mutableStateOf<String?>(null) }
    var pageMenu by remember { mutableStateOf(false) }
    var widgetMenu by remember { mutableStateOf(false) }
    var removingWidget by remember { mutableStateOf<String?>(null) }
    var styleTarget by remember { mutableStateOf<HomeWidgetItem?>(null) }
    var scrollTo by remember { mutableStateOf<Int?>(null) }
    val today = remember(editorVisible) { LocalDate.now() }
    val pagerState = rememberPagerState(pageCount = { layout.pageCount })
    val angle = rememberWobbleAngle(editing)
    val shownFolder = openFolder ?: lastFolder
    val saveLayout: () -> Unit = { scope.launch { layoutStore.save(layout.encode()) } }
    val openFolderAt: (Int) -> Unit = { n ->
        lastFolder = n
        openFolder = n
    }
    val openEditor: (Int, EditTarget) -> Unit = { copy, kind ->
        when (kind) {
            EditTarget.DATE -> dateLines = runBlocking { store.dateLinesOf(copy).first() }
            EditTarget.QUOTE -> quoteLines = runBlocking { store.quoteLinesOf(copy).first() }
            EditTarget.WEEK -> {
                weekTexts = runBlocking { store.weekTextsOf(copy).first() }
                weekAvatar = runBlocking { store.weekAvatarOf(copy).first() }
            }
            EditTarget.PHOTO -> {
                photoSources = runBlocking { store.photoSourcesOf(copy).first() }
                photoCaptions = runBlocking { store.photoCaptionsOf(copy).first() }
            }
        }
        editCopy = copy
        target = kind
        editorVisible = true
    }

    BackHandler(enabled = editing) {
        editing = false
        dragId = null
        edgeDir = 0
    }

    BackHandler(enabled = openFolder != null) {
        openFolder = null
    }

    LaunchedEffect(dragId, edgeDir) {
        if (dragId != null && edgeDir != 0) {
            delay(550)
            val wanted = pagerState.currentPage + edgeDir
            if (wanted >= layout.pageCount && layout.addPage()) {
                saveLayout()
                delay(80)
            }
            pagerState.animateScrollToPage(wanted.coerceIn(0, layout.pageCount - 1))
        }
    }

    LaunchedEffect(scrollTo) {
        val t = scrollTo
        if (t != null) {
            delay(80)
            pagerState.animateScrollToPage(t.coerceIn(0, layout.pageCount - 1))
            scrollTo = null
        }
    }

    BoxWithConstraints(
        modifier
            .onGloballyPositioned { c ->
                val p = c.positionInWindow()
                geo.rootX = p.x
                geo.rootY = p.y
                geo.rootW = c.size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (!editorVisible && openFolder == null) {
                            editing = true
                            val hit = geo.hit(layout, pagerState.currentPage, offset.x, offset.y)
                            if (hit != null && !pagerState.isScrollInProgress) {
                                dragId = hit.id
                                lastDragId = hit.id
                                dragFromDock = hit.fromDock
                                dragSize = Offset(hit.width, hit.height)
                                dragGrab = Offset(
                                    (offset.x + geo.rootX) / geo.density - hit.left,
                                    (offset.y + geo.rootY) / geo.density - hit.top,
                                )
                                dragPointer = offset
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragId != null) {
                            change.consume()
                            dragPointer = change.position
                            val edgePx = 28f * geo.density
                            edgeDir = if (change.position.x < edgePx) {
                                -1
                            } else if (change.position.x > geo.rootW - edgePx) {
                                1
                            } else {
                                0
                            }
                        }
                    },
                    onDragEnd = {
                        val id = dragId
                        if (id != null) {
                            val drop = geo.drop(
                                layout, id, pagerState.currentPage,
                                dragPointer.x, dragPointer.y, dragGrab.x, dragGrab.y,
                            )
                            if (drop != null) {
                                var moved = false
                                val mergeId = drop.mergeId
                                if (mergeId != null) moved = layout.mergeIntoFolder(id, mergeId)
                                if (!moved) {
                                    moved = if (drop.dockIndex >= 0) {
                                        layout.moveToDock(id, drop.dockIndex)
                                    } else {
                                        layout.moveToGrid(id, drop.page, drop.column, drop.row)
                                    }
                                }
                                if (moved) scope.launch { layoutStore.save(layout.encode()) }
                            }
                        }
                        dragId = null
                        edgeDir = 0
                    },
                    onDragCancel = {
                        dragId = null
                        edgeDir = 0
                    },
                )
            },
    ) {
        val dockCellW = (maxWidth - 32.dp) / HOME_COLUMNS
        val dockIcon: Dp = minOf(60.dp, dockCellW - 14.dp)
        SideEffect { geo.dockIcon = dockIcon.value }

        val itemContent: @Composable (HomeItem, Boolean, Dp) -> Unit = { item, inDock, iconSize ->
            when (item) {
                is HomeWidgetItem -> Box(Modifier.fillMaxSize()) {
                    HomeWidgetBody(
                        item = item,
                        store = store,
                        editing = editing,
                        onEdit = { kind -> openEditor(item.copy, kind) },
                    )
                    if (editing) {
                        HomeWidgetRemoveBadge(Modifier.align(Alignment.TopStart)) { removingWidget = item.id }
                        HomeWidgetStyleBadge(Modifier.align(Alignment.TopEnd)) { styleTarget = item }
                    }
                }
                is HomeAppItem -> if (item.isFolder()) {
                    val number = folderNumber(item.id) ?: 0
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        HomeFolderIcon(
                            apps = layout.folderApps(number),
                            iconSize = iconSize,
                            onClick = { if (!editing) openFolderAt(number) },
                        )
                        HomeItemLabel(layout.folderName(number))
                    }
                } else if (inDock) {
                    HomeAppIcon(name = item.iconName, size = iconSize)
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        HomeAppIcon(name = item.iconName, size = iconSize)
                        HomeItemLabel(item.label)
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { c ->
                        val p = c.positionInWindow()
                        geo.pagerX = p.x
                        geo.pagerY = p.y
                    },
            ) {
                val m = remember(maxWidth, maxHeight) { homeGridMetrics(maxWidth - 32.dp, maxHeight) }
                SideEffect { geo.metrics = m }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !editorVisible && dragId == null && openFolder == null,
                ) { page ->
                    HomePage(
                        page = page,
                        layout = layout,
                        metrics = m,
                        hiddenId = dragId,
                        editing = editing,
                        angle = angle,
                        snapId = lastDragId,
                        content = itemContent,
                    )
                }
            }
            HomePageDots(layout.pageCount, pagerState.currentPage, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            HomeDock(
                layout = layout,
                cellW = dockCellW,
                iconSize = dockIcon,
                hiddenId = dragId,
                editing = editing,
                angle = angle,
                onPlaced = { c ->
                    val p = c.positionInWindow()
                    geo.dockX = p.x
                    geo.dockY = p.y
                    geo.dockW = c.size.width.toFloat()
                    geo.dockH = c.size.height.toFloat()
                },
                content = itemContent,
            )
            Spacer(Modifier.height(10.dp))
        }

        val activeItem = dragId?.let { HomeCatalog.items[it] }
        if (activeItem != null) {
            val d = geo.density
            val overlayIcon = if (dragFromDock) dockIcon else (geo.metrics?.iconSize ?: dockIcon)
            Box(
                Modifier
                    .offset(x = (dragPointer.x / d - dragGrab.x).dp, y = (dragPointer.y / d - dragGrab.y).dp)
                    .size(width = dragSize.x.dp, height = dragSize.y.dp)
                    .graphicsLayer {
                        scaleX = 1.06f
                        scaleY = 1.06f
                        alpha = 0.95f
                    },
            ) {
                itemContent(activeItem, dragFromDock, overlayIcon)
            }
        }

        if (editing) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 8.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeRoundButton(plus = true) {
                    if (layout.addPage()) {
                        saveLayout()
                        scrollTo = layout.pageCount - 1
                    }
                }
                if (layout.pageCount > 1) {
                    HomeRoundButton(plus = false) { pageMenu = true }
                }
                HomeWidgetAddButton { widgetMenu = true }
            }

            val doneSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .height(44.dp)
                    .clickable(interactionSource = doneSource, indication = null) {
                        editing = false
                        dragId = null
                        edgeDir = 0
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .height(30.dp)
                        .background(colors.fill, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "完成",
                        style = TextStyle(
                            color = colors.accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        HomeFolderOverlay(
            visible = openFolder != null,
            name = layout.folderName(shownFolder),
            apps = layout.folderApps(shownFolder),
            onNameClick = { folderMenu = true },
            onAppLongPress = { appId -> movingApp = appId },
            onAppDragOut = { appId ->
                if (layout.removeFromFolder(appId)) {
                    if (layout.find(folderIdOf(shownFolder)) == null) openFolder = null
                    saveLayout()
                }
            },
            onAppReorder = { appId, index ->
                if (layout.reorderInFolder(appId, index)) saveLayout()
            },
            onDismiss = { openFolder = null },
        )

        IosActionSheet(
            visible = pageMenu,
            actions = listOf(
                SheetAction(title = "删除这一页", destructive = true) {
                    pageMenu = false
                    val page = pagerState.currentPage
                    if (layout.deletePage(page)) {
                        saveLayout()
                        scrollTo = minOf(page, layout.pageCount - 1)
                    }
                },
            ),
            cancelText = "取消",
            onDismiss = { pageMenu = false },
        )

        IosActionSheet(
            visible = widgetMenu,
            actions = HomeWidgetKind.values().map { kind ->
                SheetAction(title = HomeCatalog.items[HomeCatalog.widgetBaseId(kind)]?.label ?: "") {
                    widgetMenu = false
                    val placedId = layout.addWidgetKind(kind, pagerState.currentPage)
                    if (placedId != null) {
                        saveLayout()
                        val placed = layout.find(placedId)?.page ?: -1
                        if (placed >= 0) scrollTo = placed
                    }
                }
            },
            cancelText = "取消",
            onDismiss = { widgetMenu = false },
        )

        IosActionSheet(
            visible = removingWidget != null,
            actions = listOf(
                SheetAction(title = "移除组件", destructive = true) {
                    val widgetId = removingWidget
                    removingWidget = null
                    if (widgetId != null) {
                        val copyInfo = HomeCatalog.widgetCopyOf(widgetId)
                        if (layout.removeWidget(widgetId)) {
                            saveLayout()
                            if (copyInfo != null) {
                                val kind = copyInfo.first
                                val copy = copyInfo.second
                                scope.launch {
                                    when (kind) {
                                        HomeWidgetKind.DATE -> store.resetDateLines(copy)
                                        HomeWidgetKind.QUOTE -> store.resetQuoteLines(copy)
                                        HomeWidgetKind.WEEK -> {
                                            val old = store.weekAvatarOf(copy).first()
                                            HomeWidgetImages.commit(context, emptyList(), listOf(old))
                                            store.resetWeek(copy)
                                        }
                                        HomeWidgetKind.PHOTOS -> {
                                            val old = store.photoSourcesOf(copy).first()
                                            HomeWidgetImages.commit(context, emptyList(), old)
                                            store.resetPhotos(copy)
                                        }
                                        HomeWidgetKind.WORD -> {}
                                    }
                                    store.resetStyle(kind.name, copy)
                                }
                            }
                        }
                    }
                },
            ),
            cancelText = "取消",
            onDismiss = { removingWidget = null },
        )

        IosActionSheet(
            visible = folderMenu,
            actions = listOf(
                SheetAction(title = "重命名") {
                    folderMenu = false
                    renameVisible = true
                },
                SheetAction(title = "解散文件夹", destructive = true) {
                    folderMenu = false
                    if (layout.dissolveFolder(shownFolder)) {
                        openFolder = null
                        saveLayout()
                    }
                },
            ),
            cancelText = "取消",
            onDismiss = { folderMenu = false },
        )

        IosActionSheet(
            visible = movingApp != null,
            actions = listOf(
                SheetAction(title = "移出文件夹") {
                    val id = movingApp
                    movingApp = null
                    if (id != null && layout.removeFromFolder(id)) {
                        if (layout.find(folderIdOf(shownFolder)) == null) openFolder = null
                        saveLayout()
                    }
                },
            ),
            cancelText = "取消",
            onDismiss = { movingApp = null },
        )

        IosTextEditDialog(
            visible = renameVisible,
            title = "文件夹名称",
            values = listOf(layout.folderName(shownFolder)),
            maxLength = 12,
            cancelText = "取消",
            saveText = "保存",
            resetText = "恢复默认",
            onSave = { lines ->
                layout.renameFolder(shownFolder, lines.firstOrNull() ?: "")
                renameVisible = false
                saveLayout()
            },
            onReset = {
                layout.renameFolder(shownFolder, "")
                renameVisible = false
                saveLayout()
            },
            onDismiss = { renameVisible = false },
        )

        IosTextEditDialog(
            visible = editorVisible && (target == EditTarget.DATE || target == EditTarget.QUOTE),
            title = if (target == EditTarget.DATE) "日期文案" else "语录文案",
            values = if (target == EditTarget.DATE) dateLines else quoteLines,
            maxLength = 30,
            cancelText = "取消",
            saveText = "保存",
            resetText = "恢复默认",
            onSave = { lines ->
                val copy = editCopy
                val isDate = target == EditTarget.DATE
                scope.launch {
                    if (isDate) store.setDateLines(lines, copy) else store.setQuoteLines(lines, copy)
                }
                editorVisible = false
            },
            onReset = {
                val copy = editCopy
                val isDate = target == EditTarget.DATE
                scope.launch {
                    if (isDate) store.resetDateLines(copy) else store.resetQuoteLines(copy)
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
                photoCaptions
            },
            textLabels = if (isWeek) {
                listOf("左上", "右上", "日期", "星期")
            } else {
                listOf("第一", "第二", "第三", "第四")
            },
            maxLength = if (isWeek) 20 else 12,
            linkHint = "图片链接",
            localHint = "相册图片",
            pickText = "相册",
            cancelText = "取消",
            saveText = "保存",
            resetText = "恢复默认",
            onImport = { uri -> HomeWidgetImages.importPicked(context, uri) },
            onSave = { imgs, txts ->
                val copy = editCopy
                scope.launch {
                    if (isWeek) {
                        val saved = HomeWidgetImages.commit(context, imgs, listOf(weekAvatar))
                        val list = txts.toMutableList()
                        if (list.size > 3) {
                            if (list[2].trim() == weekDateText(today)) list[2] = ""
                            if (list[3].trim() == weekDayText(today)) list[3] = ""
                        }
                        store.setWeek(saved.firstOrNull() ?: "", list, copy)
                    } else {
                        val saved = HomeWidgetImages.commit(context, imgs, photoSources)
                        store.setPhotos(saved, txts, copy)
                    }
                }
                editorVisible = false
            },
            onReset = {
                val copy = editCopy
                scope.launch {
                    if (isWeek) {
                        HomeWidgetImages.commit(context, emptyList(), listOf(weekAvatar))
                        store.resetWeek(copy)
                    } else {
                        HomeWidgetImages.commit(context, emptyList(), photoSources)
                        store.resetPhotos(copy)
                    }
                }
                editorVisible = false
            },
            onDismiss = { editorVisible = false },
        )

        HomeWidgetStyleController(
            trigger = styleTarget,
            store = store,
            onConsumed = { styleTarget = null },
        )
    }
}

@Composable
private fun HomePageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(colors.label.copy(alpha = if (index == current) 0.85f else 0.25f), CircleShape),
            )
        }
    }
}

@Composable
private fun HomeRoundButton(plus: Boolean, onClick: () -> Unit) {
    val colors = IslandTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(44.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(colors.fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) {
                val w = 2.dp.toPx()
                drawLine(
                    color = colors.accent,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
                if (plus) {
                    drawLine(
                        color = colors.accent,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = w,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDock(
    layout: HomeLayoutState,
    cellW: Dp,
    iconSize: Dp,
    hiddenId: String?,
    editing: Boolean,
    angle: State<Float>,
    onPlaced: (LayoutCoordinates) -> Unit,
    content: @Composable (HomeItem, Boolean, Dp) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(iconSize)
            .onGloballyPositioned(onPlaced),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        layout.dock().forEach { p ->
            val item = HomeCatalog.items[p.id]
            if (item != null) {
                key(p.id) {
                    Box(
                        Modifier
                            .width(cellW)
                            .height(iconSize)
                            .homeWobble(item, editing, angle),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (p.id != hiddenId) content(item, true, iconSize)
                    }
                }
            }
        }
    }
}
