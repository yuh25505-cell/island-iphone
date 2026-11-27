package com.island.phone.app.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

internal const val DOCK_PAGE = -1
internal const val HOME_PAGE_COUNT = 2
internal const val HOME_PAGE_MAX = 8
internal const val DOCK_MAX = 4

internal const val WIDGET_ID_PREFIX = "widget_"
internal const val WIDGET_COPY_MARK = "~"

internal val HomeItem.id: String
    get() = when (this) {
        is HomeAppItem -> "app_$iconName"
        is HomeWidgetItem -> if (copy == 0) {
            "$WIDGET_ID_PREFIX${kind.name}"
        } else {
            "$WIDGET_ID_PREFIX${kind.name}$WIDGET_COPY_MARK$copy"
        }
    }

@Immutable
internal class HomePlacement(val id: String, val page: Int, val column: Int, val row: Int)

internal object HomeCatalog {
    val items: MutableMap<String, HomeItem> = buildMap<String, HomeItem> {
        (HomeFirstPageLayout.items + HomeSecondPageLayout.items + HomeFirstPageLayout.dock).forEach { put(it.id, it) }
    }.toMutableMap()

    val baseIds: Set<String> = items.keys.toSet()

    val defaults: List<HomePlacement> = buildList {
        HomeFirstPageLayout.items.forEach { add(HomePlacement(it.id, 0, it.column, it.row)) }
        HomeSecondPageLayout.items.forEach { add(HomePlacement(it.id, 1, it.column, it.row)) }
        HomeFirstPageLayout.dock.forEachIndexed { index, item -> add(HomePlacement(item.id, DOCK_PAGE, index, 0)) }
    }

    fun registerFolder(number: Int) {
        items[folderIdOf(number)] = HomeAppItem(column = 0, row = 0, label = "", iconName = "$FOLDER_ICON_PREFIX$number")
    }

    fun clearFolders() {
        items.keys.filter { it.startsWith(FOLDER_ID_PREFIX) }.forEach { items.remove(it) }
    }

    fun widgetBaseId(kind: HomeWidgetKind): String = "$WIDGET_ID_PREFIX${kind.name}"

    fun widgetCopyOf(id: String): Pair<HomeWidgetKind, Int>? {
        if (!id.startsWith(WIDGET_ID_PREFIX)) return null
        val body = id.removePrefix(WIDGET_ID_PREFIX)
        val mark = body.indexOf(WIDGET_COPY_MARK)
        if (mark < 0) return null
        val kind = HomeWidgetKind.values().firstOrNull { it.name == body.substring(0, mark) } ?: return null
        val number = body.substring(mark + 1).toIntOrNull() ?: return null
        if (number < 1) return null
        return Pair(kind, number)
    }

    fun registerWidgetCopy(kind: HomeWidgetKind, number: Int): String? {
        val base = items[widgetBaseId(kind)] as? HomeWidgetItem ?: return null
        val item = HomeWidgetItem(
            column = base.column,
            row = base.row,
            columnSpan = base.columnSpan,
            rowSpan = base.rowSpan,
            label = base.label,
            kind = kind,
            square = base.square,
            copy = number,
        )
        items[item.id] = item
        return item.id
    }

    fun clearWidgetCopies() {
        items.keys.filter { widgetCopyOf(it) != null }.forEach { items.remove(it) }
    }
}

@Stable
internal class HomeLayoutState private constructor() {
    private val list = mutableStateListOf<HomePlacement>()
    private val folderNames = mutableStateMapOf<Int, String>()
    private val hidden = mutableStateListOf<String>()

    var pageCount by mutableIntStateOf(HOME_PAGE_COUNT)
        private set

    fun onPage(page: Int): List<HomePlacement> = list.filter { it.page == page }

    fun dock(): List<HomePlacement> = list.filter { it.page == DOCK_PAGE }.sortedBy { it.column }

    fun find(id: String): HomePlacement? = list.firstOrNull { it.id == id }

    fun folderApps(number: Int): List<String> =
        list.filter { it.page == FOLDER_PAGE && it.row == number }.sortedBy { it.column }.map { it.id }

    fun folderName(number: Int): String = folderNames[number] ?: FOLDER_DEFAULT_NAME

    fun hiddenWidgets(): List<String> = hidden.toList()

    fun encode(): String {
        val parts = ArrayList<String>()
        list.forEach { parts.add("${it.id}:${it.page}:${it.column}:${it.row}") }
        folderNames.entries.forEach { parts.add("#${it.key}=${it.value}") }
        hidden.forEach { parts.add("!$it") }
        parts.add("@$pageCount")
        return parts.joinToString(";")
    }

    fun addPage(): Boolean {
        if (pageCount >= HOME_PAGE_MAX) return false
        pageCount += 1
        return true
    }

    fun deletePage(page: Int): Boolean {
        if (pageCount <= 1 || page !in 0 until pageCount) return false
        val snapshot = list.toList()
        val moving = onPage(page).sortedByDescending { p ->
            HomeCatalog.items[p.id]?.let { it.columnSpan * it.rowSpan } ?: 0
        }
        for (p in moving) {
            var placed = false
            for (other in 0 until pageCount) {
                if (other == page) continue
                val spot = nearestFree(other, p.column, p.row, p.id) ?: continue
                put(HomePlacement(p.id, other, spot.first, spot.second))
                placed = true
                break
            }
            if (!placed) {
                list.clear()
                list.addAll(snapshot)
                return false
            }
        }
        list.filter { it.page > page }.forEach { put(HomePlacement(it.id, it.page - 1, it.column, it.row)) }
        pageCount -= 1
        return true
    }

    fun removeWidget(id: String): Boolean {
        if (HomeCatalog.items[id] !is HomeWidgetItem) return false
        if (find(id) == null) return false
        list.removeAll { it.id == id }
        if (HomeCatalog.widgetCopyOf(id) != null) {
            HomeCatalog.items.remove(id)
        } else if (id !in hidden) {
            hidden.add(id)
        }
        return true
    }

    fun addWidget(id: String, page: Int): Boolean {
        if (id !in hidden) return false
        if (!placeNear(id, page, 0, 0)) return false
        hidden.remove(id)
        return true
    }

    fun addWidgetKind(kind: HomeWidgetKind, page: Int): String? {
        val baseId = HomeCatalog.widgetBaseId(kind)
        if (baseId in hidden) {
            if (!placeNear(baseId, page, 0, 0)) return null
            hidden.remove(baseId)
            return baseId
        }
        val id = HomeCatalog.registerWidgetCopy(kind, nextCopyNumber(kind)) ?: return null
        if (!placeNear(id, page, 0, 0)) {
            HomeCatalog.items.remove(id)
            return null
        }
        return id
    }

    private fun nextCopyNumber(kind: HomeWidgetKind): Int {
        val used = list.mapNotNull { HomeCatalog.widgetCopyOf(it.id) }
            .filter { it.first == kind }
            .map { it.second }
        var n = 1
        while (n in used) n++
        return n
    }

    fun renameFolder(number: Int, name: String) {
        if (find(folderIdOf(number)) == null) return
        val clean = name.replace(";", " ").replace(":", " ").replace("#", "").replace("@", "").trim()
        if (clean.isEmpty() || clean == FOLDER_DEFAULT_NAME) folderNames.remove(number) else folderNames[number] = clean
    }

    fun mergeIntoFolder(id: String, targetId: String): Boolean {
        if (id == targetId) return false
        val dragged = HomeCatalog.items[id] ?: return false
        if (dragged !is HomeAppItem || dragged.isFolder()) return false
        val current = find(id) ?: return false
        if (current.page == FOLDER_PAGE) return false
        val target = find(targetId) ?: return false
        if (target.page < 0) return false
        val targetItem = HomeCatalog.items[targetId] ?: return false
        if (targetItem !is HomeAppItem) return false
        if (targetItem.isFolder()) {
            val n = folderNumber(targetId) ?: return false
            val apps = folderApps(n)
            if (apps.size >= FOLDER_MAX) return false
            put(HomePlacement(id, FOLDER_PAGE, apps.size, n))
            normalizeDock()
            return true
        }
        val n = nextFolderNumber()
        HomeCatalog.registerFolder(n)
        put(HomePlacement(folderIdOf(n), target.page, target.column, target.row))
        put(HomePlacement(targetId, FOLDER_PAGE, 0, n))
        put(HomePlacement(id, FOLDER_PAGE, 1, n))
        normalizeDock()
        return true
    }

    fun removeFromFolder(id: String): Boolean {
        val current = find(id) ?: return false
        if (current.page != FOLDER_PAGE) return false
        val n = current.row
        val folder = find(folderIdOf(n)) ?: return false
        if (!placeNear(id, folder.page, folder.column, folder.row)) return false
        reindexFolder(n)
        if (folderApps(n).size <= 1) dissolveFolder(n)
        return true
    }

    fun reorderInFolder(id: String, index: Int): Boolean {
        val current = find(id) ?: return false
        if (current.page != FOLDER_PAGE) return false
        val n = current.row
        val order = folderApps(n).toMutableList()
        val from = order.indexOf(id)
        if (from < 0 || order.isEmpty()) return false
        val to = index.coerceIn(0, order.size - 1)
        if (from == to) return false
        order.removeAt(from)
        order.add(to, id)
        order.forEachIndexed { i, appId -> put(HomePlacement(appId, FOLDER_PAGE, i, n)) }
        return true
    }

    fun dissolveFolder(number: Int): Boolean {
        val folder = find(folderIdOf(number)) ?: return false
        val apps = folderApps(number)
        if (apps.size > 1) {
            var free = 0
            for (page in 0 until pageCount) {
                for (r in 0 until HOME_ROWS) {
                    for (c in 0 until HOME_COLUMNS) {
                        if (isFree(page, c, r, apps[0])) free++
                    }
                }
            }
            if (free < apps.size - 1) return false
        }
        list.removeAll { it.id == folder.id }
        HomeCatalog.items.remove(folder.id)
        folderNames.remove(number)
        apps.forEachIndexed { index, appId ->
            if (index == 0) {
                put(HomePlacement(appId, folder.page, folder.column, folder.row))
            } else {
                placeNear(appId, folder.page, folder.column, folder.row)
            }
        }
        return true
    }

    fun moveToGrid(id: String, page: Int, column: Int, row: Int): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        val current = find(id) ?: return false
        if (page !in 0 until pageCount) return false
        val c = column.coerceIn(0, HOME_COLUMNS - item.columnSpan)
        val r = row.coerceIn(0, HOME_ROWS - item.rowSpan)
        if (item is HomeAppItem) {
            val other = list.firstOrNull { p ->
                p.id != id && p.page == page && p.column == c && p.row == r &&
                    HomeCatalog.items[p.id] is HomeAppItem
            }
            if (other != null) {
                put(HomePlacement(other.id, current.page, current.column, current.row))
                put(HomePlacement(id, page, c, r))
                normalizeDock()
                return true
            }
        }
        if (isFree(page, c, r, id)) {
            put(HomePlacement(id, page, c, r))
            normalizeDock()
            return true
        }
        if (item is HomeWidgetItem && pushAside(id, current, page, c, r)) {
            normalizeDock()
            return true
        }
        val spot = nearestFree(page, c, r, id) ?: return false
        put(HomePlacement(id, page, spot.first, spot.second))
        normalizeDock()
        return true
    }

    fun moveToDock(id: String, index: Int): Boolean {
        val dragged = HomeCatalog.items[id]
        if (dragged !is HomeAppItem || dragged.isFolder()) return false
        val current = find(id) ?: return false
        val others = dock().filter { it.id != id }
        if (others.size >= DOCK_MAX) {
            if (current.page == DOCK_PAGE) return false
            val target = others[index.coerceIn(0, others.size - 1)]
            put(HomePlacement(target.id, current.page, current.column, current.row))
            put(HomePlacement(id, DOCK_PAGE, target.column, 0))
            normalizeDock()
            return true
        }
        val order = others.map { it.id }.toMutableList()
        order.add(index.coerceIn(0, order.size), id)
        order.forEachIndexed { i, itemId -> put(HomePlacement(itemId, DOCK_PAGE, i, 0)) }
        return true
    }

    private fun pushAside(id: String, current: HomePlacement, page: Int, c: Int, r: Int): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        val snapshot = list.toList()
        val hit = list.filter { p ->
            if (p.id == id || p.page != page) return@filter false
            val other = HomeCatalog.items[p.id] ?: return@filter false
            c < p.column + other.columnSpan && p.column < c + item.columnSpan &&
                r < p.row + other.rowSpan && p.row < r + item.rowSpan
        }.sortedByDescending { p -> HomeCatalog.items[p.id]?.let { it.columnSpan * it.rowSpan } ?: 0 }
        if (hit.isEmpty()) return false
        val hitIds = hit.map { it.id }.toSet()
        list.removeAll { it.id in hitIds }
        put(HomePlacement(id, page, c, r))
        for (d in hit) {
            val spot = if (current.page in 0 until pageCount) {
                nearestFree(current.page, current.column, current.row, d.id)
            } else {
                null
            }
            if (spot != null) {
                put(HomePlacement(d.id, current.page, spot.first, spot.second))
            } else if (!placeNear(d.id, page, d.column, d.row)) {
                list.clear()
                list.addAll(snapshot)
                return false
            }
        }
        return true
    }

    private fun put(p: HomePlacement) {
        val index = list.indexOfFirst { it.id == p.id }
        if (index >= 0) list[index] = p else list.add(p)
    }

    private fun normalizeDock() {
        dock().forEachIndexed { index, p ->
            if (p.column != index || p.row != 0) put(HomePlacement(p.id, DOCK_PAGE, index, 0))
        }
    }

    private fun nextFolderNumber(): Int =
        (list.mapNotNull { folderNumber(it.id) }.maxOrNull() ?: 0) + 1

    private fun reindexFolder(number: Int) {
        folderApps(number).forEachIndexed { index, appId -> put(HomePlacement(appId, FOLDER_PAGE, index, number)) }
    }

    private fun placeNear(id: String, page: Int, column: Int, row: Int): Boolean {
        val startPage = if (page in 0 until pageCount) page else 0
        val order = listOf(startPage) + (0 until pageCount).filter { it != startPage }
        for (p in order) {
            val spot = nearestFree(p, column, row, id) ?: continue
            put(HomePlacement(id, p, spot.first, spot.second))
            return true
        }
        return false
    }

    private fun isFree(page: Int, column: Int, row: Int, id: String): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        if (column < 0 || row < 0) return false
        if (column + item.columnSpan > HOME_COLUMNS || row + item.rowSpan > HOME_ROWS) return false
        return list.none { p ->
            if (p.id == id || p.page != page) return@none false
            val other = HomeCatalog.items[p.id] ?: return@none false
            column < p.column + other.columnSpan && p.column < column + item.columnSpan &&
                row < p.row + other.rowSpan && p.row < row + item.rowSpan
        }
    }

    private fun nearestFree(page: Int, column: Int, row: Int, id: String): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        for (r in 0 until HOME_ROWS) {
            for (c in 0 until HOME_COLUMNS) {
                if (!isFree(page, c, r, id)) continue
                val distance = abs(c - column) + abs(r - row)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = Pair(c, r)
                }
            }
        }
        return best
    }

    private fun tryAdd(p: HomePlacement): Boolean {
        val item = HomeCatalog.items[p.id] ?: return false
        if (find(p.id) != null) return false
        if (p.page == DOCK_PAGE) {
            if (item !is HomeAppItem || item.isFolder() || dock().size >= DOCK_MAX) return false
            list.add(HomePlacement(p.id, DOCK_PAGE, dock().size, 0))
            return true
        }
        if (p.page !in 0 until pageCount) return false
        if (!isFree(p.page, p.column, p.row, p.id)) return false
        list.add(p)
        return true
    }

    private fun tryAddToFolder(p: HomePlacement): Boolean {
        val item = HomeCatalog.items[p.id]
        if (item !is HomeAppItem || item.isFolder()) return false
        if (find(p.id) != null) return false
        val n = p.row
        if (find(folderIdOf(n)) == null) return false
        val size = folderApps(n).size
        if (size >= FOLDER_MAX) return false
        list.add(HomePlacement(p.id, FOLDER_PAGE, size, n))
        return true
    }

    private fun cleanupFolders() {
        list.mapNotNull { folderNumber(it.id) }.forEach { n ->
            if (folderApps(n).size <= 1) dissolveFolder(n)
        }
        folderNames.keys.filter { find(folderIdOf(it)) == null }.forEach { folderNames.remove(it) }
        HomeCatalog.items.keys
            .filter { it.startsWith(FOLDER_ID_PREFIX) && find(it) == null }
            .forEach { HomeCatalog.items.remove(it) }
    }

    private fun addAnywhere(id: String): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        for (page in 0 until pageCount) {
            val spot = nearestFree(page, 0, 0, id) ?: continue
            list.add(HomePlacement(id, page, spot.first, spot.second))
            return true
        }
        if (item is HomeAppItem && !item.isFolder() && dock().size < DOCK_MAX) {
            list.add(HomePlacement(id, DOCK_PAGE, dock().size, 0))
            return true
        }
        return false
    }

    companion object {
        fun load(raw: String): HomeLayoutState {
            HomeCatalog.clearFolders()
            HomeCatalog.clearWidgetCopies()
            val state = HomeLayoutState()
            val tokens = raw.split(";")
            var savedCount = HOME_PAGE_COUNT
            for (t in tokens) {
                if (t.startsWith("#")) {
                    val eq = t.indexOf('=')
                    val n = if (eq > 1) t.substring(1, eq).toIntOrNull() else null
                    if (n != null) state.folderNames[n] = t.substring(eq + 1)
                } else if (t.startsWith("@")) {
                    val n = t.substring(1).toIntOrNull()
                    if (n != null) savedCount = n
                } else if (t.startsWith("!")) {
                    val w = t.substring(1)
                    if (HomeCatalog.items[w] is HomeWidgetItem && w !in state.hidden) state.hidden.add(w)
                }
            }
            val saved = tokens.mapNotNull { parse(it) }
            val lastPage = saved.filter { it.page >= 0 }.maxOfOrNull { it.page } ?: 0
            state.pageCount = maxOf(savedCount, lastPage + 1).coerceIn(1, HOME_PAGE_MAX)
            saved.forEach { p -> folderNumber(p.id)?.let { HomeCatalog.registerFolder(it) } }
            saved.forEach { p ->
                HomeCatalog.widgetCopyOf(p.id)?.let { HomeCatalog.registerWidgetCopy(it.first, it.second) }
            }
            saved.filter { it.page != DOCK_PAGE && it.page != FOLDER_PAGE }.forEach { state.tryAdd(it) }
            saved.filter { it.page == DOCK_PAGE }.sortedBy { it.column }.forEach { state.tryAdd(it) }
            saved.filter { it.page == FOLDER_PAGE }.sortedBy { it.column }.forEach { state.tryAddToFolder(it) }
            state.cleanupFolders()
            HomeCatalog.items.keys
                .filter { HomeCatalog.widgetCopyOf(it) != null && state.find(it) == null }
                .forEach { HomeCatalog.items.remove(it) }
            state.hidden.removeAll { state.find(it) != null }
            HomeCatalog.defaults.forEach { d ->
                if (state.find(d.id) == null && d.id !in state.hidden) {
                    if (!state.tryAdd(d)) state.addAnywhere(d.id)
                }
            }
            if (HomeCatalog.baseIds.any { state.find(it) == null && it !in state.hidden }) return defaultState()
            state.normalizeDock()
            return state
        }

        private fun defaultState(): HomeLayoutState {
            HomeCatalog.clearFolders()
            HomeCatalog.clearWidgetCopies()
            val state = HomeLayoutState()
            HomeCatalog.defaults.forEach { state.list.add(it) }
            return state
        }

        private fun parse(token: String): HomePlacement? {
            if (token.startsWith("#") || token.startsWith("@") || token.startsWith("!")) return null
            val parts = token.split(":")
            if (parts.size != 4) return null
            val page = parts[1].toIntOrNull() ?: return null
            val column = parts[2].toIntOrNull() ?: return null
            val row = parts[3].toIntOrNull() ?: return null
            return HomePlacement(parts[0], page, column, row)
        }
    }
}
