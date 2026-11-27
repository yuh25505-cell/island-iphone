package com.island.phone.app.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

@Immutable
internal class HomeRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float): Boolean =
        px >= x && px <= x + w && py >= y && py <= y + h
}

@Immutable
internal class HomeHit(
    val id: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val fromDock: Boolean,
)

@Immutable
internal class HomeDrop(
    val page: Int,
    val column: Int,
    val row: Int,
    val dockIndex: Int,
    val mergeId: String? = null,
)

internal fun homeItemRect(item: HomeItem, column: Int, row: Int, m: HomeGridMetrics): HomeRect {
    val top = (m.edge + m.cellH * row).value
    if (item is HomeWidgetItem) {
        val left = (m.cellW * column + HomeWidgetInset).value
        val maxW = m.cellW * item.columnSpan - HomeWidgetInset * 2
        if (item.square) {
            val side = minOf(maxW, m.cellH * item.rowSpan - HomeLabelArea - HomeWidgetGap)
            return HomeRect(left, top, side.value, side.value)
        }
        val h = m.cellH * item.rowSpan - HomeWidgetGap
        return HomeRect(left, top, maxW.value, h.value)
    }
    return HomeRect((m.cellW * column).value, top, m.cellW.value, (m.iconSize + HomeLabelArea).value)
}

@Stable
internal class HomeGeometry {
    var metrics by mutableStateOf<HomeGridMetrics?>(null)
    var density: Float = 1f
    var dockIcon: Float = 0f
    var rootX: Float = 0f
    var rootY: Float = 0f
    var rootW: Float = 0f
    var pagerX: Float = 0f
    var pagerY: Float = 0f
    var dockX: Float = 0f
    var dockY: Float = 0f
    var dockW: Float = 0f
    var dockH: Float = 0f

    private fun dp(px: Float): Float = px / density

    private fun inDock(yDp: Float): Boolean = dockW > 0f && yDp >= dp(dockY) - 24f

    fun hit(layout: HomeLayoutState, page: Int, pxX: Float, pxY: Float): HomeHit? {
        val m = metrics ?: return null
        val x = dp(pxX + rootX)
        val y = dp(pxY + rootY)
        if (inDock(y)) {
            val items = layout.dock()
            if (items.isEmpty()) return null
            val slot = dp(dockW) / items.size
            val icon = dockIcon
            for (i in items.indices) {
                val left = dp(dockX) + slot * (i + 0.5f) - icon / 2f
                val top = dp(dockY) + (dp(dockH) - icon) / 2f
                if (x >= left && x <= left + icon && y >= top && y <= top + icon) {
                    return HomeHit(items[i].id, left, top, icon, icon, true)
                }
            }
            return null
        }
        val originX = dp(pagerX) + 16f
        val originY = dp(pagerY)
        for (p in layout.onPage(page)) {
            val item = HomeCatalog.items[p.id] ?: continue
            val r = homeItemRect(item, p.column, p.row, m)
            if (r.contains(x - originX, y - originY)) {
                return HomeHit(p.id, originX + r.x, originY + r.y, r.w, r.h, false)
            }
        }
        return null
    }

    fun drop(
        layout: HomeLayoutState,
        id: String,
        page: Int,
        pxX: Float,
        pxY: Float,
        grabX: Float,
        grabY: Float,
    ): HomeDrop? {
        val m = metrics ?: return null
        val item = HomeCatalog.items[id] ?: return null
        val x = dp(pxX + rootX)
        val y = dp(pxY + rootY)
        val movable = item is HomeAppItem && !item.isFolder()
        if (movable && inDock(y)) {
            val others = layout.dock().count { it.id != id }
            val slot = dp(dockW) / (others + 1)
            val index = floor((x - dp(dockX)) / slot).toInt().coerceIn(0, others)
            return HomeDrop(DOCK_PAGE, 0, 0, index)
        }
        val originX = dp(pagerX) + 16f
        val originY = dp(pagerY)
        if (movable) {
            val centerX = x - grabX - originX + m.iconSize.value / 2f
            val centerY = y - grabY - originY + m.iconSize.value / 2f
            val reach = m.iconSize.value * 0.3f
            for (p in layout.onPage(page)) {
                if (p.id == id) continue
                val other = HomeCatalog.items[p.id] ?: continue
                if (other !is HomeAppItem) continue
                val r = homeItemRect(other, p.column, p.row, m)
                val cx = r.x + r.w / 2f
                val cy = r.y + m.iconSize.value / 2f
                if (abs(centerX - cx) <= reach && abs(centerY - cy) <= reach) {
                    return HomeDrop(page, p.column, p.row, -1, p.id)
                }
            }
        }
        val inset = if (item is HomeWidgetItem) HomeWidgetInset.value else 0f
        val column = ((x - grabX - originX - inset) / m.cellW.value).roundToInt()
        val row = ((y - grabY - originY - m.edge.value) / m.cellH.value).roundToInt()
        return HomeDrop(page, column, row, -1)
    }
}
