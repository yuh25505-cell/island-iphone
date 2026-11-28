package com.island.phone.app.home.widget.custom

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal class Len(val px: Float, val pct: Float) {
    fun resolve(basis: Float): Float = px + pct * basis / 100f

    companion object {
        val Zero = Len(0f, 0f)
    }
}

internal class LenCtx(val fontPx: Float, val vw: Float, val vh: Float)

private val NumUnitRe = Regex("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)([a-zA-Z%]*)$")

private fun unitLen(v: Float, unit: String, ctx: LenCtx): Len? = when (unit) {
    "px", "" -> Len(v, 0f)
    "em" -> Len(v * ctx.fontPx, 0f)
    "rem" -> Len(v * 16f, 0f)
    "pt" -> Len(v * 4f / 3f, 0f)
    "%" -> Len(0f, v)
    "vw" -> Len(v * ctx.vw / 100f, 0f)
    "vh" -> Len(v * ctx.vh / 100f, 0f)
    "vmin" -> Len(v * minOf(ctx.vw, ctx.vh) / 100f, 0f)
    "vmax" -> Len(v * maxOf(ctx.vw, ctx.vh) / 100f, 0f)
    else -> null
}

private class CalcParser(private val s: String, private val ctx: LenCtx) {
    private var i = 0

    private class V(val len: Len?, val num: Float?)

    fun run(): Len? {
        val v = expr() ?: return null
        skipWs()
        if (i != s.length) return null
        return v.len ?: v.num?.let { Len(it, 0f) }
    }

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun expr(): V? {
        var a = term() ?: return null
        while (true) {
            skipWs()
            if (i >= s.length) break
            val c = s[i]
            if (c != '+' && c != '-') break
            i++
            val b = term() ?: return null
            a = addSub(a, b, c == '+') ?: return null
        }
        return a
    }

    private fun term(): V? {
        var a = factor() ?: return null
        while (true) {
            skipWs()
            if (i >= s.length) break
            val c = s[i]
            if (c != '*' && c != '/') break
            i++
            val b = factor() ?: return null
            a = mulDiv(a, b, c == '*') ?: return null
        }
        return a
    }

    private fun factor(): V? {
        skipWs()
        if (i >= s.length) return null
        if (s[i] == '(') {
            i++
            val v = expr() ?: return null
            skipWs()
            if (i >= s.length || s[i] != ')') return null
            i++
            return v
        }
        val st = i
        if (s[i] == '+' || s[i] == '-') i++
        while (i < s.length && !s[i].isWhitespace() && s[i] !in "()*/+") i++
        val tok = s.substring(st, i)
        val m = NumUnitRe.find(tok) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        val u = m.groupValues[2]
        if (u.isEmpty()) return V(null, n)
        val len = unitLen(n, u, ctx) ?: return null
        return V(len, null)
    }

    private fun addSub(a: V, b: V, plus: Boolean): V? {
        val sign = if (plus) 1f else -1f
        val al = a.len
        val bl = b.len
        if (al != null && bl != null) return V(Len(al.px + sign * bl.px, al.pct + sign * bl.pct), null)
        val an = a.num
        val bn = b.num
        if (an != null && bn != null) return V(null, an + sign * bn)
        return null
    }

    private fun mulDiv(a: V, b: V, mul: Boolean): V? {
        val al = a.len
        val bl = b.len
        val an = a.num
        val bn = b.num
        if (mul) {
            if (al != null && bn != null) return V(Len(al.px * bn, al.pct * bn), null)
            if (an != null && bl != null) return V(Len(bl.px * an, bl.pct * an), null)
            if (an != null && bn != null) return V(null, an * bn)
            return null
        }
        if (bn == null || bn == 0f) return null
        if (al != null) return V(Len(al.px / bn, al.pct / bn), null)
        if (an != null) return V(null, an / bn)
        return null
    }
}

internal fun parseLen(raw: String?, ctx: LenCtx): Len? {
    if (raw == null) return null
    val t = raw.trim().lowercase()
    if (t.isEmpty() || t == "auto" || t == "none" || t == "normal" || t == "initial") return null
    if (t.startsWith("calc(") && t.endsWith(")")) return CalcParser(t.substring(5, t.length - 1), ctx).run()
    val m = NumUnitRe.find(t) ?: return null
    val n = m.groupValues[1].toFloatOrNull() ?: return null
    return unitLen(n, m.groupValues[2], ctx)
}

internal fun parseNumber(raw: String?): Float? = raw?.trim()?.toFloatOrNull()

internal fun parseUrl(raw: String): String? {
    val t = raw.trim()
    if (!t.lowercase().startsWith("url(") || !t.endsWith(")")) return null
    var inner = t.substring(4, t.length - 1).trim()
    if (inner.length >= 2 && (inner[0] == '"' || inner[0] == '\'') && inner.last() == inner[0]) {
        inner = inner.substring(1, inner.length - 1)
    }
    return inner
}

// ---------- 颜色 ----------

private val NamedColors: Map<String, Int> = mapOf(
    "black" to 0x000000, "white" to 0xffffff, "red" to 0xff0000, "green" to 0x008000, "blue" to 0x0000ff,
    "yellow" to 0xffff00, "orange" to 0xffa500, "purple" to 0x800080, "pink" to 0xffc0cb, "gray" to 0x808080,
    "grey" to 0x808080, "silver" to 0xc0c0c0, "brown" to 0xa52a2a, "cyan" to 0x00ffff, "aqua" to 0x00ffff,
    "magenta" to 0xff00ff, "fuchsia" to 0xff00ff, "navy" to 0x000080, "teal" to 0x008080, "lime" to 0x00ff00,
    "maroon" to 0x800000, "olive" to 0x808000, "gold" to 0xffd700, "coral" to 0xff7f50, "salmon" to 0xfa8072,
    "skyblue" to 0x87ceeb, "lightblue" to 0xadd8e6, "lightgray" to 0xd3d3d3, "lightgrey" to 0xd3d3d3,
    "darkgray" to 0xa9a9a9, "darkgrey" to 0xa9a9a9, "violet" to 0xee82ee, "indigo" to 0x4b0082,
    "tomato" to 0xff6347, "crimson" to 0xdc143c, "beige" to 0xf5f5dc, "ivory" to 0xfffff0, "khaki" to 0xf0e68c,
    "lavender" to 0xe6e6fa, "plum" to 0xdda0dd, "orchid" to 0xda70d6, "turquoise" to 0x40e0d0,
    "hotpink" to 0xff69b4, "lightpink" to 0xffb6c1, "lightgreen" to 0x90ee90, "lightyellow" to 0xffffe0,
    "darkblue" to 0x00008b, "darkgreen" to 0x006400, "darkred" to 0x8b0000, "whitesmoke" to 0xf5f5f5,
    "gainsboro" to 0xdcdcdc, "snow" to 0xfffafa, "wheat" to 0xf5deb3, "tan" to 0xd2b48c, "peachpuff" to 0xffdab9,
    "mistyrose" to 0xffe4e1, "lemonchiffon" to 0xfffacd, "aliceblue" to 0xf0f8ff, "steelblue" to 0x4682b4,
    "royalblue" to 0x4169e1, "slategray" to 0x708090, "dimgray" to 0x696969, "chocolate" to 0xd2691e,
)

private fun hslToColor(h: Float, s: Float, l: Float, a: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f / 360f
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun hue(tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val r = if (s == 0f) l else hue(hh + 1f / 3f)
    val g = if (s == 0f) l else hue(hh)
    val b = if (s == 0f) l else hue(hh - 1f / 3f)
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
}

private val SplitRe = Regex("[,\\s/]+")

internal fun parseColorValue(raw: String?, current: Color = Color.Black): Color? {
    if (raw == null) return null
    val t = raw.trim().lowercase()
    if (t.isEmpty()) return null
    if (t == "transparent") return Color.Transparent
    if (t == "currentcolor") return current
    if (t.startsWith("#")) {
        val hex = t.substring(1)
        if (hex.isEmpty() || !hex.all { it in '0'..'9' || it in 'a'..'f' }) return null
        val full = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("") + "ff"
            4 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex + "ff"
            8 -> hex
            else -> return null
        }
        val r = full.substring(0, 2).toInt(16)
        val g = full.substring(2, 4).toInt(16)
        val b = full.substring(4, 6).toInt(16)
        val a = full.substring(6, 8).toInt(16)
        return Color(r, g, b, a)
    }
    if ((t.startsWith("rgb") || t.startsWith("hsl")) && t.contains('(') && t.endsWith(")")) {
        val inner = t.substring(t.indexOf('(') + 1, t.length - 1)
        val tok = inner.split(SplitRe).filter { it.isNotEmpty() }
        if (tok.size < 3) return null
        val alpha = if (tok.size > 3) {
            val a = tok[3]
            if (a.endsWith("%")) (a.removeSuffix("%").toFloatOrNull() ?: return null) / 100f else (a.toFloatOrNull() ?: return null)
        } else {
            1f
        }
        if (t.startsWith("rgb")) {
            fun comp(x: String): Float? {
                return if (x.endsWith("%")) x.removeSuffix("%").toFloatOrNull()?.let { it / 100f * 255f } else x.toFloatOrNull()
            }
            val r = comp(tok[0]) ?: return null
            val g = comp(tok[1]) ?: return null
            val b = comp(tok[2]) ?: return null
            return Color(
                r.coerceIn(0f, 255f).toInt(), g.coerceIn(0f, 255f).toInt(), b.coerceIn(0f, 255f).toInt(),
                (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt(),
            )
        }
        val h = tok[0].removeSuffix("deg").toFloatOrNull() ?: return null
        val s = (tok[1].removeSuffix("%").toFloatOrNull() ?: return null) / 100f
        val l = (tok[2].removeSuffix("%").toFloatOrNull() ?: return null) / 100f
        return hslToColor(h, s, l, alpha)
    }
    val named = NamedColors[t] ?: return null
    return Color(0xFF000000.toInt() or named)
}

// ---------- 渐变 ----------

internal class GradStop(val frac: Float?, val px: Float?, val color: Color)
internal class Gradient(
    val radial: Boolean,
    val angleDeg: Float,
    val cx: Float,
    val cy: Float,
    val stops: List<GradStop>,
)

private val AngleRe = Regex("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+))(deg|rad|turn|grad)$")

internal fun parseAngle(raw: String): Float? {
    val m = AngleRe.find(raw.trim().lowercase()) ?: return null
    val v = m.groupValues[1].toFloatOrNull() ?: return null
    return when (m.groupValues[2]) {
        "deg" -> v
        "rad" -> (v * 180.0 / Math.PI).toFloat()
        "turn" -> v * 360f
        else -> v * 0.9f
    }
}

private fun dirAngle(words: List<String>): Float? {
    val top = "top" in words
    val bottom = "bottom" in words
    val left = "left" in words
    val right = "right" in words
    return when {
        top && right -> 45f
        bottom && right -> 135f
        bottom && left -> 225f
        top && left -> 315f
        top -> 0f
        right -> 90f
        bottom -> 180f
        left -> 270f
        else -> null
    }
}

private fun posFraction(word: String): Float? = when (word) {
    "left", "top" -> 0f
    "center" -> 0.5f
    "right", "bottom" -> 1f
    else -> if (word.endsWith("%")) word.removeSuffix("%").toFloatOrNull()?.let { it / 100f } else null
}

internal fun parseGradient(raw: String, current: Color, ctx: LenCtx): Gradient? {
    val t = raw.trim()
    val lower = t.lowercase()
    val radial = lower.contains("radial-gradient(")
    if (!lower.contains("linear-gradient(") && !radial) return null
    if (lower.contains("conic-gradient(")) return null
    val open = t.indexOf('(')
    val close = t.lastIndexOf(')')
    if (open < 0 || close <= open) return null
    val args = splitTopLevel(t.substring(open + 1, close), ',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    if (args.isEmpty()) return null
    var angle = 180f
    var cx = 0.5f
    var cy = 0.5f
    val first = args[0].lowercase()
    if (!radial) {
        if (first.startsWith("to ")) {
            angle = dirAngle(first.removePrefix("to ").split(' ').filter { it.isNotEmpty() }) ?: 180f
            args.removeAt(0)
        } else {
            val a = parseAngle(first)
            if (a != null) {
                angle = a
                args.removeAt(0)
            }
        }
    } else if (first.startsWith("circle") || first.startsWith("ellipse") || first.startsWith("closest") ||
        first.startsWith("farthest") || first.startsWith("at ")
    ) {
        val at = first.indexOf("at ")
        if (at >= 0) {
            val words = first.substring(at + 3).split(' ').filter { it.isNotEmpty() }
            if (words.isNotEmpty()) {
                val w0 = words[0]
                val w1 = words.getOrNull(1)
                if (w0 == "top" || w0 == "bottom") {
                    cy = posFraction(w0) ?: 0.5f
                    cx = if (w1 != null) posFraction(w1) ?: 0.5f else 0.5f
                } else {
                    cx = posFraction(w0) ?: 0.5f
                    cy = if (w1 != null) posFraction(w1) ?: 0.5f else 0.5f
                }
            }
        }
        args.removeAt(0)
    }
    val stops = ArrayList<GradStop>()
    for (arg in args) {
        val toks = splitSpaces(arg).toMutableList()
        val positions = ArrayList<Len>()
        while (toks.size > 1 && positions.size < 2) {
            val l = parseLen(toks.last(), ctx) ?: break
            val lastTok = toks.last().lowercase()
            if (!(lastTok.endsWith("%") || lastTok.endsWith("px") || lastTok == "0")) break
            positions.add(0, l)
            toks.removeAt(toks.size - 1)
        }
        val color = parseColorValue(toks.joinToString(" "), current) ?: return null
        if (positions.isEmpty()) {
            stops.add(GradStop(null, null, color))
        } else {
            for (p in positions) {
                if (p.pct != 0f || p.px == 0f) stops.add(GradStop(p.pct / 100f, null, color)) else stops.add(GradStop(null, p.px, color))
            }
        }
    }
    if (stops.isEmpty()) return null
    return Gradient(radial, angle, cx, cy, stops)
}

internal fun Gradient.brush(w: Float, h: Float): Brush? {
    if (w <= 0f || h <= 0f) return null
    var start = Offset.Zero
    var end = Offset.Zero
    var center = Offset.Zero
    val lineLen: Float
    var radius = 0f
    if (!radial) {
        val a = Math.toRadians(angleDeg.toDouble())
        val dx = Math.sin(a).toFloat()
        val dy = (-Math.cos(a)).toFloat()
        lineLen = Math.abs(w * dx) + Math.abs(h * dy)
        val c = Offset(w / 2f, h / 2f)
        start = Offset(c.x - dx * lineLen / 2f, c.y - dy * lineLen / 2f)
        end = Offset(c.x + dx * lineLen / 2f, c.y + dy * lineLen / 2f)
    } else {
        center = Offset(cx * w, cy * h)
        val fx = maxOf(center.x, w - center.x)
        val fy = maxOf(center.y, h - center.y)
        radius = Math.sqrt((fx * fx + fy * fy).toDouble()).toFloat()
        lineLen = radius
    }
    val fr = arrayOfNulls<Float>(stops.size)
    for ((i, s) in stops.withIndex()) {
        fr[i] = s.frac ?: s.px?.let { if (lineLen > 0f) it / lineLen else 0f }
    }
    if (fr[0] == null) fr[0] = 0f
    if (fr[fr.size - 1] == null) fr[fr.size - 1] = 1f
    var i = 1
    while (i < fr.size) {
        if (fr[i] == null) {
            var j = i
            while (fr[j] == null) j++
            val a = fr[i - 1] ?: 0f
            val b = fr[j] ?: 1f
            val n = j - i + 1
            for (k in i until j) fr[k] = a + (b - a) * (k - i + 1).toFloat() / n.toFloat()
            i = j
        }
        i++
    }
    val pairs = ArrayList<Pair<Float, Color>>()
    var prev = 0f
    for ((k, s) in stops.withIndex()) {
        val f = maxOf(prev, (fr[k] ?: 0f).coerceIn(0f, 1f))
        prev = f
        pairs.add(Pair(f, s.color))
    }
    if (pairs.size == 1) pairs.add(Pair(1f, pairs[0].second))
    val arr = pairs.toTypedArray()
    return if (radial) {
        Brush.radialGradient(*arr, center = center, radius = if (radius > 0f) radius else 1f)
    } else {
        Brush.linearGradient(*arr, start = start, end = end)
    }
}

// ---------- 阴影 ----------

internal class BoxShadow(
    val dx: Float,
    val dy: Float,
    val blur: Float,
    val spread: Float,
    val color: Color,
    val inset: Boolean,
)

private fun looksLikeLength(tok: String): Boolean {
    val c = tok.firstOrNull() ?: return false
    return c.isDigit() || c == '-' || c == '.' || c == '+'
}

internal fun parseShadows(raw: String?, current: Color, ctx: LenCtx): List<BoxShadow> {
    if (raw == null || raw.trim().lowercase() == "none") return emptyList()
    val out = ArrayList<BoxShadow>()
    for (layer in splitTopLevel(raw, ',')) {
        var inset = false
        var color = Color(0f, 0f, 0f, 0.25f)
        val lens = ArrayList<Float>()
        for (tok in splitSpaces(layer.trim())) {
            if (tok.lowercase() == "inset") {
                inset = true
            } else if (looksLikeLength(tok)) {
                lens.add(parseLen(tok, ctx)?.px ?: 0f)
            } else {
                color = parseColorValue(tok, current) ?: color
            }
        }
        if (lens.size >= 2) {
            out.add(BoxShadow(lens[0], lens[1], lens.getOrElse(2) { 0f }, lens.getOrElse(3) { 0f }, color, inset))
        }
    }
    return out
}

// ---------- 变形 ----------

internal class Xf(
    var tx: Len = Len.Zero,
    var ty: Len = Len.Zero,
    var sx: Float = 1f,
    var sy: Float = 1f,
    var rot: Float = 0f,
)

internal fun parseTransform(raw: String?, ctx: LenCtx): Xf {
    val xf = Xf()
    if (raw == null || raw.trim().lowercase() == "none") return xf
    var i = 0
    val s = raw.trim()
    while (i < s.length) {
        while (i < s.length && s[i].isWhitespace()) i++
        if (i >= s.length) break
        val open = s.indexOf('(', i)
        if (open < 0) break
        val name = s.substring(i, open).trim().lowercase()
        var depth = 0
        var j = open
        while (j < s.length) {
            if (s[j] == '(') depth++
            if (s[j] == ')') {
                depth--
                if (depth == 0) break
            }
            j++
        }
        if (j >= s.length) break
        val args = splitTopLevel(s.substring(open + 1, j), ',').map { it.trim() }
        i = j + 1
        fun len(k: Int): Len? = args.getOrNull(k)?.let { parseLen(it, ctx) ?: if (it == "0") Len.Zero else null }
        fun num(k: Int): Float? = args.getOrNull(k)?.toFloatOrNull()
        when (name) {
            "translate", "translate3d" -> {
                len(0)?.let { xf.tx = Len(xf.tx.px + it.px, xf.tx.pct + it.pct) }
                len(1)?.let { xf.ty = Len(xf.ty.px + it.px, xf.ty.pct + it.pct) }
            }
            "translatex" -> len(0)?.let { xf.tx = Len(xf.tx.px + it.px, xf.tx.pct + it.pct) }
            "translatey" -> len(0)?.let { xf.ty = Len(xf.ty.px + it.px, xf.ty.pct + it.pct) }
            "scale", "scale3d" -> {
                val a = num(0) ?: 1f
                xf.sx *= a
                xf.sy *= num(1) ?: a
            }
            "scalex" -> xf.sx *= num(0) ?: 1f
            "scaley" -> xf.sy *= num(0) ?: 1f
            "rotate", "rotatez" -> xf.rot += args.getOrNull(0)?.let { parseAngle(it) ?: if (it == "0") 0f else null } ?: 0f
        }
    }
    return xf
}

internal fun Xf.serialize(): String {
    fun l(v: Len): String = if (v.pct == 0f) "${v.px}px" else "calc(${v.px}px + ${v.pct}%)"
    return "translate(${l(tx)}, ${l(ty)}) scale($sx, $sy) rotate(${rot}deg)"
}

// ---------- 时间曲线 ----------

private fun bezier(x1: Float, y1: Float, x2: Float, y2: Float, t: Float): Float {
    var lo = 0f
    var hi = 1f
    for (k in 0 until 24) {
        val m = (lo + hi) / 2f
        val x = 3f * (1f - m) * (1f - m) * m * x1 + 3f * (1f - m) * m * m * x2 + m * m * m
        if (x < t) lo = m else hi = m
    }
    val s = (lo + hi) / 2f
    return 3f * (1f - s) * (1f - s) * s * y1 + 3f * (1f - s) * s * s * y2 + s * s * s
}

internal fun easeValue(timing: String, tIn: Float): Float {
    val t = tIn.coerceIn(0f, 1f)
    val f = timing.trim().lowercase()
    return when {
        f == "linear" -> t
        f == "ease-in" -> bezier(0.42f, 0f, 1f, 1f, t)
        f == "ease-out" -> bezier(0f, 0f, 0.58f, 1f, t)
        f == "ease-in-out" -> bezier(0.42f, 0f, 0.58f, 1f, t)
        f == "step-start" -> if (t > 0f) 1f else 0f
        f == "step-end" -> if (t >= 1f) 1f else 0f
        f.startsWith("cubic-bezier(") -> {
            val p = f.substring(13, f.length - 1).split(',').map { it.trim().toFloatOrNull() }
            if (p.size == 4 && p.all { it != null }) bezier(p[0]!!, p[1]!!, p[2]!!, p[3]!!, t) else bezier(0.25f, 0.1f, 0.25f, 1f, t)
        }
        f.startsWith("steps(") -> {
            val p = f.substring(6, f.length - 1).split(',').map { it.trim() }
            val n = p.getOrNull(0)?.toIntOrNull() ?: 1
            if (n <= 0) t else Math.floor((t * n).toDouble()).toFloat().coerceAtMost(n.toFloat()) / n.toFloat()
        }
        else -> bezier(0.25f, 0.1f, 0.25f, 1f, t)
    }
}
