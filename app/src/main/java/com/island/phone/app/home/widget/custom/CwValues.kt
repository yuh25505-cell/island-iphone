package com.island.phone.app.home.widget.custom

import java.math.BigDecimal

internal object Undefined {
    override fun toString(): String = "undefined"
}

internal open class JsObject {
    val props = LinkedHashMap<String, Any?>()
    open fun getProp(name: String): Any? = if (props.containsKey(name)) props[name] else Undefined
    open fun setProp(name: String, value: Any?) {
        props[name] = value
    }
    open fun hasProp(name: String): Boolean = props.containsKey(name)
    open fun deleteProp(name: String) {
        props.remove(name)
    }
    open fun ownKeys(): List<String> = props.keys.toList()
    open fun jsString(): String = "[object Object]"
}

internal class JsArray(val list: MutableList<Any?> = ArrayList()) : JsObject() {
    override fun jsString(): String = list.joinToString(",") { if (it == null || it === Undefined) "" else toStr(it) }
    override fun ownKeys(): List<String> = list.indices.map { it.toString() }
    override fun hasProp(name: String): Boolean {
        val i = name.toIntOrNull()
        return if (i != null) i in list.indices else super.hasProp(name)
    }
}

internal class JsError(val errName: String, val msg: String) : JsObject() {
    init {
        props["name"] = errName
        props["message"] = msg
    }
    override fun jsString(): String = if (msg.isEmpty()) errName else "$errName: $msg"
}

internal class JsDate(var ms: Double) : JsObject() {
    override fun jsString(): String = dateToString(ms)
}

internal class JsPromise(val value: Any?, val rejected: Boolean = false) : JsObject()

internal abstract class JsFunction : JsObject() {
    open val fname: String get() = ""
    abstract fun call(thisVal: Any?, args: List<Any?>): Any?
}

internal class NativeFn(
    val name: String,
    val ctor: ((List<Any?>) -> Any?)? = null,
    val fn: (Any?, List<Any?>) -> Any?,
) : JsFunction() {
    override val fname: String get() = name
    override fun call(thisVal: Any?, args: List<Any?>): Any? = fn(thisVal, args)
}

internal class JsThrow(val value: Any?) : RuntimeException(null, null, false, false) {
    override val message: String get() = errorText(value)
}

internal class BudgetEx : RuntimeException("脚本运行太久，已经停止", null, false, false)

internal fun errorText(v: Any?): String {
    if (v is JsObject && v.hasProp("message")) {
        val n = if (v.hasProp("name")) toStr(v.getProp("name")) else "Error"
        val m = toStr(v.getProp("message"))
        return if (m.isEmpty()) n else "$n: $m"
    }
    return toStr(v)
}

internal fun List<Any?>.arg(i: Int): Any? = if (i < size) this[i] else Undefined

internal fun isNullish(v: Any?): Boolean = v == null || v === Undefined

internal fun truthy(v: Any?): Boolean = when (v) {
    null -> false
    Undefined -> false
    is Boolean -> v
    is Double -> !(v == 0.0 || v.isNaN())
    is String -> v.isNotEmpty()
    else -> true
}

internal fun numStr(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d > 0) "Infinity" else "-Infinity"
    if (d == Math.floor(d) && Math.abs(d) < 1e15) return d.toLong().toString()
    val s = d.toString()
    if (!s.contains('E')) return s
    return BigDecimal(s).stripTrailingZeros().toPlainString()
}

private val NumberPattern = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$")

internal fun strToNum(s: String): Double {
    val t = s.trim()
    if (t.isEmpty()) return 0.0
    if (t == "Infinity" || t == "+Infinity") return Double.POSITIVE_INFINITY
    if (t == "-Infinity") return Double.NEGATIVE_INFINITY
    if (t.startsWith("0x") || t.startsWith("0X")) {
        val v = t.substring(2).toLongOrNull(16)
        return v?.toDouble() ?: Double.NaN
    }
    if (!NumberPattern.matches(t)) return Double.NaN
    return t.toDoubleOrNull() ?: Double.NaN
}

internal fun toNum(v: Any?): Double = when (v) {
    null -> 0.0
    Undefined -> Double.NaN
    is Boolean -> if (v) 1.0 else 0.0
    is Double -> v
    is String -> strToNum(v)
    is JsDate -> v.ms
    is JsArray -> if (v.list.isEmpty()) 0.0 else if (v.list.size == 1) toNum(v.list[0]) else Double.NaN
    else -> Double.NaN
}

internal fun toInt32(d: Double): Int = if (d.isNaN() || d.isInfinite()) 0 else d.toLong().toInt()

internal fun toStr(v: Any?): String = when (v) {
    null -> "null"
    Undefined -> "undefined"
    is Boolean -> if (v) "true" else "false"
    is Double -> numStr(v)
    is String -> v
    is JsFunction -> "function " + v.fname + "() { [native code] }"
    is JsObject -> v.jsString()
    else -> v.toString()
}

internal fun typeOf(v: Any?): String = when (v) {
    null -> "object"
    Undefined -> "undefined"
    is Boolean -> "boolean"
    is Double -> "number"
    is String -> "string"
    is JsFunction -> "function"
    else -> "object"
}

internal fun strictEq(a: Any?, b: Any?): Boolean {
    if (a is Double && b is Double) return a == b
    if (a is String && b is String) return a == b
    if (a is Boolean && b is Boolean) return a == b
    if (a == null && b == null) return true
    if (a === Undefined && b === Undefined) return true
    return a === b
}

internal fun looseEq(a: Any?, b: Any?): Boolean {
    if (isNullish(a) && isNullish(b)) return true
    if (isNullish(a) || isNullish(b)) return false
    if (a is JsObject && b is JsObject) return a === b
    if (a is Double && b is Double) return a == b
    if (a is String && b is String) return a == b
    if (a is Boolean && b is Boolean) return a == b
    if (a is Boolean) return looseEq(toNum(a), b)
    if (b is Boolean) return looseEq(a, toNum(b))
    if (a is Double && b is String) return a == strToNum(b)
    if (a is String && b is Double) return strToNum(a) == b
    if (a is JsObject) return looseEq(a.jsString(), b)
    if (b is JsObject) return looseEq(a, b.jsString())
    return false
}

internal fun jsonQuote(s: String): String {
    val sb = StringBuilder("\"")
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000c' -> sb.append("\\f")
            else -> if (c.code < 0x20) sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}

internal fun jsonStringify(v: Any?, indent: String = "", cur: String = ""): String? {
    when (v) {
        null -> return "null"
        Undefined -> return null
        is Boolean -> return v.toString()
        is Double -> return if (v.isNaN() || v.isInfinite()) "null" else numStr(v)
        is String -> return jsonQuote(v)
        is JsFunction -> return null
        is JsArray -> {
            if (v.list.isEmpty()) return "[]"
            val inner = cur + indent
            val parts = v.list.map { jsonStringify(it, indent, inner) ?: "null" }
            return if (indent.isEmpty()) {
                "[" + parts.joinToString(",") + "]"
            } else {
                "[\n" + parts.joinToString(",\n") { inner + it } + "\n" + cur + "]"
            }
        }
        is JsDate -> return jsonQuote(dateToIso(v.ms))
        is JsObject -> {
            val parts = ArrayList<String>()
            val inner = cur + indent
            for (k in v.ownKeys()) {
                val s = jsonStringify(v.getProp(k), indent, inner) ?: continue
                parts.add(jsonQuote(k) + (if (indent.isEmpty()) ":" else ": ") + s)
            }
            if (parts.isEmpty()) return "{}"
            return if (indent.isEmpty()) {
                "{" + parts.joinToString(",") + "}"
            } else {
                "{\n" + parts.joinToString(",\n") { inner + it } + "\n" + cur + "}"
            }
        }
    }
    return null
}

internal class JsonReader(private val s: String) {
    private var i = 0

    private fun fail(): Nothing = throw JsThrow(JsError("SyntaxError", "JSON 格式不对"))

    private fun skip() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    fun readAll(): Any? {
        val v = value()
        skip()
        if (i < s.length) fail()
        return v
    }

    private fun value(): Any? {
        skip()
        if (i >= s.length) fail()
        val c = s[i]
        if (c == '{') return obj()
        if (c == '[') return arr()
        if (c == '"') return str()
        if (s.startsWith("true", i)) {
            i += 4
            return true
        }
        if (s.startsWith("false", i)) {
            i += 5
            return false
        }
        if (s.startsWith("null", i)) {
            i += 4
            return null
        }
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '-' || s[i] == '+' || s[i] == '.' || s[i] == 'e' || s[i] == 'E')) i++
        return s.substring(start, i).toDoubleOrNull() ?: fail()
    }

    private fun obj(): Any? {
        val o = JsObject()
        i++
        skip()
        if (i < s.length && s[i] == '}') {
            i++
            return o
        }
        while (true) {
            skip()
            if (i >= s.length || s[i] != '"') fail()
            val k = str()
            skip()
            if (i >= s.length || s[i] != ':') fail()
            i++
            o.props[k] = value()
            skip()
            if (i >= s.length) fail()
            if (s[i] == ',') {
                i++
                continue
            }
            if (s[i] == '}') {
                i++
                return o
            }
            fail()
        }
    }

    private fun arr(): Any? {
        val a = ArrayList<Any?>()
        i++
        skip()
        if (i < s.length && s[i] == ']') {
            i++
            return JsArray(a)
        }
        while (true) {
            a.add(value())
            skip()
            if (i >= s.length) fail()
            if (s[i] == ',') {
                i++
                continue
            }
            if (s[i] == ']') {
                i++
                return JsArray(a)
            }
            fail()
        }
    }

    private fun str(): String {
        val sb = StringBuilder()
        i++
        while (i < s.length) {
            val c = s[i]
            if (c == '"') {
                i++
                return sb.toString()
            }
            if (c == '\\' && i + 1 < s.length) {
                val e = s[i + 1]
                when (e) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    'u' -> {
                        if (i + 6 > s.length) fail()
                        val v = s.substring(i + 2, i + 6).toIntOrNull(16) ?: fail()
                        sb.append(v.toChar())
                        i += 4
                    }
                    else -> sb.append(e)
                }
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        fail()
    }
}

internal fun jsonParse(text: String): Any? = JsonReader(text).readAll()
