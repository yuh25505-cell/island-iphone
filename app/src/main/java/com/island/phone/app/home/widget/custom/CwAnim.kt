package com.island.phone.app.home.widget.custom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor

internal class AnimSpec(
    val name: String,
    val durMs: Float,
    val delayMs: Float,
    val timing: String,
    val iterations: Float,
    val direction: String,
    val fill: String,
)

internal class TransSpec(val prop: String, val durMs: Float, val delayMs: Float, val timing: String)

private val TimingWords = setOf("linear", "ease", "ease-in", "ease-out", "ease-in-out", "step-start", "step-end")
private val DirectionWords = setOf("normal", "reverse", "alternate", "alternate-reverse")
private val FillWords = setOf("none", "forwards", "backwards", "both")

private fun timeMs(tok: String): Float? {
    val t = tok.lowercase()
    return when {
        t.endsWith("ms") -> t.removeSuffix("ms").toFloatOrNull()
        t.endsWith("s") -> t.removeSuffix("s").toFloatOrNull()?.times(1000f)
        else -> null
    }
}

private fun parseOneAnim(part: String): AnimSpec? {
    var name = ""
    var dur = -1f
    var delay = 0f
    var timing = "ease"
    var iter = 1f
    var dir = "normal"
    var fill = "none"
    for (tok in splitSpaces(part)) {
        val lt = tok.lowercase()
        val ms = timeMs(lt)
        when {
            ms != null -> if (dur < 0f) dur = ms else delay = ms
            lt in TimingWords || lt.startsWith("cubic-bezier(") || lt.startsWith("steps(") -> timing = lt
            lt == "infinite" -> iter = Float.POSITIVE_INFINITY
            lt.toFloatOrNull() != null -> iter = lt.toFloat()
            lt in DirectionWords -> dir = lt
            lt in FillWords && lt != "none" -> fill = lt
            lt == "none" || lt == "running" || lt == "paused" -> {}
            else -> name = tok
        }
    }
    if (name.isEmpty()) return null
    return AnimSpec(name, if (dur < 0f) 0f else dur, delay, timing, iter, dir, fill)
}

internal fun parseAnimations(cs: Map<String, String>): List<AnimSpec> {
    val specs = ArrayList<AnimSpec>()
    val raw = cs["animation"]
    if (raw != null && raw.trim().lowercase() != "none") {
        for (part in splitTopLevel(raw, ',')) {
            val s = parseOneAnim(part.trim())
            if (s != null) specs.add(s)
        }
    }
    val ln = cs["animation-name"]
    if (ln != null && ln.trim().lowercase() != "none") {
        val base = specs.firstOrNull()
        val name = ln.split(',')[0].trim()
        val dur = cs["animation-duration"]?.let { timeMs(it.split(',')[0].trim()) } ?: base?.durMs ?: 0f
        val delay = cs["animation-delay"]?.let { timeMs(it.split(',')[0].trim()) } ?: base?.delayMs ?: 0f
        val timing = cs["animation-timing-function"]?.trim() ?: base?.timing ?: "ease"
        val iterRaw = cs["animation-iteration-count"]?.trim()?.lowercase()
        val iter = if (iterRaw == "infinite") Float.POSITIVE_INFINITY else iterRaw?.toFloatOrNull() ?: base?.iterations ?: 1f
        val dir = cs["animation-direction"]?.trim()?.lowercase() ?: base?.direction ?: "normal"
        val fill = cs["animation-fill-mode"]?.trim()?.lowercase() ?: base?.fill ?: "none"
        if (specs.isNotEmpty()) specs.removeAt(0)
        specs.add(0, AnimSpec(name, dur, delay, timing, iter, dir, fill))
    } else if (ln != null && specs.isNotEmpty()) {
        specs.clear()
    }
    return specs
}

internal val AnimatableProps = listOf(
    "transform", "opacity", "color", "background-color", "border-top-color", "border-right-color",
    "border-bottom-color", "border-left-color", "width", "height", "top", "left", "right", "bottom",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "padding-top", "padding-right",
    "padding-bottom", "padding-left", "font-size", "letter-spacing", "flex-grow",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "row-gap", "column-gap",
)

private fun expandTransName(name: String): List<String> = when (name) {
    "all" -> AnimatableProps
    "background" -> listOf("background-color")
    "margin" -> listOf("margin-top", "margin-right", "margin-bottom", "margin-left")
    "padding" -> listOf("padding-top", "padding-right", "padding-bottom", "padding-left")
    "border-radius" -> listOf("border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius")
    "border-color", "border" -> listOf("border-top-color", "border-right-color", "border-bottom-color", "border-left-color")
    "gap" -> listOf("row-gap", "column-gap")
    "inset" -> listOf("top", "right", "bottom", "left")
    else -> listOf(name)
}

internal fun parseTransitions(cs: Map<String, String>): List<TransSpec> {
    val out = ArrayList<TransSpec>()
    val raw = cs["transition"]
    if (raw != null && raw.trim().lowercase() != "none") {
        for (part in splitTopLevel(raw, ',')) {
            var prop = "all"
            var dur = -1f
            var delay = 0f
            var timing = "ease"
            for (tok in splitSpaces(part.trim())) {
                val lt = tok.lowercase()
                val ms = timeMs(lt)
                when {
                    ms != null -> if (dur < 0f) dur = ms else delay = ms
                    lt in TimingWords || lt.startsWith("cubic-bezier(") || lt.startsWith("steps(") -> timing = lt
                    else -> prop = lt
                }
            }
            for (p in expandTransName(prop)) out.add(TransSpec(p, if (dur < 0f) 0f else dur, delay, timing))
        }
        return out
    }
    val durs = cs["transition-duration"]?.split(',')?.map { timeMs(it.trim()) ?: 0f }
    if (durs != null) {
        val props = (cs["transition-property"] ?: "all").split(',').map { it.trim().lowercase() }
        val delays = cs["transition-delay"]?.split(',')?.map { timeMs(it.trim()) ?: 0f }
        val timings = cs["transition-timing-function"]?.let { splitTopLevel(it, ',').map { x -> x.trim() } }
        for ((i, p) in props.withIndex()) {
            val d = durs[i % durs.size]
            val dl = delays?.let { it[i % it.size] } ?: 0f
            val tf = timings?.let { it[i % it.size] } ?: "ease"
            for (n in expandTransName(p)) out.add(TransSpec(n, d, dl, tf))
        }
    }
    return out
}

// ---------- 数值插值 ----------

private fun fmt(v: Float): String = if (v == Math.floor(v.toDouble()).toFloat()) v.toInt().toString() else v.toString()

private fun colorText(c: Color): String =
    "rgba(" + Math.round(c.red * 255f) + "," + Math.round(c.green * 255f) + "," + Math.round(c.blue * 255f) + "," + c.alpha + ")"

internal fun lerpProp(prop: String, a: String, b: String, t: Float, ctx: LenCtx, current: Color): String {
    if (t <= 0f) return a
    if (t >= 1f) return b
    if (prop == "transform") {
        val xa = parseTransform(a, ctx)
        val xb = parseTransform(b, ctx)
        val r = Xf(
            Len(xa.tx.px + (xb.tx.px - xa.tx.px) * t, xa.tx.pct + (xb.tx.pct - xa.tx.pct) * t),
            Len(xa.ty.px + (xb.ty.px - xa.ty.px) * t, xa.ty.pct + (xb.ty.pct - xa.ty.pct) * t),
            xa.sx + (xb.sx - xa.sx) * t,
            xa.sy + (xb.sy - xa.sy) * t,
            xa.rot + (xb.rot - xa.rot) * t,
        )
        return r.serialize()
    }
    val na = a.trim().toFloatOrNull()
    val nb = b.trim().toFloatOrNull()
    if (na != null && nb != null) return fmt(na + (nb - na) * t)
    if (prop.contains("color") || prop == "fill" || prop == "stroke") {
        val ca = parseColorValue(a, current)
        val cb = parseColorValue(b, current)
        if (ca != null && cb != null) return colorText(lerpColor(ca, cb, t))
    }
    val la = parseLen(a, ctx)
    val lb = parseLen(b, ctx)
    if (la != null && lb != null) {
        val px = la.px + (lb.px - la.px) * t
        val pct = la.pct + (lb.pct - la.pct) * t
        return if (pct == 0f) fmt(px) + "px" else "calc(" + fmt(px) + "px + " + fmt(pct) + "%)"
    }
    return if (t < 0.5f) a else b
}

// ---------- 关键帧动画 ----------

internal class AnimResult(val values: Map<String, String>?, val running: Boolean)

private fun defaultFor(prop: String): String? = when (prop) {
    "opacity" -> "1"
    "transform" -> "none"
    else -> null
}

private fun sampleAt(
    kf: Keyframes,
    p: Float,
    timing: String,
    base: Map<String, String>,
    ctx: LenCtx,
    current: Color,
): Map<String, String> {
    val out = HashMap<String, String>()
    val props = LinkedHashSet<String>()
    for (s in kf.steps) props.addAll(s.decls.keys)
    for (propRaw in props) {
        val prop = if (propRaw == "background") "background-color" else propRaw
        var prev: KeyframeStep? = null
        var next: KeyframeStep? = null
        for (s in kf.steps) {
            if (!s.decls.containsKey(propRaw)) continue
            if (s.at <= p) prev = s
            if (s.at >= p && next == null) next = s
        }
        val prevVal = prev?.decls?.get(propRaw) ?: base[prop] ?: defaultFor(prop)
        val nextVal = next?.decls?.get(propRaw) ?: base[prop] ?: defaultFor(prop)
        if (prevVal == null && nextVal == null) continue
        val a = prevVal ?: nextVal ?: continue
        val b = nextVal ?: prevVal ?: continue
        val prevAt = prev?.at ?: 0f
        val nextAt = next?.at ?: 1f
        val segT = if (nextAt > prevAt) (p - prevAt) / (nextAt - prevAt) else 1f
        out[prop] = lerpProp(prop, a, b, easeValue(timing, segT), ctx, current)
    }
    return out
}

internal fun evalAnimation(
    spec: AnimSpec,
    kf: Keyframes,
    elapsedMs: Float,
    base: Map<String, String>,
    ctx: LenCtx,
    current: Color,
): AnimResult {
    val local = elapsedMs - spec.delayMs
    val keepAfter = spec.fill == "forwards" || spec.fill == "both"
    if (local < 0f) {
        val before = if (spec.fill == "backwards" || spec.fill == "both") sampleAt(kf, 0f, spec.timing, base, ctx, current) else null
        return AnimResult(before, true)
    }
    if (spec.durMs <= 0f) {
        return AnimResult(if (keepAfter) sampleAt(kf, 1f, spec.timing, base, ctx, current) else null, false)
    }
    val total = local / spec.durMs
    var iter = Math.floor(total.toDouble()).toFloat()
    var frac = total - iter
    var finished = false
    if (total >= spec.iterations) {
        finished = true
        iter = Math.max(0f, Math.ceil(spec.iterations.toDouble()).toFloat() - 1f)
        frac = 1f
    }
    if (finished && !keepAfter) return AnimResult(null, false)
    val k = iter.toInt()
    val reverse = when (spec.direction) {
        "reverse" -> true
        "alternate" -> k % 2 == 1
        "alternate-reverse" -> k % 2 == 0
        else -> false
    }
    val p = if (reverse) 1f - frac else frac
    return AnimResult(sampleAt(kf, p, spec.timing, base, ctx, current), !finished)
}

// ---------- 单个元素的动画状态 ----------

internal class TransRun(val from: String, val to: String, val start: Long, val dur: Float, val timing: String)

internal class AnimHolder {
    var lastTarget: Map<String, String>? = null
    val shown = HashMap<String, String>()
    val running = HashMap<String, TransRun>()
    val starts = HashMap<String, Long>()
}

internal class AnimOut(val style: Map<String, String>, val active: Boolean)

internal fun resolveAnimated(
    holder: AnimHolder,
    cs: Map<String, String>,
    anims: List<AnimSpec>,
    trans: List<TransSpec>,
    keyframes: Map<String, Keyframes>,
    epoch: Int,
    nowMs: Long,
    ctx: LenCtx,
    current: Color,
): AnimOut {
    val out = HashMap<String, String>(cs)
    var active = false
    if (trans.isNotEmpty()) {
        val last = holder.lastTarget
        if (last != null) {
            for (spec in trans) {
                val p = spec.prop
                val newV = cs[p]
                val oldV = last[p]
                if (newV != null && oldV != null && newV != oldV && spec.durMs + spec.delayMs > 0f) {
                    val from = holder.shown[p] ?: oldV
                    holder.running[p] = TransRun(from, newV, nowMs + spec.delayMs.toLong(), spec.durMs, spec.timing)
                }
            }
        }
        holder.lastTarget = cs
        val it = holder.running.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val tr = e.value
            if (nowMs < tr.start) {
                out[e.key] = tr.from
                active = true
                continue
            }
            val t = if (tr.dur <= 0f) 1f else (nowMs - tr.start).toFloat() / tr.dur
            if (t >= 1f) {
                it.remove()
                holder.shown.remove(e.key)
            } else {
                val v = lerpProp(e.key, tr.from, tr.to, easeValue(tr.timing, t), ctx, current)
                out[e.key] = v
                holder.shown[e.key] = v
                active = true
            }
        }
    } else {
        holder.lastTarget = null
        holder.running.clear()
        holder.shown.clear()
    }
    for (spec in anims) {
        val kf = keyframes[spec.name] ?: continue
        val key = spec.name + "#" + epoch
        val start = holder.starts.getOrPut(key) { nowMs }
        val r = evalAnimation(spec, kf, (nowMs - start).toFloat(), cs, ctx, current)
        val vals = r.values
        if (vals != null) out.putAll(vals)
        if (r.running) active = true
    }
    return AnimOut(out, active)
}
