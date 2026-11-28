package com.island.phone.app.home.widget.custom

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

internal fun nf(name: String, f: (Any?, List<Any?>) -> Any?): NativeFn = NativeFn(name, null, f)

private fun Interp.fnArg(v: Any?, what: String): JsFunction {
    if (v is JsFunction) return v
    throw typeError("$what 需要传入一个函数")
}

private fun relIndex(v: Any?, len: Int, def: Int): Int {
    if (v === Undefined) return def
    val d = toNum(v)
    if (d.isNaN()) return 0
    if (d == Double.NEGATIVE_INFINITY) return 0
    if (d == Double.POSITIVE_INFINITY) return len
    val i = if (d < 0) Math.ceil(d) else Math.floor(d)
    return if (i < 0) maxOf(0, (len + i).toInt()) else minOf(i, len.toDouble()).toInt()
}

private fun sameValueZero(x: Any?, y: Any?): Boolean {
    if (x is Double && y is Double && x.isNaN() && y.isNaN()) return true
    return strictEq(x, y)
}

private fun mergeSort(src: List<Any?>, cmp: (Any?, Any?) -> Int): List<Any?> {
    if (src.size <= 1) return src
    val mid = src.size / 2
    val l = mergeSort(src.subList(0, mid), cmp)
    val r = mergeSort(src.subList(mid, src.size), cmp)
    val out = ArrayList<Any?>(src.size)
    var i = 0
    var j = 0
    while (i < l.size && j < r.size) {
        if (cmp(l[i], r[j]) <= 0) {
            out.add(l[i])
            i++
        } else {
            out.add(r[j])
            j++
        }
    }
    while (i < l.size) {
        out.add(l[i])
        i++
    }
    while (j < r.size) {
        out.add(r[j])
        j++
    }
    return out
}

private fun flatInto(out: ArrayList<Any?>, src: List<Any?>, depth: Int) {
    for (x in src) {
        if (x is JsArray && depth > 0) flatInto(out, x.list, depth - 1) else out.add(x)
    }
}

// ---------- 字符串 ----------

private fun replaceStr(it: Interp, s: String, pat: String, rep: Any?, all: Boolean): String {
    val sb = StringBuilder()
    var from = 0
    var found = false
    while (true) {
        val at = if (pat.isEmpty()) (if (found) -1 else 0) else s.indexOf(pat, from)
        if (at < 0) break
        found = true
        sb.append(s, from, at)
        if (rep is JsFunction) {
            sb.append(toStr(it.callValue(rep, Undefined, listOf(pat, at.toDouble(), s))))
        } else {
            sb.append(toStr(rep).replace("\$&", pat))
        }
        from = at + pat.length
        if (!all) break
    }
    sb.append(s, from, s.length)
    return sb.toString()
}

internal fun Interp.stringMember(s: String, key: Any?): Any? {
    if (key is Double) {
        val i = key.toInt()
        return if (key == i.toDouble() && i >= 0 && i < s.length) s[i].toString() else Undefined
    }
    val name = toStr(key)
    val idx = name.toIntOrNull()
    if (idx != null) return if (idx >= 0 && idx < s.length) s[idx].toString() else Undefined
    return when (name) {
        "length" -> s.length.toDouble()
        "charAt" -> nf(name) { _, a ->
            val i = toInt32(toNum(a.arg(0)))
            if (i >= 0 && i < s.length) s[i].toString() else ""
        }
        "charCodeAt", "codePointAt" -> nf(name) { _, a ->
            val i = toInt32(toNum(a.arg(0)))
            if (i >= 0 && i < s.length) s[i].code.toDouble() else Double.NaN
        }
        "at" -> nf(name) { _, a ->
            var i = toInt32(toNum(a.arg(0)))
            if (i < 0) i += s.length
            if (i >= 0 && i < s.length) s[i].toString() else Undefined
        }
        "indexOf" -> nf(name) { _, a ->
            val from = relIndex(a.arg(1), s.length, 0)
            s.indexOf(toStr(a.arg(0)), from).toDouble()
        }
        "lastIndexOf" -> nf(name) { _, a -> s.lastIndexOf(toStr(a.arg(0))).toDouble() }
        "includes" -> nf(name) { _, a -> s.indexOf(toStr(a.arg(0)), relIndex(a.arg(1), s.length, 0)) >= 0 }
        "startsWith" -> nf(name) { _, a -> s.startsWith(toStr(a.arg(0)), relIndex(a.arg(1), s.length, 0)) }
        "endsWith" -> nf(name) { _, a ->
            val end = relIndex(a.arg(1), s.length, s.length)
            s.substring(0, end).endsWith(toStr(a.arg(0)))
        }
        "slice" -> nf(name) { _, a ->
            val st = relIndex(a.arg(0), s.length, 0)
            val en = relIndex(a.arg(1), s.length, s.length)
            if (st < en) s.substring(st, en) else ""
        }
        "substring" -> nf(name) { _, a ->
            fun clamp(v: Any?, def: Int): Int {
                if (v === Undefined) return def
                val d = toNum(v)
                if (d.isNaN() || d < 0) return 0
                return if (d > s.length) s.length else d.toInt()
            }
            val x = clamp(a.arg(0), 0)
            val y = clamp(a.arg(1), s.length)
            s.substring(minOf(x, y), maxOf(x, y))
        }
        "substr" -> nf(name) { _, a ->
            val st = relIndex(a.arg(0), s.length, 0)
            val len = if (a.arg(1) === Undefined) s.length - st else maxOf(0, minOf(toInt32(toNum(a.arg(1))), s.length - st))
            s.substring(st, st + len)
        }
        "toUpperCase", "toLocaleUpperCase" -> nf(name) { _, _ -> s.uppercase() }
        "toLowerCase", "toLocaleLowerCase" -> nf(name) { _, _ -> s.lowercase() }
        "trim" -> nf(name) { _, _ -> s.trim() }
        "trimStart" -> nf(name) { _, _ -> s.trimStart() }
        "trimEnd" -> nf(name) { _, _ -> s.trimEnd() }
        "split" -> nf(name) { _, a ->
            val sep = a.arg(0)
            val lim = if (a.arg(1) === Undefined) Int.MAX_VALUE else toInt32(toNum(a.arg(1)))
            val parts: List<String> = if (sep === Undefined) {
                listOf(s)
            } else {
                val sp = toStr(sep)
                if (sp.isEmpty()) s.map { it.toString() } else s.split(sp)
            }
            JsArray(ArrayList<Any?>(parts.take(maxOf(0, lim))))
        }
        "replace" -> nf(name) { _, a -> replaceStr(this, s, toStr(a.arg(0)), a.arg(1), false) }
        "replaceAll" -> nf(name) { _, a -> replaceStr(this, s, toStr(a.arg(0)), a.arg(1), true) }
        "repeat" -> nf(name) { _, a ->
            val n = toInt32(toNum(a.arg(0)))
            if (n < 0 || n.toLong() * s.length > 1_000_000L) throw JsThrow(JsError("RangeError", "重复次数不对"))
            s.repeat(n)
        }
        "padStart" -> nf(name) { _, a ->
            val n = toInt32(toNum(a.arg(0)))
            val fill = if (a.arg(1) === Undefined) " " else toStr(a.arg(1))
            if (n <= s.length || fill.isEmpty() || n > 100_000) {
                s
            } else {
                val need = n - s.length
                fill.repeat(need / fill.length + 1).substring(0, need) + s
            }
        }
        "padEnd" -> nf(name) { _, a ->
            val n = toInt32(toNum(a.arg(0)))
            val fill = if (a.arg(1) === Undefined) " " else toStr(a.arg(1))
            if (n <= s.length || fill.isEmpty() || n > 100_000) {
                s
            } else {
                val need = n - s.length
                s + fill.repeat(need / fill.length + 1).substring(0, need)
            }
        }
        "concat" -> nf(name) { _, a -> s + a.joinToString("") { toStr(it) } }
        "localeCompare" -> nf(name) { _, a ->
            val c = s.compareTo(toStr(a.arg(0)))
            if (c < 0) -1.0 else if (c > 0) 1.0 else 0.0
        }
        "normalize", "toString", "valueOf" -> nf(name) { _, _ -> s }
        else -> Undefined
    }
}

// ---------- 数组 ----------

internal fun Interp.arrayMember(a: JsArray, key: Any?): Any? {
    val list = a.list
    if (key is Double) {
        val i = key.toInt()
        return if (key == i.toDouble() && i >= 0 && i < list.size) list[i] else Undefined
    }
    val name = toStr(key)
    val idx = name.toIntOrNull()
    if (idx != null) return if (idx >= 0 && idx < list.size) list[idx] else Undefined
    return when (name) {
        "length" -> list.size.toDouble()
        "push" -> nf(name) { _, args ->
            list.addAll(args)
            list.size.toDouble()
        }
        "pop" -> nf(name) { _, _ -> if (list.isEmpty()) Undefined else list.removeAt(list.size - 1) }
        "shift" -> nf(name) { _, _ -> if (list.isEmpty()) Undefined else list.removeAt(0) }
        "unshift" -> nf(name) { _, args ->
            list.addAll(0, args)
            list.size.toDouble()
        }
        "slice" -> nf(name) { _, args ->
            val n = list.size
            val st = relIndex(args.arg(0), n, 0)
            val en = relIndex(args.arg(1), n, n)
            JsArray(if (st < en) ArrayList<Any?>(list.subList(st, en)) else ArrayList<Any?>())
        }
        "splice" -> nf(name) { _, args ->
            val n = list.size
            val st = relIndex(args.arg(0), n, 0)
            val dc = if (args.isEmpty()) 0 else if (args.size == 1) n - st else maxOf(0, minOf(toInt32(toNum(args[1])), n - st))
            val removed = ArrayList<Any?>(list.subList(st, st + dc))
            for (k in 0 until dc) list.removeAt(st)
            if (args.size > 2) list.addAll(st, args.drop(2))
            JsArray(removed)
        }
        "concat" -> nf(name) { _, args ->
            val out = ArrayList<Any?>(list)
            for (x in args) {
                if (x is JsArray) out.addAll(x.list) else out.add(x)
            }
            JsArray(out)
        }
        "join" -> nf(name) { _, args ->
            val sep = if (args.arg(0) === Undefined) "," else toStr(args.arg(0))
            list.joinToString(sep) { if (isNullish(it)) "" else toStr(it) }
        }
        "toString" -> nf(name) { _, _ -> a.jsString() }
        "reverse" -> nf(name) { _, _ ->
            list.reverse()
            a
        }
        "indexOf" -> nf(name) { _, args ->
            var r = -1.0
            for (i in list.indices) {
                if (strictEq(list[i], args.arg(0))) {
                    r = i.toDouble()
                    break
                }
            }
            r
        }
        "lastIndexOf" -> nf(name) { _, args ->
            var r = -1.0
            for (i in list.indices.reversed()) {
                if (strictEq(list[i], args.arg(0))) {
                    r = i.toDouble()
                    break
                }
            }
            r
        }
        "includes" -> nf(name) { _, args -> list.any { sameValueZero(it, args.arg(0)) } }
        "at" -> nf(name) { _, args ->
            var i = toInt32(toNum(args.arg(0)))
            if (i < 0) i += list.size
            if (i >= 0 && i < list.size) list[i] else Undefined
        }
        "find", "findIndex", "findLast", "findLastIndex" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            val last = name.startsWith("findLast")
            val wantIndex = name.endsWith("Index")
            var res: Any? = if (wantIndex) -1.0 else Undefined
            val n = list.size
            for (k in 0 until n) {
                val i = if (last) n - 1 - k else k
                if (i >= list.size) continue
                val item = list[i]
                if (truthy(callValue(f, Undefined, listOf(item, i.toDouble(), a)))) {
                    res = if (wantIndex) i.toDouble() else item
                    break
                }
            }
            res
        }
        "filter" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            val out = ArrayList<Any?>()
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                val item = list[i]
                if (truthy(callValue(f, Undefined, listOf(item, i.toDouble(), a)))) out.add(item)
            }
            JsArray(out)
        }
        "map" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            val out = ArrayList<Any?>()
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                out.add(callValue(f, Undefined, listOf(list[i], i.toDouble(), a)))
            }
            JsArray(out)
        }
        "flatMap" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            val out = ArrayList<Any?>()
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                val r = callValue(f, Undefined, listOf(list[i], i.toDouble(), a))
                if (r is JsArray) out.addAll(r.list) else out.add(r)
            }
            JsArray(out)
        }
        "forEach" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                callValue(f, Undefined, listOf(list[i], i.toDouble(), a))
            }
            Undefined
        }
        "some" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            var r = false
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                if (truthy(callValue(f, Undefined, listOf(list[i], i.toDouble(), a)))) {
                    r = true
                    break
                }
            }
            r
        }
        "every" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            var r = true
            val n = list.size
            for (i in 0 until n) {
                if (i >= list.size) break
                if (!truthy(callValue(f, Undefined, listOf(list[i], i.toDouble(), a)))) {
                    r = false
                    break
                }
            }
            r
        }
        "reduce" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            var i = 0
            var acc: Any? = Undefined
            if (args.size >= 2) {
                acc = args[1]
            } else {
                if (list.isEmpty()) throw typeError("空数组做 reduce 时需要给初始值")
                acc = list[0]
                i = 1
            }
            while (i < list.size) {
                acc = callValue(f, Undefined, listOf(acc, list[i], i.toDouble(), a))
                i++
            }
            acc
        }
        "reduceRight" -> nf(name) { _, args ->
            val f = fnArg(args.arg(0), name)
            var i = list.size - 1
            var acc: Any? = Undefined
            if (args.size >= 2) {
                acc = args[1]
            } else {
                if (list.isEmpty()) throw typeError("空数组做 reduceRight 时需要给初始值")
                acc = list[i]
                i--
            }
            while (i >= 0) {
                if (i < list.size) acc = callValue(f, Undefined, listOf(acc, list[i], i.toDouble(), a))
                i--
            }
            acc
        }
        "flat" -> nf(name) { _, args ->
            val depth = if (args.arg(0) === Undefined) 1 else toInt32(toNum(args.arg(0)))
            val out = ArrayList<Any?>()
            flatInto(out, list, depth)
            JsArray(out)
        }
        "fill" -> nf(name) { _, args ->
            val n = list.size
            val st = relIndex(args.arg(1), n, 0)
            val en = relIndex(args.arg(2), n, n)
            for (i in st until en) list[i] = args.arg(0)
            a
        }
        "keys" -> nf(name) { _, _ -> JsArray(ArrayList<Any?>(list.indices.map { it.toDouble() })) }
        "values" -> nf(name) { _, _ -> JsArray(ArrayList<Any?>(list)) }
        "entries" -> nf(name) { _, _ ->
            JsArray(ArrayList<Any?>(list.indices.map { JsArray(arrayListOf<Any?>(it.toDouble(), list[it])) }))
        }
        "sort" -> nf(name) { _, args ->
            val cmp = args.arg(0)
            val vals = list.filter { it !== Undefined }
            val undef = list.size - vals.size
            val sorted = mergeSort(vals) { x, y ->
                if (cmp is JsFunction) {
                    val r = toNum(callValue(cmp, Undefined, listOf(x, y)))
                    if (r.isNaN()) 0 else if (r < 0) -1 else if (r > 0) 1 else 0
                } else {
                    toStr(x).compareTo(toStr(y))
                }
            }
            list.clear()
            list.addAll(sorted)
            for (k in 0 until undef) list.add(Undefined)
            a
        }
        else -> a.getProp(name)
    }
}

// ---------- 数字 ----------

internal fun numLocale(d: Double): String {
    if (d.isNaN() || d.isInfinite()) return numStr(d)
    val s = BigDecimal(d).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    val neg = s.startsWith("-")
    val body = if (neg) s.substring(1) else s
    val parts = body.split(".")
    val ip = parts[0]
    val sb = StringBuilder()
    for ((i, ch) in ip.withIndex()) {
        if (i > 0 && (ip.length - i) % 3 == 0) sb.append(',')
        sb.append(ch)
    }
    return (if (neg) "-" else "") + sb.toString() + (if (parts.size > 1) "." + parts[1] else "")
}

internal fun Interp.numberMember(d: Double, name: String): Any? = when (name) {
    "toFixed" -> nf(name) { _, a ->
        val n = toInt32(toNum(a.arg(0))).coerceIn(0, 100)
        if (d.isNaN() || d.isInfinite()) numStr(d) else BigDecimal(d).setScale(n, RoundingMode.HALF_UP).toPlainString()
    }
    "toPrecision" -> nf(name) { _, a ->
        if (a.arg(0) === Undefined || d.isNaN() || d.isInfinite() || d == 0.0) {
            numStr(d)
        } else {
            val p = toInt32(toNum(a.arg(0))).coerceIn(1, 100)
            BigDecimal(d).round(MathContext(p, RoundingMode.HALF_UP)).toPlainString()
        }
    }
    "toString" -> nf(name) { _, a ->
        val radix = if (a.arg(0) === Undefined) 10 else toInt32(toNum(a.arg(0)))
        if (radix == 10 || radix < 2 || radix > 36 || d != Math.floor(d) || d.isInfinite()) {
            numStr(d)
        } else {
            java.lang.Long.toString(d.toLong(), radix)
        }
    }
    "toLocaleString" -> nf(name) { _, _ -> numLocale(d) }
    "valueOf" -> nf(name) { _, _ -> d }
    else -> Undefined
}

// ---------- 函数 / Promise / 普通对象 ----------

internal fun Interp.functionMember(f: JsFunction, name: String): Any? = when (name) {
    "call" -> nf(name) { _, a -> callValue(f, a.arg(0), if (a.size > 1) a.drop(1) else emptyList()) }
    "apply" -> nf(name) { _, a ->
        val arr = a.arg(1)
        callValue(f, a.arg(0), if (arr is JsArray) arr.list.toList() else emptyList())
    }
    "bind" -> nf(name) { _, a ->
        val bound = a.arg(0)
        val pre: List<Any?> = if (a.size > 1) a.drop(1) else emptyList()
        nf(f.fname) { _, b -> callValue(f, bound, pre + b) }
    }
    "name" -> f.fname
    "length" -> 0.0
    "toString" -> nf(name) { _, _ -> toStr(f) }
    else -> Undefined
}

private fun Interp.settle(p: JsPromise, onOk: Any?, onErr: Any?): Any? {
    val h = if (p.rejected) onErr else onOk
    if (h !is JsFunction) return p
    return try {
        val r = callValue(h, Undefined, listOf(p.value))
        if (r is JsPromise) r else JsPromise(r)
    } catch (t: JsThrow) {
        JsPromise(t.value, true)
    }
}

internal fun Interp.promiseMember(p: JsPromise, name: String): Any? = when (name) {
    "then" -> nf(name) { _, a -> settle(p, a.arg(0), a.arg(1)) }
    "catch" -> nf(name) { _, a -> settle(p, Undefined, a.arg(0)) }
    "finally" -> nf(name) { _, a ->
        val f = a.arg(0)
        if (f is JsFunction) callValue(f, Undefined, emptyList())
        p
    }
    else -> Undefined
}

internal fun Interp.objectMember(o: JsObject, name: String): Any? = when (name) {
    "hasOwnProperty" -> nf(name) { _, a -> o.hasProp(toStr(a.arg(0))) }
    "toString" -> nf(name) { _, _ -> toStr(o) }
    "valueOf" -> nf(name) { _, _ -> o }
    else -> Undefined
}

// ---------- 日期 ----------

private val DayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MonthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val WeekCn = arrayOf("日", "一", "二", "三", "四", "五", "六")

private fun cal(ms: Double, utc: Boolean = false): Calendar {
    val c = if (utc) GregorianCalendar(TimeZone.getTimeZone("UTC")) else GregorianCalendar()
    c.timeInMillis = ms.toLong()
    return c
}

private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

private fun tzText(ms: Double): String {
    val off = TimeZone.getDefault().getOffset(ms.toLong()) / 60000
    val sign = if (off < 0) "-" else "+"
    val a = Math.abs(off)
    return "GMT" + sign + two(a / 60) + two(a % 60)
}

internal fun dateToString(ms: Double): String {
    if (ms.isNaN()) return "Invalid Date"
    val c = cal(ms)
    return DayNames[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + MonthNames[c.get(Calendar.MONTH)] + " " +
        two(c.get(Calendar.DAY_OF_MONTH)) + " " + c.get(Calendar.YEAR) + " " +
        two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE)) + ":" + two(c.get(Calendar.SECOND)) + " " + tzText(ms)
}

internal fun dateToIso(ms: Double): String {
    if (ms.isNaN()) throw JsThrow(JsError("RangeError", "无效的日期"))
    val c = cal(ms, true)
    val milli = c.get(Calendar.MILLISECOND)
    val mm = if (milli < 10) "00$milli" else if (milli < 100) "0$milli" else milli.toString()
    return c.get(Calendar.YEAR).toString() + "-" + two(c.get(Calendar.MONTH) + 1) + "-" + two(c.get(Calendar.DAY_OF_MONTH)) +
        "T" + two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE)) + ":" + two(c.get(Calendar.SECOND)) + "." + mm + "Z"
}

private val DateRe = Regex(
    "^(\\d{4})[-/](\\d{1,2})(?:[-/](\\d{1,2}))?(?:[T\\s]+(\\d{1,2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,9}))?)?)?\\s*(Z|[+-]\\d{2}:?\\d{2})?$",
    RegexOption.IGNORE_CASE,
)

internal fun parseDateStr(raw: String): Double {
    val m = DateRe.find(raw.trim()) ?: return Double.NaN
    val g = m.groupValues
    val y = g[1].toInt()
    val mo = g[2].toInt() - 1
    val d = if (g[3].isEmpty()) 1 else g[3].toInt()
    val h = if (g[4].isEmpty()) 0 else g[4].toInt()
    val mi = if (g[5].isEmpty()) 0 else g[5].toInt()
    val s = if (g[6].isEmpty()) 0 else g[6].toInt()
    val ms = if (g[7].isEmpty()) 0 else g[7].padEnd(3, '0').substring(0, 3).toInt()
    if (mo < 0 || mo > 11 || d < 1 || d > 31 || h > 24 || mi > 59 || s > 59) return Double.NaN
    val tz = g[8]
    val c = if (tz.isEmpty()) GregorianCalendar() else GregorianCalendar(TimeZone.getTimeZone("UTC"))
    c.clear()
    c.set(y, mo, d, h, mi, s)
    c.set(Calendar.MILLISECOND, ms)
    var t = c.timeInMillis.toDouble()
    if (tz.isNotEmpty() && !tz.equals("Z", true)) {
        val sign = if (tz[0] == '-') -1.0 else 1.0
        val hh = tz.substring(1, 3).toInt()
        val mm = tz.takeLast(2).toInt()
        t -= sign * (hh * 60 + mm) * 60000.0
    }
    return t
}

internal fun makeDateMs(a: List<Any?>): Double {
    if (a.isEmpty()) return System.currentTimeMillis().toDouble()
    if (a.size == 1) {
        val v = a[0]
        if (v is JsDate) return v.ms
        if (v is String) return parseDateStr(v)
        val d = toNum(v)
        return if (d.isNaN() || d.isInfinite()) Double.NaN else if (d < 0) Math.ceil(d) else Math.floor(d)
    }
    val nums = a.map { toNum(it) }
    if (nums.any { it.isNaN() || it.isInfinite() }) return Double.NaN
    var y = nums[0].toInt()
    if (y in 0..99) y += 1900
    val c = GregorianCalendar()
    c.clear()
    c.set(y, nums[1].toInt(), if (nums.size > 2) nums[2].toInt() else 1,
        if (nums.size > 3) nums[3].toInt() else 0, if (nums.size > 4) nums[4].toInt() else 0, if (nums.size > 5) nums[5].toInt() else 0)
    c.set(Calendar.MILLISECOND, if (nums.size > 6) nums[6].toInt() else 0)
    return c.timeInMillis.toDouble()
}

private fun optStr(o: Any?, k: String): String? {
    if (o !is JsObject) return null
    val v = o.getProp(k)
    return if (v === Undefined) null else toStr(v)
}

internal fun fmtLocaleDate(ms: Double, opts: Any?): String {
    if (ms.isNaN()) return "Invalid Date"
    val c = cal(ms)
    val y = c.get(Calendar.YEAR)
    val mo = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    val wd = c.get(Calendar.DAY_OF_WEEK) - 1
    val oy = optStr(opts, "year")
    val om = optStr(opts, "month")
    val od = optStr(opts, "day")
    val ow = optStr(opts, "weekday")
    if (oy == null && om == null && od == null && ow == null) return "$y/$mo/$d"
    val sb = StringBuilder()
    if (oy != null) sb.append(y).append('年')
    if (om != null) sb.append(if (om == "2-digit") two(mo) else mo.toString()).append('月')
    if (od != null) sb.append(if (od == "2-digit") two(d) else d.toString()).append('日')
    if (ow != null) {
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append(if (ow == "long") "星期" else "周").append(WeekCn[wd])
    }
    return sb.toString()
}

internal fun fmtLocaleTime(ms: Double, opts: Any?): String {
    if (ms.isNaN()) return "Invalid Date"
    val c = cal(ms)
    val h = c.get(Calendar.HOUR_OF_DAY)
    val mi = c.get(Calendar.MINUTE)
    val s = c.get(Calendar.SECOND)
    val oh = optStr(opts, "hour")
    val omi = optStr(opts, "minute")
    val os = optStr(opts, "second")
    val h12 = optStr(opts, "hour12") == "true"
    val any = oh != null || omi != null || os != null
    val showH = !any || oh != null
    val showM = !any || omi != null
    val showS = !any || os != null
    var prefix = ""
    var hh = h
    if (h12) {
        prefix = if (h < 12) "上午" else "下午"
        hh = h % 12
        if (hh == 0) hh = 12
    }
    val parts = ArrayList<String>()
    if (showH) parts.add(if (h12) hh.toString() else two(hh))
    if (showM) parts.add(two(mi))
    if (showS) parts.add(two(s))
    return prefix + parts.joinToString(":")
}

internal fun fmtLocaleBoth(ms: Double, opts: Any?): String {
    val hasDate = listOf("year", "month", "day", "weekday").any { optStr(opts, it) != null }
    val hasTime = listOf("hour", "minute", "second").any { optStr(opts, it) != null }
    if (hasDate && !hasTime) return fmtLocaleDate(ms, opts)
    if (hasTime && !hasDate) return fmtLocaleTime(ms, opts)
    return fmtLocaleDate(ms, opts) + " " + fmtLocaleTime(ms, opts)
}

internal fun Interp.dateMember(d: JsDate, name: String): Any? {
    fun getter(field: Int, add: Int, utc: Boolean): Any? = nf(name) { _, _ ->
        if (d.ms.isNaN()) Double.NaN else (cal(d.ms, utc).get(field) + add).toDouble()
    }
    fun setter(apply: (Calendar, List<Any?>) -> Unit): Any? = nf(name) { _, args ->
        if (d.ms.isNaN()) {
            d.ms
        } else {
            val c = cal(d.ms)
            apply(c, args)
            d.ms = c.timeInMillis.toDouble()
            d.ms
        }
    }
    fun n(args: List<Any?>, i: Int): Int = toInt32(toNum(args.arg(i)))
    return when (name) {
        "getTime", "valueOf" -> nf(name) { _, _ -> d.ms }
        "getFullYear" -> getter(Calendar.YEAR, 0, false)
        "getMonth" -> getter(Calendar.MONTH, 0, false)
        "getDate" -> getter(Calendar.DAY_OF_MONTH, 0, false)
        "getDay" -> getter(Calendar.DAY_OF_WEEK, -1, false)
        "getHours" -> getter(Calendar.HOUR_OF_DAY, 0, false)
        "getMinutes" -> getter(Calendar.MINUTE, 0, false)
        "getSeconds" -> getter(Calendar.SECOND, 0, false)
        "getMilliseconds" -> getter(Calendar.MILLISECOND, 0, false)
        "getUTCFullYear" -> getter(Calendar.YEAR, 0, true)
        "getUTCMonth" -> getter(Calendar.MONTH, 0, true)
        "getUTCDate" -> getter(Calendar.DAY_OF_MONTH, 0, true)
        "getUTCDay" -> getter(Calendar.DAY_OF_WEEK, -1, true)
        "getUTCHours" -> getter(Calendar.HOUR_OF_DAY, 0, true)
        "getUTCMinutes" -> getter(Calendar.MINUTE, 0, true)
        "getUTCSeconds" -> getter(Calendar.SECOND, 0, true)
        "getTimezoneOffset" -> nf(name) { _, _ -> (-(TimeZone.getDefault().getOffset(d.ms.toLong()) / 60000)).toDouble() }
        "setTime" -> nf(name) { _, args ->
            d.ms = toNum(args.arg(0))
            d.ms
        }
        "setFullYear" -> setter { c, a ->
            c.set(Calendar.YEAR, n(a, 0))
            if (a.size > 1) c.set(Calendar.MONTH, n(a, 1))
            if (a.size > 2) c.set(Calendar.DAY_OF_MONTH, n(a, 2))
        }
        "setMonth" -> setter { c, a ->
            c.set(Calendar.MONTH, n(a, 0))
            if (a.size > 1) c.set(Calendar.DAY_OF_MONTH, n(a, 1))
        }
        "setDate" -> setter { c, a -> c.set(Calendar.DAY_OF_MONTH, n(a, 0)) }
        "setHours" -> setter { c, a ->
            c.set(Calendar.HOUR_OF_DAY, n(a, 0))
            if (a.size > 1) c.set(Calendar.MINUTE, n(a, 1))
            if (a.size > 2) c.set(Calendar.SECOND, n(a, 2))
            if (a.size > 3) c.set(Calendar.MILLISECOND, n(a, 3))
        }
        "setMinutes" -> setter { c, a ->
            c.set(Calendar.MINUTE, n(a, 0))
            if (a.size > 1) c.set(Calendar.SECOND, n(a, 1))
            if (a.size > 2) c.set(Calendar.MILLISECOND, n(a, 2))
        }
        "setSeconds" -> setter { c, a ->
            c.set(Calendar.SECOND, n(a, 0))
            if (a.size > 1) c.set(Calendar.MILLISECOND, n(a, 1))
        }
        "setMilliseconds" -> setter { c, a -> c.set(Calendar.MILLISECOND, n(a, 0)) }
        "toISOString" -> nf(name) { _, _ -> dateToIso(d.ms) }
        "toJSON" -> nf(name) { _, _ -> if (d.ms.isNaN()) null else dateToIso(d.ms) }
        "toString" -> nf(name) { _, _ -> dateToString(d.ms) }
        "toDateString" -> nf(name) { _, _ ->
            if (d.ms.isNaN()) {
                "Invalid Date"
            } else {
                val c = cal(d.ms)
                DayNames[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + MonthNames[c.get(Calendar.MONTH)] + " " +
                    two(c.get(Calendar.DAY_OF_MONTH)) + " " + c.get(Calendar.YEAR)
            }
        }
        "toTimeString" -> nf(name) { _, _ -> fmtLocaleTime(d.ms, Undefined) + " " + tzText(d.ms) }
        "toLocaleDateString" -> nf(name) { _, a -> fmtLocaleDate(d.ms, a.arg(1)) }
        "toLocaleTimeString" -> nf(name) { _, a -> fmtLocaleTime(d.ms, a.arg(1)) }
        "toLocaleString" -> nf(name) { _, a -> fmtLocaleBoth(d.ms, a.arg(1)) }
        else -> Undefined
    }
}
