package com.island.phone.app.home.widget.custom

internal fun showValue(v: Any?): String = when {
    v is String -> v
    v is JsFunction -> toStr(v)
    v is JsError -> toStr(v)
    v is JsDate -> toStr(v)
    v is JsObject -> jsonStringify(v) ?: toStr(v)
    else -> toStr(v)
}

private fun ownValue(o: JsObject, k: String): Any? {
    if (o is JsArray) {
        val i = k.toIntOrNull()
        if (i != null) return if (i >= 0 && i < o.list.size) o.list[i] else Undefined
    }
    return o.getProp(k)
}

private fun strKeys(v: Any?): List<String> = when (v) {
    is JsObject -> v.ownKeys()
    is String -> v.indices.map { it.toString() }
    else -> emptyList()
}

private fun jsParseInt(text: String, radixArg: Any?): Double {
    var s = text.trim()
    var sign = 1.0
    if (s.startsWith("-")) {
        sign = -1.0
        s = s.substring(1)
    } else if (s.startsWith("+")) {
        s = s.substring(1)
    }
    var radix = if (radixArg === Undefined) 0 else toInt32(toNum(radixArg))
    if ((radix == 0 || radix == 16) && (s.startsWith("0x") || s.startsWith("0X"))) {
        s = s.substring(2)
        radix = 16
    }
    if (radix == 0) radix = 10
    if (radix < 2 || radix > 36) return Double.NaN
    var i = 0
    var v = 0.0
    while (i < s.length) {
        val dg = Character.digit(s[i], radix)
        if (dg < 0) break
        v = v * radix + dg
        i++
    }
    return if (i == 0) Double.NaN else sign * v
}

private val FloatPrefix = Regex("^[+-]?(Infinity|\\d+\\.?\\d*([eE][+-]?\\d+)?|\\.\\d+([eE][+-]?\\d+)?)")

private fun jsParseFloat(text: String): Double {
    val m = FloatPrefix.find(text.trim()) ?: return Double.NaN
    val t = m.value
    if (t.endsWith("Infinity")) return if (t.startsWith("-")) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    return t.toDoubleOrNull() ?: Double.NaN
}

private fun Interp.makeArray(a: List<Any?>): JsArray {
    if (a.size == 1) {
        val n = a[0]
        if (n is Double) {
            if (n < 0 || n > 10_000_000 || n != Math.floor(n)) throw JsThrow(JsError("RangeError", "数组长度不对"))
            return JsArray(MutableList<Any?>(n.toInt()) { Undefined })
        }
    }
    return JsArray(ArrayList<Any?>(a))
}

internal fun Interp.installBuiltins() {
    val g = global.vars
    g["undefined"] = Undefined
    g["NaN"] = Double.NaN
    g["Infinity"] = Double.POSITIVE_INFINITY

    // ---- console ----
    val console = JsObject()
    for (n in listOf("log", "info", "warn", "error", "debug")) {
        console.props[n] = nf(n) { _, a ->
            if (logs.size >= 200) logs.removeAt(0)
            logs.add(a.joinToString(" ") { showValue(it) })
            Undefined
        }
    }
    g["console"] = console

    // ---- Math ----
    val math = JsObject()
    math.props["PI"] = Math.PI
    math.props["E"] = Math.E
    math.props["LN2"] = Math.log(2.0)
    math.props["LN10"] = Math.log(10.0)
    math.props["SQRT2"] = Math.sqrt(2.0)
    fun m1(name: String, f: (Double) -> Double) {
        math.props[name] = nf(name) { _, a -> f(toNum(a.arg(0))) }
    }
    m1("abs") { Math.abs(it) }
    m1("floor") { Math.floor(it) }
    m1("ceil") { Math.ceil(it) }
    m1("round") { if (it.isNaN() || it.isInfinite()) it else Math.floor(it + 0.5) }
    m1("trunc") { if (it < 0) Math.ceil(it) else Math.floor(it) }
    m1("sign") { if (it.isNaN()) it else if (it > 0) 1.0 else if (it < 0) -1.0 else it }
    m1("sqrt") { Math.sqrt(it) }
    m1("cbrt") { Math.cbrt(it) }
    m1("sin") { Math.sin(it) }
    m1("cos") { Math.cos(it) }
    m1("tan") { Math.tan(it) }
    m1("asin") { Math.asin(it) }
    m1("acos") { Math.acos(it) }
    m1("atan") { Math.atan(it) }
    m1("exp") { Math.exp(it) }
    m1("log") { Math.log(it) }
    m1("log2") { Math.log(it) / Math.log(2.0) }
    m1("log10") { Math.log10(it) }
    math.props["atan2"] = nf("atan2") { _, a -> Math.atan2(toNum(a.arg(0)), toNum(a.arg(1))) }
    math.props["pow"] = nf("pow") { _, a -> Math.pow(toNum(a.arg(0)), toNum(a.arg(1))) }
    math.props["random"] = nf("random") { _, _ -> Math.random() }
    math.props["hypot"] = nf("hypot") { _, a ->
        var s = 0.0
        for (x in a) {
            val d = toNum(x)
            s += d * d
        }
        Math.sqrt(s)
    }
    math.props["min"] = nf("min") { _, a ->
        var r = Double.POSITIVE_INFINITY
        for (x in a) {
            val d = toNum(x)
            if (d.isNaN()) {
                r = Double.NaN
                break
            }
            if (d < r) r = d
        }
        r
    }
    math.props["max"] = nf("max") { _, a ->
        var r = Double.NEGATIVE_INFINITY
        for (x in a) {
            val d = toNum(x)
            if (d.isNaN()) {
                r = Double.NaN
                break
            }
            if (d > r) r = d
        }
        r
    }
    g["Math"] = math

    // ---- JSON ----
    val json = JsObject()
    json.props["parse"] = nf("parse") { _, a -> jsonParse(toStr(a.arg(0))) }
    json.props["stringify"] = nf("stringify") { _, a ->
        val ind = a.arg(2)
        val indent = when (ind) {
            is Double -> " ".repeat(ind.toInt().coerceIn(0, 10))
            is String -> ind.take(10)
            else -> ""
        }
        jsonStringify(a.arg(0), indent) ?: Undefined
    }
    g["JSON"] = json

    // ---- Object ----
    val objectFn = NativeFn(
        "Object",
        ctor = { _ -> JsObject() },
        fn = { _, a -> if (a.arg(0) is JsObject) a.arg(0) else JsObject() },
    )
    objectFn.props["keys"] = nf("keys") { _, a ->
        if (isNullish(a.arg(0))) throw typeError("Object.keys 不能用在 ${toStr(a.arg(0))} 上")
        JsArray(ArrayList<Any?>(strKeys(a.arg(0))))
    }
    objectFn.props["values"] = nf("values") { _, a ->
        val o = a.arg(0)
        if (isNullish(o)) throw typeError("Object.values 不能用在 ${toStr(o)} 上")
        val out = ArrayList<Any?>()
        for (k in strKeys(o)) out.add(if (o is JsObject) ownValue(o, k) else getMember(o, k))
        JsArray(out)
    }
    objectFn.props["entries"] = nf("entries") { _, a ->
        val o = a.arg(0)
        if (isNullish(o)) throw typeError("Object.entries 不能用在 ${toStr(o)} 上")
        val out = ArrayList<Any?>()
        for (k in strKeys(o)) {
            out.add(JsArray(arrayListOf<Any?>(k, if (o is JsObject) ownValue(o, k) else getMember(o, k))))
        }
        JsArray(out)
    }
    objectFn.props["assign"] = nf("assign") { _, a ->
        val t = a.arg(0)
        if (isNullish(t)) throw typeError("Object.assign 的第一个参数不能是 ${toStr(t)}")
        for (i in 1 until a.size) {
            val src = a[i]
            if (src is JsObject) {
                for (k in src.ownKeys()) setMember(t, k, ownValue(src, k))
            }
        }
        t
    }
    objectFn.props["fromEntries"] = nf("fromEntries") { _, a ->
        val o = JsObject()
        for (e in iterToList(a.arg(0))) {
            if (e is JsArray && e.list.isNotEmpty()) {
                o.props[toStr(e.list[0])] = if (e.list.size > 1) e.list[1] else Undefined
            }
        }
        o
    }
    objectFn.props["freeze"] = nf("freeze") { _, a -> a.arg(0) }
    objectFn.props["isFrozen"] = nf("isFrozen") { _, _ -> false }
    g["Object"] = objectFn

    // ---- Array ----
    val arrayFn = NativeFn("Array", ctor = { a -> makeArray(a) }, fn = { _, a -> makeArray(a) })
    arrayFn.props["isArray"] = nf("isArray") { _, a -> a.arg(0) is JsArray }
    arrayFn.props["of"] = nf("of") { _, a -> JsArray(ArrayList<Any?>(a)) }
    arrayFn.props["from"] = nf("from") { _, a ->
        val src = a.arg(0)
        val items: List<Any?> = when {
            src is JsArray -> src.list.toList()
            src is String -> src.map { it.toString() }
            src is JsObject && src.hasProp("length") -> {
                val n = toInt32(toNum(src.getProp("length"))).coerceIn(0, 1_000_000)
                (0 until n).map { src.getProp(it.toString()) }
            }
            else -> emptyList()
        }
        val f = a.arg(1)
        if (f is JsFunction) {
            JsArray(ArrayList<Any?>(items.mapIndexed { i, x -> callValue(f, Undefined, listOf(x, i.toDouble())) }))
        } else {
            JsArray(ArrayList<Any?>(items))
        }
    }
    g["Array"] = arrayFn

    // ---- Number / String / Boolean ----
    val numberFn = nf("Number") { _, a -> if (a.isEmpty()) 0.0 else toNum(a[0]) }
    numberFn.props["isInteger"] = nf("isInteger") { _, a ->
        val v = a.arg(0)
        v is Double && !v.isNaN() && !v.isInfinite() && v == Math.floor(v)
    }
    numberFn.props["isSafeInteger"] = nf("isSafeInteger") { _, a ->
        val v = a.arg(0)
        v is Double && !v.isNaN() && !v.isInfinite() && v == Math.floor(v) && Math.abs(v) <= 9007199254740991.0
    }
    numberFn.props["isFinite"] = nf("isFinite") { _, a ->
        val v = a.arg(0)
        v is Double && !v.isNaN() && !v.isInfinite()
    }
    numberFn.props["isNaN"] = nf("isNaN") { _, a ->
        val v = a.arg(0)
        v is Double && v.isNaN()
    }
    numberFn.props["parseFloat"] = nf("parseFloat") { _, a -> jsParseFloat(toStr(a.arg(0))) }
    numberFn.props["parseInt"] = nf("parseInt") { _, a -> jsParseInt(toStr(a.arg(0)), a.arg(1)) }
    numberFn.props["MAX_SAFE_INTEGER"] = 9007199254740991.0
    numberFn.props["MIN_SAFE_INTEGER"] = -9007199254740991.0
    numberFn.props["MAX_VALUE"] = Double.MAX_VALUE
    numberFn.props["MIN_VALUE"] = java.lang.Double.MIN_VALUE
    numberFn.props["EPSILON"] = Math.ulp(1.0)
    numberFn.props["POSITIVE_INFINITY"] = Double.POSITIVE_INFINITY
    numberFn.props["NEGATIVE_INFINITY"] = Double.NEGATIVE_INFINITY
    numberFn.props["NaN"] = Double.NaN
    g["Number"] = numberFn

    val stringFn = nf("String") { _, a -> if (a.isEmpty()) "" else toStr(a[0]) }
    stringFn.props["fromCharCode"] = nf("fromCharCode") { _, a ->
        val sb = StringBuilder()
        for (x in a) sb.append(toInt32(toNum(x)).toChar())
        sb.toString()
    }
    g["String"] = stringFn
    g["Boolean"] = nf("Boolean") { _, a -> truthy(a.arg(0)) }
    g["parseInt"] = nf("parseInt") { _, a -> jsParseInt(toStr(a.arg(0)), a.arg(1)) }
    g["parseFloat"] = nf("parseFloat") { _, a -> jsParseFloat(toStr(a.arg(0))) }
    g["isNaN"] = nf("isNaN") { _, a -> toNum(a.arg(0)).isNaN() }
    g["isFinite"] = nf("isFinite") { _, a ->
        val d = toNum(a.arg(0))
        !d.isNaN() && !d.isInfinite()
    }

    // ---- Date ----
    val dateFn = NativeFn(
        "Date",
        ctor = { a -> JsDate(makeDateMs(a)) },
        fn = { _, _ -> dateToString(System.currentTimeMillis().toDouble()) },
    )
    dateFn.props["now"] = nf("now") { _, _ -> System.currentTimeMillis().toDouble() }
    dateFn.props["parse"] = nf("parse") { _, a -> parseDateStr(toStr(a.arg(0))) }
    dateFn.props["UTC"] = nf("UTC") { _, a ->
        val nums = a.map { toNum(it) }
        if (nums.isEmpty() || nums.any { it.isNaN() }) {
            Double.NaN
        } else {
            val c = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"))
            c.clear()
            c.set(
                nums[0].toInt(), if (nums.size > 1) nums[1].toInt() else 0, if (nums.size > 2) nums[2].toInt() else 1,
                if (nums.size > 3) nums[3].toInt() else 0, if (nums.size > 4) nums[4].toInt() else 0, if (nums.size > 5) nums[5].toInt() else 0,
            )
            c.timeInMillis.toDouble()
        }
    }
    g["Date"] = dateFn

    // ---- Error ----
    for (n in listOf("Error", "TypeError", "RangeError", "SyntaxError", "ReferenceError")) {
        g[n] = NativeFn(
            n,
            ctor = { a -> JsError(n, if (a.arg(0) === Undefined) "" else toStr(a.arg(0))) },
            fn = { _, a -> JsError(n, if (a.arg(0) === Undefined) "" else toStr(a.arg(0))) },
        )
    }

    // ---- Promise（同步结算版）----
    val promiseFn = NativeFn(
        "Promise",
        ctor = { a ->
            val ex = a.arg(0)
            var settled: JsPromise? = null
            val res = nf("resolve") { _, b ->
                if (settled == null) {
                    val v = b.arg(0)
                    settled = if (v is JsPromise) v else JsPromise(v)
                }
                Undefined
            }
            val rej = nf("reject") { _, b ->
                if (settled == null) settled = JsPromise(b.arg(0), true)
                Undefined
            }
            if (ex is JsFunction) {
                try {
                    callValue(ex, Undefined, listOf(res, rej))
                } catch (t: JsThrow) {
                    if (settled == null) settled = JsPromise(t.value, true)
                }
            }
            settled ?: JsPromise(Undefined)
        },
        fn = { _, _ -> throw typeError("Promise 必须用 new 来创建") },
    )
    promiseFn.props["resolve"] = nf("resolve") { _, a ->
        val v = a.arg(0)
        if (v is JsPromise) v else JsPromise(v)
    }
    promiseFn.props["reject"] = nf("reject") { _, a -> JsPromise(a.arg(0), true) }
    promiseFn.props["all"] = nf("all") { _, a ->
        val out = ArrayList<Any?>()
        var failed: JsPromise? = null
        for (x in iterToList(a.arg(0))) {
            if (x is JsPromise) {
                if (x.rejected) {
                    if (failed == null) failed = x
                } else {
                    out.add(x.value)
                }
            } else {
                out.add(x)
            }
        }
        failed ?: JsPromise(JsArray(out))
    }
    promiseFn.props["race"] = nf("race") { _, a ->
        val items = iterToList(a.arg(0))
        if (items.isEmpty()) {
            JsPromise(Undefined)
        } else {
            val x = items[0]
            if (x is JsPromise) x else JsPromise(x)
        }
    }
    g["Promise"] = promiseFn
}
