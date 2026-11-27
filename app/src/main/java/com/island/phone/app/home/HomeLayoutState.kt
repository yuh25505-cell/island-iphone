package com.island.phone.app.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import kotlin.math.abs

internal const val DOCK_PAGE = -1
internal const val HOME_PAGE_COUNT = 2
internal const val DOCK_MAX = 4

internal val HomeItem.id: String
    get() = when (this) {
        is HomeAppItem -> "app_$iconName"
        is HomeWidgetItem -> "widget_${kind.name}"
    }

@Immutable
internal class HomePlacement(val id: String, val page: Int, val column: Int, val row: Int)

internal object HomeCatalog {
    val items: Map<String, HomeItem> = buildMap {
        (HomeFirstPageLayout.items + HomeSecondPageLayout.items + HomeFirstPageLayout.dock).forEach { put(it.id, it) }
    }

    val defaults: List<HomePlacement> = buildList {
        HomeFirstPageLayout.items.forEach { add(HomePlacement(it.id, 0, it.column, it.row)) }
        HomeSecondPageLayout.items.forEach { add(HomePlacement(it.id, 1, it.column, it.row)) }
        HomeFirstPageLayout.dock.forEachIndexed { index, item -> add(HomePlacement(item.id, DOCK_PAGE, index, 0)) }
    }
}

@Stable
internal class HomeLayoutState private constructor() {
    private val list = mutableStateListOf<HomePlacement>()

    fun onPage(page: Int): List<HomePlacement> = list.filter { it.page == page }

    fun dock(): List<HomePlacement> = list.filter { it.page == DOCK_PAGE }.sortedBy { it.column }

    fun find(id: String): HomePlacement? = list.firstOrNull { it.id == id }

    fun encode(): String = list.joinToString(";") { "${it.id}:${it.page}:${it.column}:${it.row}" }

    fun moveToGrid(id: String, page: Int, column: Int, row: Int): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        val current = find(id) ?: return false
        if (page !in 0 until HOME_PAGE_COUNT) return false
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
        val spot = nearestFree(page, c, r, id) ?: return false
        put(HomePlacement(id, page, spot.first, spot.second))
        normalizeDock()
        return true
    }

    fun moveToDock(id: String, index: Int): Boolean {
        if (HomeCatalog.items[id] !is HomeAppItem) return false
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

    private fun put(p: HomePlacement) {
        val index = list.indexOfFirst { it.id == p.id }
        if (index >= 0) list[index] = p else list.add(p)
    }

    private fun normalizeDock() {
        dock().forEachIndexed { index, p ->
            if (p.column != index || p.row != 0) put(HomePlacement(p.id, DOCK_PAGE, index, 0))
        }
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
            if (item !is HomeAppItem || dock().size >= DOCK_MAX) return false
            list.add(HomePlacement(p.id, DOCK_PAGE, dock().size, 0))
            return true
        }
        if (p.page !in 0 until HOME_PAGE_COUNT) return false
        if (!isFree(p.page, p.column, p.row, p.id)) return false
        list.add(p)
        return true
    }

    private fun addAnywhere(id: String): Boolean {
        val item = HomeCatalog.items[id] ?: return false
        for (page in 0 until HOME_PAGE_COUNT) {
            val spot = nearestFree(page, 0, 0, id) ?: continue
            list.add(HomePlacement(id, page, spot.first, spot.second))
            return true
        }
        if (item is HomeAppItem && dock().size < DOCK_MAX) {
            list.add(HomePlacement(id, DOCK_PAGE, dock().size, 0))
            return true
        }
        return false
    }

    companion object {
        fun load(raw: String): HomeLayoutState {
            val state = HomeLayoutState()
            val saved = raw.split(";").mapNotNull { parse(it) }
            saved.filter { it.page != DOCK_PAGE }.forEach { state.tryAdd(it) }
            saved.filter { it.page == DOCK_PAGE }.sortedBy { it.column }.forEach { state.tryAdd(it) }
            HomeCatalog.defaults.forEach { d ->
                if (state.find(d.id) == null) {
                    if (!state.tryAdd(d)) state.addAnywhere(d.id)
                }
            }
            if (HomeCatalog.items.keys.any { state.find(it) == null }) return defaultState()
            state.normalizeDock()
            return state
        }

        private fun defaultState(): HomeLayoutState {
            val state = HomeLayoutState()
            HomeCatalog.defaults.forEach { state.list.add(it) }
            return state
        }

        private fun parse(token: String): HomePlacement? {
            val parts = token.split(":")
            if (parts.size != 4) return null
            val page = parts[1].toIntOrNull() ?: return null
            val column = parts[2].toIntOrNull() ?: return null
            val row = parts[3].toIntOrNull() ?: return null
            return HomePlacement(parts[0], page, column, row)
        }
    }
}
