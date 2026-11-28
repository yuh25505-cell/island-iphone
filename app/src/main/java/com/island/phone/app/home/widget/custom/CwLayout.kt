package com.island.phone.app.home.widget.custom

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

// 每个元素告诉它的“父盒子”自己想要多大、怎么摆
internal class Item(
    val w: Len? = null,
    val h: Len? = null,
    val minW: Len? = null,
    val maxW: Len? = null,
    val minH: Len? = null,
    val maxH: Len? = null,
    val mL: Len? = Len.Zero,
    val mR: Len? = Len.Zero,
    val mT: Len? = Len.Zero,
    val mB: Len? = Len.Zero,
    val extraW: Float = 0f,
    val extraH: Float = 0f,
    val contentBox: Boolean = false,
    val grow: Float = 0f,
    val shrink: Float = 1f,
    val basis: Len? = null,
    val alignSelf: String = "auto",
    val position: String = "static",
    val top: Len? = null,
    val right: Len? = null,
    val bottom: Len? = null,
    val left: Len? = null,
    val order: Int = 0,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val colStart: Int = 0,
    val rowStart: Int = 0,
    val aspect: Float = 0f,
    val fill: Boolean = true,
)

internal class BoxSpec(
    val mode: Int,
    val padL: Float,
    val padT: Float,
    val padR: Float,
    val padB: Float,
    val flexRow: Boolean = true,
    val reverse: Boolean = false,
    val wrap: Boolean = false,
    val justify: String = "flex-start",
    val alignItems: String = "stretch",
    val alignContent: String = "stretch",
    val rowGap: Len? = null,
    val colGap: Len? = null,
    val gridCols: String? = null,
    val gridRows: String? = null,
    val gridAutoRows: String? = null,
    val textAlign: String = "left",
    val ctx: LenCtx = LenCtx(16f, 300f, 300f),
)

private class ItemData(val item: Item) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any? = item
}

internal fun Modifier.cwItem(item: Item): Modifier = this.then(ItemData(item))

private val DefaultItem = Item()
private const val INF = Float.POSITIVE_INFINITY

private fun itemOf(m: Measurable): Item = (m.parentData as? Item) ?: DefaultItem

private fun toI(v: Float): Int = if (v.isNaN()) 0 else Math.round(v.coerceIn(0f, 200000f))

private fun toMaxI(v: Float): Int = if (v.isNaN() || v.isInfinite() || v >= 200000f) Constraints.Infinity else toI(v)

private fun mk(minW: Float, maxW: Float, minH: Float, maxH: Float): Constraints {
    val mxW = toMaxI(maxW)
    val mxH = toMaxI(maxH)
    val mnW = if (mxW == Constraints.Infinity) toI(minW) else minOf(toI(minW), mxW)
    val mnH = if (mxH == Constraints.Infinity) toI(minH) else minOf(toI(minH), mxH)
    return Constraints(mnW, mxW, mnH, mxH)
}

private fun lenPx(l: Len?, basis: Float, d: Float): Float? {
    if (l == null) return null
    if (l.pct != 0f && (basis.isInfinite() || basis.isNaN())) return null
    return l.px * d + (if (l.pct != 0f) l.pct * basis / 100f else 0f)
}

private fun sizeSpec(l: Len?, extraDp: Float, contentBox: Boolean, basis: Float, d: Float): Float? {
    val v = lenPx(l, basis, d) ?: return null
    return if (contentBox) v + extraDp * d else v
}

private fun clampF(v: Float, mn: Float?, mx: Float?): Float {
    var r = v
    if (mx != null && r > mx) r = mx
    if (mn != null && r < mn) r = mn
    return r
}

private class Pl(val p: Placeable, val x: Int, val y: Int)

private fun isAbs(it: Item): Boolean = it.position == "absolute" || it.position == "fixed"

private fun relOffX(it: Item, d: Float, basis: Float): Float {
    if (it.position != "relative") return 0f
    val l = lenPx(it.left, basis, d)
    if (l != null) return l
    val r = lenPx(it.right, basis, d)
    return if (r != null) -r else 0f
}

private fun relOffY(it: Item, d: Float, basis: Float): Float {
    if (it.position != "relative") return 0f
    val t = lenPx(it.top, basis, d)
    if (t != null) return t
    val b = lenPx(it.bottom, basis, d)
    return if (b != null) -b else 0f
}

// 绝对定位的子元素：按父盒子的内边距框来摆
private fun MeasureScope.placeAbsolute(
    list: List<Pair<Measurable, Item>>,
    boxW: Int,
    boxH: Int,
    startX: Float,
    startY: Float,
    out: MutableList<Pl>,
) {
    val d = density
    val bw = boxW.toFloat()
    val bh = boxH.toFloat()
    for ((m, it) in list) {
        val left = lenPx(it.left, bw, d)
        val right = lenPx(it.right, bw, d)
        val top = lenPx(it.top, bh, d)
        val bottom = lenPx(it.bottom, bh, d)
        val ml = lenPx(it.mL, bw, d) ?: 0f
        val mr = lenPx(it.mR, bw, d) ?: 0f
        val mt = lenPx(it.mT, bw, d) ?: 0f
        val mb = lenPx(it.mB, bw, d) ?: 0f
        var w = sizeSpec(it.w, it.extraW, it.contentBox, bw, d)
        var h = sizeSpec(it.h, it.extraH, it.contentBox, bh, d)
        if (w == null && left != null && right != null) w = bw - left - right - ml - mr
        if (h == null && top != null && bottom != null) h = bh - top - bottom - mt - mb
        if (it.aspect > 0f) {
            if (w != null && h == null) h = w / it.aspect
            if (h != null && w == null) w = h * it.aspect
        }
        val minW = sizeSpec(it.minW, it.extraW, it.contentBox, bw, d)
        val maxW = sizeSpec(it.maxW, it.extraW, it.contentBox, bw, d)
        val minH = sizeSpec(it.minH, it.extraH, it.contentBox, bh, d)
        val maxH = sizeSpec(it.maxH, it.extraH, it.contentBox, bh, d)
        if (w != null) w = clampF(w, minW, maxW)
        if (h != null) h = clampF(h, minH, maxH)
        val availW = maxOf(0f, bw - (left ?: 0f) - (right ?: 0f) - ml - mr)
        val availH = maxOf(0f, bh - (top ?: 0f) - (bottom ?: 0f) - mt - mb)
        val c = mk(w ?: (minW ?: 0f), w ?: (maxW?.let { minOf(it, availW) } ?: availW), h ?: (minH ?: 0f), h ?: (maxH?.let { minOf(it, availH) } ?: INF))
        val p = m.measure(c)
        val x = when {
            left != null -> left + ml
            right != null -> bw - p.width - right - mr
            else -> startX + ml
        }
        val y = when {
            top != null -> top + mt
            bottom != null -> bh - p.height - bottom - mb
            else -> startY + mt
        }
        out.add(Pl(p, Math.round(x), Math.round(y)))
    }
}

internal fun MeasureScope.cwMeasure(spec: BoxSpec, ms: List<Measurable>, c: Constraints): MeasureResult =
    when (spec.mode) {
        1 -> flexMeasure(spec, ms, c)
        2 -> gridMeasure(spec, ms, c)
        3 -> flowMeasure(spec, ms, c)
        else -> blockMeasure(spec, ms, c)
    }

// ---------- 普通块排版 ----------

private fun MeasureScope.blockMeasure(spec: BoxSpec, ms: List<Measurable>, c: Constraints): MeasureResult {
    val d = density
    val padL = spec.padL * d
    val padT = spec.padT * d
    val padR = spec.padR * d
    val padB = spec.padB * d
    val tightW = c.hasBoundedWidth && c.minWidth == c.maxWidth
    val definiteH = c.hasBoundedHeight && c.minHeight == c.maxHeight
    val innerMaxW = if (c.hasBoundedWidth) maxOf(0f, c.maxWidth - padL - padR) else INF
    val innerH = if (definiteH) maxOf(0f, c.maxHeight - padT - padB) else INF
    val pls = ArrayList<Pl>()
    val absList = ArrayList<Pair<Measurable, Item>>()
    var y = padT
    var maxRight = 0f
    var prevMb = 0f
    var first = true
    for (m in ms) {
        val it = itemOf(m)
        if (isAbs(it)) {
            absList.add(Pair(m, it))
            continue
        }
        val mlRaw = lenPx(it.mL, innerMaxW, d)
        val mrRaw = lenPx(it.mR, innerMaxW, d)
        val mt = lenPx(it.mT, innerMaxW, d) ?: 0f
        val mb = lenPx(it.mB, innerMaxW, d) ?: 0f
        val fixedM = (mlRaw ?: 0f) + (mrRaw ?: 0f)
        val avail = if (innerMaxW.isInfinite()) INF else maxOf(0f, innerMaxW - fixedM)
        var w = sizeSpec(it.w, it.extraW, it.contentBox, innerMaxW, d)
        var h = sizeSpec(it.h, it.extraH, it.contentBox, innerH, d)
        if (it.aspect > 0f) {
            if (w != null && h == null) h = w / it.aspect
            if (h != null && w == null) w = h * it.aspect
        }
        val minW = sizeSpec(it.minW, it.extraW, it.contentBox, innerMaxW, d)
        val maxW = sizeSpec(it.maxW, it.extraW, it.contentBox, innerMaxW, d)
        val minH = sizeSpec(it.minH, it.extraH, it.contentBox, innerH, d)
        val maxH = sizeSpec(it.maxH, it.extraH, it.contentBox, innerH, d)
        val stretch = w == null && it.fill && tightW
        if (stretch) w = avail
        if (w != null) w = clampF(w, minW, maxW)
        if (h != null) h = clampF(h, minH, maxH)
        val cw = if (w != null) mk(w, w, h ?: (minH ?: 0f), h ?: (maxH ?: INF)) else
            mk(minW ?: 0f, minOf(avail, maxW ?: INF), h ?: (minH ?: 0f), h ?: (maxH ?: INF))
        val p = m.measure(cw)
        var ml = mlRaw ?: 0f
        if (!innerMaxW.isInfinite()) {
            val free = innerMaxW - p.width - fixedM
            if (mlRaw == null && mrRaw == null) ml = maxOf(0f, free / 2f)
            else if (mlRaw == null) ml = maxOf(0f, free)
        }
        val gap = if (first) mt else maxOf(prevMb, mt)
        y += gap
        val x = padL + ml + relOffX(it, d, innerMaxW)
        pls.add(Pl(p, Math.round(x), Math.round(y + relOffY(it, d, innerH))))
        y += p.height
        prevMb = mb
        first = false
        maxRight = maxOf(maxRight, ml + p.width + (mrRaw ?: 0f))
    }
    val tail = if (padB > 0f) prevMb else 0f
    val width = if (tightW) c.maxWidth else c.constrainWidth(toI(maxRight + padL + padR))
    val height = if (definiteH) c.maxHeight else c.constrainHeight(toI(y + tail + padB))
    placeAbsolute(absList, width, height, padL, padT, pls)
    return layout(width, height) {
        for (pl in pls) pl.p.placeRelative(pl.x, pl.y)
    }
}

// ---------- flex 排版 ----------

private class FI(val m: Measurable, val it: Item) {
    var ml = 0f
    var mr = 0f
    var mt = 0f
    var mb = 0f
    var autoM0 = false
    var autoM1 = false
    var autoC0 = false
    var autoC1 = false
    var nat: Placeable? = null
    var natCrossTight = false
    var base = 0f
    var hyp = 0f
    var main = 0f
    var minMain: Float? = null
    var maxMain: Float? = null
    var crossSpec: Float? = null
    var p: Placeable? = null
    var stretch = false
}

private fun isStretch(a: String): Boolean = a == "stretch" || a == "normal" || a == "auto" || a.isEmpty()

private fun MeasureScope.flexMeasure(spec: BoxSpec, ms: List<Measurable>, c: Constraints): MeasureResult {
    val d = density
    val row = spec.flexRow
    val padL = spec.padL * d
    val padT = spec.padT * d
    val padR = spec.padR * d
    val padB = spec.padB * d
    val tightW = c.hasBoundedWidth && c.minWidth == c.maxWidth
    val definiteH = c.hasBoundedHeight && c.minHeight == c.maxHeight
    val innerW = if (c.hasBoundedWidth) maxOf(0f, c.maxWidth - padL - padR) else INF
    val innerH = if (c.hasBoundedHeight) maxOf(0f, c.maxHeight - padT - padB) else INF
    val mainTight = if (row) tightW else definiteH
    val crossTight = if (row) definiteH else tightW
    val mainAvail = if (row) innerW else (if (definiteH) innerH else INF)
    val crossAvail = if (row) (if (definiteH) innerH else INF) else innerW
    val mainGap = lenPx(if (row) spec.colGap else spec.rowGap, if (mainAvail.isInfinite()) 0f else mainAvail, d) ?: 0f
    val crossGap = lenPx(if (row) spec.rowGap else spec.colGap, if (crossAvail.isInfinite()) 0f else crossAvail, d) ?: 0f
    val absList = ArrayList<Pair<Measurable, Item>>()
    val items = ArrayList<FI>()
    for (m in ms) {
        val it = itemOf(m)
        if (isAbs(it)) absList.add(Pair(m, it)) else items.add(FI(m, it))
    }
    val sorted = items.sortedBy { it.it.order }
    val pctBasis = if (innerW.isInfinite()) INF else innerW

    for (fi in sorted) {
        val it = fi.it
        val l = lenPx(it.mL, pctBasis, d)
        val r = lenPx(it.mR, pctBasis, d)
        val t = lenPx(it.mT, pctBasis, d)
        val b = lenPx(it.mB, pctBasis, d)
        fi.ml = l ?: 0f
        fi.mr = r ?: 0f
        fi.mt = t ?: 0f
        fi.mb = b ?: 0f
        if (row) {
            fi.autoM0 = l == null
            fi.autoM1 = r == null
            fi.autoC0 = t == null
            fi.autoC1 = b == null
        } else {
            fi.autoM0 = t == null
            fi.autoM1 = b == null
            fi.autoC0 = l == null
            fi.autoC1 = r == null
        }
        val extraMain = if (row) it.extraW else it.extraH
        val extraCross = if (row) it.extraH else it.extraW
        val mainSpecV = if (row) sizeSpec(it.w, it.extraW, it.contentBox, innerW, d) else sizeSpec(it.h, it.extraH, it.contentBox, if (definiteH) innerH else INF, d)
        fi.crossSpec = if (row) sizeSpec(it.h, it.extraH, it.contentBox, if (definiteH) innerH else INF, d) else sizeSpec(it.w, it.extraW, it.contentBox, innerW, d)
        var cs = fi.crossSpec
        var ms2 = mainSpecV
        if (it.aspect > 0f) {
            if (row) {
                if (ms2 != null && cs == null) cs = ms2 / it.aspect
                if (cs != null && ms2 == null) ms2 = cs * it.aspect
            } else {
                if (ms2 != null && cs == null) cs = ms2 * it.aspect
                if (cs != null && ms2 == null) ms2 = cs / it.aspect
            }
            fi.crossSpec = cs
        }
        fi.minMain = if (row) sizeSpec(it.minW, it.extraW, it.contentBox, innerW, d) else sizeSpec(it.minH, it.extraH, it.contentBox, if (definiteH) innerH else INF, d)
        fi.maxMain = if (row) sizeSpec(it.maxW, it.extraW, it.contentBox, innerW, d) else sizeSpec(it.maxH, it.extraH, it.contentBox, if (definiteH) innerH else INF, d)
        val basisPx = lenPx(it.basis, mainAvail, d)?.let { v -> if (it.contentBox) v + extraMain * d else v }
        val al = if (it.alignSelf == "auto") spec.alignItems else it.alignSelf
        fi.stretch = isStretch(al) && cs == null && !fi.autoC0 && !fi.autoC1
        val m0 = if (row) fi.ml + fi.mr else fi.mt + fi.mb
        val c0 = if (row) fi.mt + fi.mb else fi.ml + fi.mr
        val fixedMain = basisPx ?: ms2
        val mainMaxItem = if (mainAvail.isInfinite()) INF else maxOf(0f, mainAvail - m0)
        val crossMaxItem = if (crossAvail.isInfinite()) INF else maxOf(0f, crossAvail - c0)
        val mainMin = if (fixedMain != null) fixedMain else 0f
        val mainMax = if (fixedMain != null) fixedMain else mainMaxItem
        val crossTightHere = fi.stretch && crossTight && !crossMaxItem.isInfinite()
        val crossMin = if (cs != null) cs else if (crossTightHere) crossMaxItem else 0f
        val crossMax = if (cs != null) cs else crossMaxItem
        val cons = if (row) mk(mainMin, mainMax, crossMin, crossMax) else mk(crossMin, crossMax, mainMin, mainMax)
        val nat = fi.m.measure(cons)
        fi.nat = nat
        fi.natCrossTight = crossTightHere || cs != null
        val natMain = (if (row) nat.width else nat.height).toFloat()
        fi.base = fixedMain ?: natMain
        fi.hyp = clampF(fi.base, fi.minMain, fi.maxMain)
        fi.main = fi.hyp
        if (extraCross < 0f) fi.crossSpec = fi.crossSpec
    }

    val containerMainInner = if (mainTight) (if (row) maxOf(0f, c.maxWidth - padL - padR) else maxOf(0f, c.maxHeight - padT - padB)) else INF
    val lines = ArrayList<ArrayList<FI>>()
    if (!spec.wrap || mainAvail.isInfinite()) {
        lines.add(ArrayList(sorted))
    } else {
        var cur = ArrayList<FI>()
        var sum = 0f
        for (fi in sorted) {
            val outer = fi.hyp + (if (row) fi.ml + fi.mr else fi.mt + fi.mb)
            if (cur.isNotEmpty() && sum + mainGap + outer > mainAvail + 0.5f) {
                lines.add(cur)
                cur = ArrayList()
                sum = 0f
            }
            sum += (if (cur.isEmpty()) 0f else mainGap) + outer
            cur.add(fi)
        }
        if (cur.isNotEmpty()) lines.add(cur)
    }
    if (lines.isEmpty()) lines.add(ArrayList())

    val lineCross = FloatArray(lines.size)
    val lineMain = FloatArray(lines.size)
    for ((li, line) in lines.withIndex()) {
        val n = line.size
        val gaps = mainGap * maxOf(0, n - 1)
        var sumOuter = gaps
        for (fi in line) sumOuter += fi.hyp + (if (row) fi.ml + fi.mr else fi.mt + fi.mb)
        val avail = if (mainTight) containerMainInner else mainAvail
        val free = if (avail.isInfinite()) 0f else avail - sumOuter
        for (fi in line) fi.main = fi.hyp
        if (free > 0.5f && mainTight) {
            val totalGrow = line.sumOf { it.it.grow.toDouble() }.toFloat()
            if (totalGrow > 0f) {
                val share = if (totalGrow < 1f) free * totalGrow else free
                for (fi in line) fi.main = clampF(fi.hyp + share * fi.it.grow / totalGrow, fi.minMain, fi.maxMain)
            }
        } else if (free < -0.5f && !avail.isInfinite()) {
            val scaled = line.sumOf { (it.it.shrink * it.hyp).toDouble() }.toFloat()
            if (scaled > 0f) {
                for (fi in line) {
                    fi.main = clampF(fi.hyp + free * (fi.it.shrink * fi.hyp) / scaled, fi.minMain ?: 0f, fi.maxMain)
                }
            }
        }
        var crossMaxLine = 0f
        for (fi in line) {
            val m0 = if (row) fi.ml + fi.mr else fi.mt + fi.mb
            val c0 = if (row) fi.mt + fi.mb else fi.ml + fi.mr
            val nat = fi.nat
            val natMain = if (nat == null) -1f else (if (row) nat.width else nat.height).toFloat()
            val crossMaxItem = if (crossAvail.isInfinite()) INF else maxOf(0f, crossAvail - c0)
            val p: Placeable
            if (nat != null && Math.abs(natMain - fi.main) < 0.5f) {
                p = nat
            } else {
                val cs = fi.crossSpec
                val cMin = if (cs != null) cs else 0f
                val cMax = if (cs != null) cs else crossMaxItem
                val cons = if (row) mk(fi.main, fi.main, cMin, cMax) else mk(cMin, cMax, fi.main, fi.main)
                p = fi.m.measure(cons)
            }
            fi.p = p
            val crossSize = (if (row) p.height else p.width).toFloat()
            crossMaxLine = maxOf(crossMaxLine, crossSize + c0)
        }
        lineCross[li] = crossMaxLine
        lineMain[li] = sumOuter.let { s ->
            var t = gaps
            for (fi in line) t += (fi.p?.let { if (row) it.width else it.height } ?: 0).toFloat() + (if (row) fi.ml + fi.mr else fi.mt + fi.mb)
            t
        }
    }

    val crossContainerInner = if (crossTight) (if (row) maxOf(0f, c.maxHeight - padT - padB) else maxOf(0f, c.maxWidth - padL - padR)) else INF
    if (lines.size == 1 && crossTight) lineCross[0] = crossContainerInner
    var totalCross = crossGap * (lines.size - 1)
    for (v in lineCross) totalCross += v
    var extraBetween = 0f
    var crossStart = 0f
    if (crossTight && lines.size > 1) {
        val free = crossContainerInner - totalCross
        val ac = spec.alignContent
        if (free > 0f) {
            when (ac) {
                "flex-end", "end" -> crossStart = free
                "center" -> crossStart = free / 2f
                "space-between" -> extraBetween = free / (lines.size - 1)
                "space-around" -> {
                    extraBetween = free / lines.size
                    crossStart = extraBetween / 2f
                }
                "flex-start", "start" -> {}
                else -> {
                    val each = free / lines.size
                    for (i in lineCross.indices) lineCross[i] += each
                }
            }
        }
    }
    // 拉伸：重新按行高量一次
    for ((li, line) in lines.withIndex()) {
        for (fi in line) {
            val p = fi.p ?: continue
            val c0 = if (row) fi.mt + fi.mb else fi.ml + fi.mr
            val target = maxOf(0f, lineCross[li] - c0)
            val cur = (if (row) p.height else p.width).toFloat()
            if (fi.stretch && Math.abs(target - cur) > 0.5f) {
                val cons = if (row) mk(fi.main, fi.main, target, target) else mk(target, target, fi.main, fi.main)
                fi.p = fi.m.measure(cons)
            }
        }
    }

    var maxMainLine = 0f
    for (v in lineMain) maxMainLine = maxOf(maxMainLine, v)
    var sumCrossFinal = crossGap * (lines.size - 1)
    for (v in lineCross) sumCrossFinal += v
    val width: Int
    val height: Int
    if (row) {
        width = if (tightW) c.maxWidth else c.constrainWidth(toI(maxMainLine + padL + padR))
        height = if (definiteH) c.maxHeight else c.constrainHeight(toI(sumCrossFinal + padT + padB))
    } else {
        width = if (tightW) c.maxWidth else c.constrainWidth(toI(sumCrossFinal + padL + padR))
        height = if (definiteH) c.maxHeight else c.constrainHeight(toI(maxMainLine + padT + padB))
    }
    val innerMain = if (row) maxOf(0f, width - padL - padR) else maxOf(0f, height - padT - padB)

    val pls = ArrayList<Pl>()
    var crossPos = crossStart
    for ((li, line) in lines.withIndex()) {
        val n = line.size
        var free = innerMain - lineMain[li]
        var autoCount = 0
        for (fi in line) {
            if (fi.autoM0) autoCount++
            if (fi.autoM1) autoCount++
        }
        var startOff = 0f
        var gap = mainGap
        var autoShare = 0f
        if (autoCount > 0 && free > 0f) {
            autoShare = free / autoCount
            free = 0f
        } else if (free > 0f || spec.justify == "center") {
            when (spec.justify) {
                "flex-end", "end", "right" -> startOff = free
                "center" -> startOff = free / 2f
                "space-between" -> if (n > 1) gap += free / (n - 1)
                "space-around" -> {
                    gap += free / n
                    startOff = free / (2f * n)
                }
                "space-evenly" -> {
                    gap += free / (n + 1)
                    startOff = free / (n + 1)
                }
            }
        }
        var pos = startOff
        for (fi in line) {
            val p = fi.p ?: continue
            val m0 = if (row) fi.ml else fi.mt
            val m1 = if (row) fi.mr else fi.mb
            val c0 = if (row) fi.mt else fi.ml
            val c1 = if (row) fi.mb else fi.mr
            val mStart = m0 + (if (fi.autoM0) autoShare else 0f)
            val mEnd = m1 + (if (fi.autoM1) autoShare else 0f)
            val mainSize = (if (row) p.width else p.height).toFloat()
            val crossSize = (if (row) p.height else p.width).toFloat()
            var mainPos = pos + mStart
            val lineC = lineCross[li]
            val al = if (fi.it.alignSelf == "auto") spec.alignItems else fi.it.alignSelf
            val freeC = lineC - crossSize - c0 - c1
            var cPos = when {
                fi.autoC0 && fi.autoC1 -> c0 + maxOf(0f, freeC) / 2f
                fi.autoC0 -> c0 + maxOf(0f, freeC)
                al == "center" -> c0 + freeC / 2f
                al == "flex-end" || al == "end" -> c0 + freeC
                else -> c0
            }
            cPos += crossPos
            if (spec.reverse) mainPos = innerMain - mainPos - mainSize
            val x = if (row) padL + mainPos else padL + cPos
            val y = if (row) padT + cPos else padT + mainPos
            val rx = relOffX(fi.it, d, innerW)
            val ry = relOffY(fi.it, d, innerH)
            pls.add(Pl(p, Math.round(x + rx), Math.round(y + ry)))
            pos += mStart + mainSize + mEnd + gap
        }
        crossPos += lineCross[li] + crossGap + extraBetween
    }
    placeAbsolute(absList, width, height, padL, padT, pls)
    return layout(width, height) {
        for (pl in pls) pl.p.placeRelative(pl.x, pl.y)
    }
}

// ---------- grid 排版 ----------

private class Track(val kind: Int, val len: Len?, val fr: Float, val minLen: Len?)

private fun parseTrackToken(tok: String, ctx: LenCtx): Track? {
    val t = tok.trim().lowercase()
    if (t == "auto" || t == "min-content" || t == "max-content") return Track(2, null, 0f, null)
    if (t.endsWith("fr")) {
        val v = t.removeSuffix("fr").toFloatOrNull() ?: return null
        return Track(1, null, v, null)
    }
    if (t.startsWith("minmax(") && t.endsWith(")")) {
        val parts = splitTopLevel(t.substring(7, t.length - 1), ',').map { it.trim() }
        if (parts.size != 2) return null
        val mn = parseLen(parts[0], ctx)
        val mxFr = if (parts[1].endsWith("fr")) parts[1].removeSuffix("fr").toFloatOrNull() else null
        if (mxFr != null) return Track(3, null, mxFr, mn)
        val mx = parseLen(parts[1], ctx)
        return if (mx != null) Track(0, mx, 0f, null) else Track(2, null, 0f, mn)
    }
    val l = parseLen(t, ctx) ?: return null
    return Track(0, l, 0f, null)
}

private fun parseTracks(raw: String?, ctx: LenCtx, availPx: Float, gapPx: Float, d: Float): List<Track> {
    if (raw == null) return emptyList()
    val out = ArrayList<Track>()
    for (tok in splitSpaces(raw)) {
        val lt = tok.lowercase()
        if (lt.startsWith("repeat(") && lt.endsWith(")")) {
            val inner = splitTopLevel(tok.substring(7, tok.length - 1), ',').map { it.trim() }
            if (inner.size < 2) continue
            val tracks = splitSpaces(inner.drop(1).joinToString(" ")).mapNotNull { parseTrackToken(it, ctx) }
            if (tracks.isEmpty()) continue
            val countWord = inner[0].lowercase()
            val count = if (countWord == "auto-fill" || countWord == "auto-fit") {
                val first = tracks[0]
                val minPx = (first.minLen?.px ?: first.len?.px ?: 50f) * d
                if (availPx.isInfinite()) 1 else maxOf(1, ((availPx + gapPx) / (minPx + gapPx)).toInt())
            } else {
                countWord.toIntOrNull() ?: 1
            }
            for (k in 0 until minOf(count, 100)) out.addAll(tracks)
        } else {
            val t = parseTrackToken(tok, ctx)
            if (t != null) out.add(t)
        }
    }
    return out
}

private class GI(val m: Measurable, val it: Item) {
    var col = 0
    var row = 0
    var cs = 1
    var rs = 1
    var p: Placeable? = null
}

private fun MeasureScope.gridMeasure(spec: BoxSpec, ms: List<Measurable>, c: Constraints): MeasureResult {
    val d = density
    val padL = spec.padL * d
    val padT = spec.padT * d
    val padR = spec.padR * d
    val padB = spec.padB * d
    val tightW = c.hasBoundedWidth && c.minWidth == c.maxWidth
    val definiteH = c.hasBoundedHeight && c.minHeight == c.maxHeight
    val innerW = if (c.hasBoundedWidth) maxOf(0f, c.maxWidth - padL - padR) else INF
    val innerH = if (definiteH) maxOf(0f, c.maxHeight - padT - padB) else INF
    val colGap = lenPx(spec.colGap, if (innerW.isInfinite()) 0f else innerW, d) ?: 0f
    val rowGap = lenPx(spec.rowGap, if (innerH.isInfinite()) 0f else innerH, d) ?: 0f
    val absList = ArrayList<Pair<Measurable, Item>>()
    val gis = ArrayList<GI>()
    for (m in ms) {
        val it = itemOf(m)
        if (isAbs(it)) absList.add(Pair(m, it)) else gis.add(GI(m, it))
    }
    gis.sortBy { it.it.order }
    var cols = parseTracks(spec.gridCols, spec.ctx, innerW, colGap, d)
    if (cols.isEmpty()) cols = listOf(Track(1, null, 1f, null))
    val ncols = cols.size
    val explicitRows = parseTracks(spec.gridRows, spec.ctx, innerH, rowGap, d)
    val autoRowTrack = parseTrackToken(spec.gridAutoRows ?: "auto", spec.ctx) ?: Track(2, null, 0f, null)

    // 放格子
    val occupied = ArrayList<BooleanArray>()
    fun ensureRows(n: Int) {
        while (occupied.size < n) occupied.add(BooleanArray(ncols))
    }
    fun free(r: Int, cc: Int, rs: Int, cs: Int): Boolean {
        if (cc + cs > ncols) return false
        ensureRows(r + rs)
        for (i in r until r + rs) for (j in cc until cc + cs) if (occupied[i][j]) return false
        return true
    }
    var curR = 0
    var curC = 0
    for (g in gis) {
        g.cs = minOf(maxOf(1, g.it.colSpan), ncols)
        g.rs = maxOf(1, g.it.rowSpan)
        if (g.it.colStart > 0) {
            g.col = minOf(g.it.colStart - 1, ncols - g.cs)
            var r = if (g.it.rowStart > 0) g.it.rowStart - 1 else 0
            while (!free(r, g.col, g.rs, g.cs)) r++
            g.row = r
        } else if (g.it.rowStart > 0) {
            g.row = g.it.rowStart - 1
            var cc = 0
            while (cc + g.cs <= ncols && !free(g.row, cc, g.rs, g.cs)) cc++
            g.col = if (cc + g.cs <= ncols) cc else 0
        } else {
            var placed = false
            var r = curR
            var cc = curC
            while (!placed) {
                if (cc + g.cs > ncols) {
                    r++
                    cc = 0
                    continue
                }
                if (free(r, cc, g.rs, g.cs)) {
                    g.row = r
                    g.col = cc
                    placed = true
                } else {
                    cc++
                }
            }
            curR = g.row
            curC = g.col + g.cs
        }
        ensureRows(g.row + g.rs)
        for (i in g.row until g.row + g.rs) for (j in g.col until g.col + g.cs) occupied[i][j] = true
    }
    val nrows = maxOf(occupied.size, explicitRows.size)

    // 列宽
    val colW = FloatArray(ncols)
    val colNat = FloatArray(ncols)
    for (g in gis) {
        if (g.cs == 1) {
            val p = g.m.measure(mk(0f, if (innerW.isInfinite()) INF else innerW, 0f, INF))
            colNat[g.col] = maxOf(colNat[g.col], p.width + (lenPx(g.it.mL, innerW, d) ?: 0f) + (lenPx(g.it.mR, innerW, d) ?: 0f))
        }
    }
    var fixedSum = 0f
    var frSum = 0f
    for ((i, t) in cols.withIndex()) {
        when (t.kind) {
            0 -> {
                colW[i] = lenPx(t.len, innerW, d) ?: colNat[i]
                fixedSum += colW[i]
            }
            2 -> {
                colW[i] = maxOf(colNat[i], (t.minLen?.let { lenPx(it, innerW, d) } ?: 0f))
                fixedSum += colW[i]
            }
            else -> frSum += if (t.fr > 0f) t.fr else 1f
        }
    }
    val totalColGap = colGap * (ncols - 1)
    val remaining = if (innerW.isInfinite()) -1f else innerW - fixedSum - totalColGap
    for ((i, t) in cols.withIndex()) {
        if (t.kind == 1 || t.kind == 3) {
            val share = if (remaining < 0f) colNat[i] else remaining * (if (t.fr > 0f) t.fr else 1f) / frSum
            val mn = if (t.kind == 3) (t.minLen?.let { lenPx(it, innerW, d) } ?: 0f) else 0f
            colW[i] = if (remaining < 0f) colNat[i] else maxOf(share, mn)
        }
    }
    val colX = FloatArray(ncols)
    var acc = 0f
    for (i in 0 until ncols) {
        colX[i] = acc
        acc += colW[i] + colGap
    }

    // 行高
    val rowH = FloatArray(nrows)
    val rowTrack = Array(nrows) { i -> explicitRows.getOrNull(i) ?: autoRowTrack }
    for (g in gis) {
        val w = (0 until g.cs).sumOf { colW[g.col + it].toDouble() }.toFloat() + colGap * (g.cs - 1)
        val ml = lenPx(g.it.mL, innerW, d) ?: 0f
        val mr = lenPx(g.it.mR, innerW, d) ?: 0f
        val specH = sizeSpec(g.it.h, g.it.extraH, g.it.contentBox, innerH, d)
        val specW = sizeSpec(g.it.w, g.it.extraW, g.it.contentBox, w, d)
        val wFinal = specW ?: maxOf(0f, w - ml - mr)
        val p = g.m.measure(mk(wFinal, wFinal, specH ?: 0f, specH ?: INF))
        val mt = lenPx(g.it.mT, innerW, d) ?: 0f
        val mb = lenPx(g.it.mB, innerW, d) ?: 0f
        val need = p.height + mt + mb
        if (g.rs == 1) {
            rowH[g.row] = maxOf(rowH[g.row], need)
        } else {
            val have = (0 until g.rs).sumOf { rowH[g.row + it].toDouble() }.toFloat() + rowGap * (g.rs - 1)
            if (need > have) rowH[g.row + g.rs - 1] += need - have
        }
    }
    var rowFrSum = 0f
    var rowFixed = 0f
    for (i in 0 until nrows) {
        val t = rowTrack[i]
        if (t.kind == 0) {
            rowH[i] = lenPx(t.len, innerH, d) ?: rowH[i]
        } else if (t.kind == 1 || t.kind == 3) {
            rowFrSum += if (t.fr > 0f) t.fr else 1f
            continue
        }
        rowFixed += rowH[i]
    }
    if (rowFrSum > 0f && !innerH.isInfinite()) {
        val rem = maxOf(0f, innerH - rowFixed - rowGap * (nrows - 1))
        for (i in 0 until nrows) {
            val t = rowTrack[i]
            if (t.kind == 1 || t.kind == 3) rowH[i] = maxOf(rowH[i], rem * (if (t.fr > 0f) t.fr else 1f) / rowFrSum)
        }
    } else if (rowFrSum > 0f && innerH.isInfinite()) {
        // 高度不固定时，fr 行就按内容高度
    }
    val rowY = FloatArray(nrows)
    var accY = 0f
    for (i in 0 until nrows) {
        rowY[i] = accY
        accY += rowH[i] + rowGap
    }
    val contentW = if (ncols == 0) 0f else acc - colGap
    val contentH = if (nrows == 0) 0f else accY - rowGap

    val width = if (tightW) c.maxWidth else c.constrainWidth(toI(contentW + padL + padR))
    val height = if (definiteH) c.maxHeight else c.constrainHeight(toI(contentH + padT + padB))
    val pls = ArrayList<Pl>()
    for (g in gis) {
        val cellW = (0 until g.cs).sumOf { colW[g.col + it].toDouble() }.toFloat() + colGap * (g.cs - 1)
        val cellH = (0 until g.rs).sumOf { rowH[g.row + it].toDouble() }.toFloat() + rowGap * (g.rs - 1)
        val ml = lenPx(g.it.mL, innerW, d) ?: 0f
        val mr = lenPx(g.it.mR, innerW, d) ?: 0f
        val mt = lenPx(g.it.mT, innerW, d) ?: 0f
        val mb = lenPx(g.it.mB, innerW, d) ?: 0f
        val specW = sizeSpec(g.it.w, g.it.extraW, g.it.contentBox, cellW, d)
        val specH = sizeSpec(g.it.h, g.it.extraH, g.it.contentBox, cellH, d)
        val alignY = if (g.it.alignSelf == "auto") spec.alignItems else g.it.alignSelf
        val stretchH = specH == null && isStretch(alignY)
        val wF = specW ?: maxOf(0f, cellW - ml - mr)
        val hF = specH ?: if (stretchH) maxOf(0f, cellH - mt - mb) else null
        val p = g.m.measure(mk(wF, wF, hF ?: 0f, hF ?: INF))
        g.p = p
        val freeY = cellH - mt - mb - p.height
        val offY = when (alignY) {
            "center" -> freeY / 2f
            "flex-end", "end" -> freeY
            else -> 0f
        }
        val x = padL + colX[g.col] + ml + relOffX(g.it, d, innerW)
        val y = padT + rowY[g.row] + mt + offY + relOffY(g.it, d, innerH)
        pls.add(Pl(p, Math.round(x), Math.round(y)))
    }
    placeAbsolute(absList, width, height, padL, padT, pls)
    return layout(width, height) {
        for (pl in pls) pl.p.placeRelative(pl.x, pl.y)
    }
}

// ---------- 行内排布（文字和行内块混排） ----------

private fun MeasureScope.flowMeasure(spec: BoxSpec, ms: List<Measurable>, c: Constraints): MeasureResult {
    val d = density
    val padL = spec.padL * d
    val padT = spec.padT * d
    val padR = spec.padR * d
    val padB = spec.padB * d
    val tightW = c.hasBoundedWidth && c.minWidth == c.maxWidth
    val definiteH = c.hasBoundedHeight && c.minHeight == c.maxHeight
    val innerW = if (c.hasBoundedWidth) maxOf(0f, c.maxWidth - padL - padR) else INF
    val absList = ArrayList<Pair<Measurable, Item>>()
    class Cell(val p: Placeable, val ml: Float, val mr: Float, val mt: Float, val mb: Float, val it: Item)
    val lines = ArrayList<ArrayList<Cell>>()
    var cur = ArrayList<Cell>()
    var curW = 0f
    for (m in ms) {
        val it = itemOf(m)
        if (isAbs(it)) {
            absList.add(Pair(m, it))
            continue
        }
        val ml = lenPx(it.mL, innerW, d) ?: 0f
        val mr = lenPx(it.mR, innerW, d) ?: 0f
        val mt = lenPx(it.mT, innerW, d) ?: 0f
        val mb = lenPx(it.mB, innerW, d) ?: 0f
        val w = sizeSpec(it.w, it.extraW, it.contentBox, innerW, d)
        val h = sizeSpec(it.h, it.extraH, it.contentBox, INF, d)
        val avail = if (innerW.isInfinite()) INF else maxOf(0f, innerW - ml - mr)
        val cons = mk(w ?: 0f, w ?: avail, h ?: 0f, h ?: INF)
        val p = m.measure(cons)
        val outer = p.width + ml + mr
        if (cur.isNotEmpty() && !innerW.isInfinite() && curW + outer > innerW + 0.5f) {
            lines.add(cur)
            cur = ArrayList()
            curW = 0f
        }
        cur.add(Cell(p, ml, mr, mt, mb, it))
        curW += outer
    }
    if (cur.isNotEmpty()) lines.add(cur)
    var maxLine = 0f
    var totalH = 0f
    val lineH = FloatArray(lines.size)
    val lineW = FloatArray(lines.size)
    for ((i, line) in lines.withIndex()) {
        var w = 0f
        var h = 0f
        for (cell in line) {
            w += cell.p.width + cell.ml + cell.mr
            h = maxOf(h, cell.p.height + cell.mt + cell.mb)
        }
        lineW[i] = w
        lineH[i] = h
        maxLine = maxOf(maxLine, w)
        totalH += h
    }
    val width = if (tightW) c.maxWidth else c.constrainWidth(toI(maxLine + padL + padR))
    val height = if (definiteH) c.maxHeight else c.constrainHeight(toI(totalH + padT + padB))
    val inner = maxOf(0f, width - padL - padR)
    val pls = ArrayList<Pl>()
    var y = padT
    for ((i, line) in lines.withIndex()) {
        val free = inner - lineW[i]
        var x = padL + when (spec.textAlign) {
            "center" -> free / 2f
            "right", "end" -> free
            else -> 0f
        }
        for (cell in line) {
            val offY = (lineH[i] - cell.p.height - cell.mt - cell.mb) / 2f
            x += cell.ml
            pls.add(Pl(cell.p, Math.round(x + relOffX(cell.it, d, innerW)), Math.round(y + cell.mt + offY + relOffY(cell.it, d, INF))))
            x += cell.p.width + cell.mr
        }
        y += lineH[i]
    }
    placeAbsolute(absList, width, height, padL, padT, pls)
    return layout(width, height) {
        for (pl in pls) pl.p.placeRelative(pl.x, pl.y)
    }
}
