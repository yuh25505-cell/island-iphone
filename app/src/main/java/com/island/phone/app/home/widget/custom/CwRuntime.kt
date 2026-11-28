package com.island.phone.app.home.widget.custom

internal class CwConfig(
    val html: String,
    val css: String,
    val images: Map<String, String>,
    val fields: Map<String, String>,
    val stateJson: String,
    val widthDp: Float,
    val heightDp: Float,
    val dark: Boolean,
    val textColor: String,
)

private object Unhandled

private class Timer(val id: Int, val fn: JsFunction, val args: List<Any?>, var due: Long, val interval: Long)

internal fun camelToKebab(s: String): String {
    if (s.startsWith("--")) return s
    if (s == "cssFloat") return "float"
    val sb = StringBuilder()
    for (c in s) {
        if (c.isUpperCase()) {
            sb.append('-')
            sb.append(c.lowercaseChar())
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

private fun kebabToCamel(s: String): String {
    val sb = StringBuilder()
    var up = false
    for (c in s) {
        if (c == '-') {
            up = true
        } else {
            sb.append(if (up) c.uppercaseChar() else c)
            up = false
        }
    }
    return sb.toString()
}

internal class JsElement(val node: DomNode, private val rt: CwRuntime) : JsObject() {
    override fun getProp(name: String): Any? {
        val v = rt.elementGet(this, name)
        return if (v === Unhandled) super.getProp(name) else v
    }

    override fun setProp(name: String, value: Any?) {
        if (!rt.elementSet(this, name, value)) super.setProp(name, value)
    }

    override fun hasProp(name: String): Boolean = getProp(name) !== Undefined
    override fun ownKeys(): List<String> = emptyList()
    override fun jsString(): String = if (node.isText) "[object Text]" else "[object HTMLElement]"
}

internal class JsStyle(private val node: DomNode, private val rt: CwRuntime) : JsObject() {
    override fun getProp(name: String): Any? = rt.styleGet(node, name)
    override fun setProp(name: String, value: Any?) {
        rt.styleSet(node, name, value)
    }
    override fun hasProp(name: String): Boolean = getProp(name) !== Undefined
    override fun jsString(): String = "[object CSSStyleDeclaration]"
}

internal class JsClassList(private val node: DomNode, private val rt: CwRuntime) : JsObject() {
    private fun list(): MutableList<String> = node.classes().toMutableList()

    private fun save(l: List<String>) {
        node.attrs["class"] = l.joinToString(" ")
        rt.invalidate()
    }

    override fun getProp(name: String): Any? = when (name) {
        "add" -> nf(name) { _, a ->
            val l = list()
            for (x in a) {
                val s = toStr(x)
                if (s.isNotEmpty() && s !in l) l.add(s)
            }
            save(l)
            Undefined
        }
        "remove" -> nf(name) { _, a ->
            val l = list()
            for (x in a) l.remove(toStr(x))
            save(l)
            Undefined
        }
        "toggle" -> nf(name) { _, a ->
            val l = list()
            val s = toStr(a.arg(0))
            val has = s in l
            val want = if (a.arg(1) === Undefined) !has else truthy(a.arg(1))
            if (want && !has) l.add(s)
            if (!want && has) l.remove(s)
            save(l)
            want
        }
        "contains" -> nf(name) { _, a -> toStr(a.arg(0)) in list() }
        "replace" -> nf(name) { _, a ->
            val l = list()
            val i = l.indexOf(toStr(a.arg(0)))
            if (i >= 0) {
                l[i] = toStr(a.arg(1))
                save(l)
            }
            i >= 0
        }
        "item" -> nf(name) { _, a -> list().getOrNull(toInt32(toNum(a.arg(0)))) }
        "length" -> list().size.toDouble()
        "value" -> node.attrs["class"] ?: ""
        else -> Undefined
    }

    override fun hasProp(name: String): Boolean = getProp(name) !== Undefined
    override fun jsString(): String = node.attrs["class"] ?: ""
}

internal class JsDataset(private val node: DomNode, private val rt: CwRuntime) : JsObject() {
    override fun getProp(name: String): Any? = node.attrs["data-" + camelToKebab(name)] ?: Undefined
    override fun setProp(name: String, value: Any?) {
        node.attrs["data-" + camelToKebab(name)] = toStr(value)
        rt.invalidate()
    }
    override fun hasProp(name: String): Boolean = node.attrs.containsKey("data-" + camelToKebab(name))
    override fun deleteProp(name: String) {
        node.attrs.remove("data-" + camelToKebab(name))
        rt.invalidate()
    }
    override fun ownKeys(): List<String> =
        node.attrs.keys.filter { it.startsWith("data-") }.map { kebabToCamel(it.removePrefix("data-")) }
}

internal class JsGlobal(private val vars: HashMap<String, Any?>) : JsObject() {
    override fun getProp(name: String): Any? = if (vars.containsKey(name)) vars[name] else Undefined
    override fun setProp(name: String, value: Any?) {
        vars[name] = value
    }
    override fun hasProp(name: String): Boolean = vars.containsKey(name)
    override fun deleteProp(name: String) {
        vars.remove(name)
    }
    override fun ownKeys(): List<String> = vars.keys.toList()
    override fun jsString(): String = "[object Window]"
}

private val PlaceRe = Regex("\\{\\{\\s*([^{}]+?)\\s*\\}\\}")
private val ScriptRe = Regex("<script\\b[\\s\\S]*?</script\\s*>", RegexOption.IGNORE_CASE)
private val TapTypes = listOf("click", "touchstart", "touchend", "pointerdown", "pointerup", "mousedown", "mouseup")
private val HandlerAttrs = mapOf(
    "id" to "id", "className" to "class", "title" to "title", "alt" to "alt", "src" to "src", "href" to "href",
    "placeholder" to "placeholder", "type" to "type", "name" to "name", "htmlFor" to "for", "lang" to "lang",
    "role" to "role",
)

internal class CwRuntime(
    private val cfg: CwConfig,
    private val onSave: (String) -> Unit,
    private val onToast: (String) -> Unit,
    private val onReload: () -> Unit,
) {
    val interp = Interp()
    val docRoot = DomNode("#root")
    val body = DomNode("body")
    val errors = ArrayList<String>()
    val sizes = HashMap<DomNode, FloatArray>()
    val animEpoch = HashMap<DomNode, Int>()
    var version = 0
    var onChange: (() -> Unit)? = null
    var canvasFactory: ((DomNode) -> JsObject?)? = null
    var sheet: Stylesheet = Stylesheet.Empty
        private set
    var dark: Boolean = cfg.dark
        private set

    private var computedCache: ComputedTree? = null
    private val state = LinkedHashMap<String, String>()
    private var dirtyAt = -1L
    private var ready = false
    private val readyCallbacks = ArrayList<JsFunction>()
    private val themeCallbacks = ArrayList<JsFunction>()
    private val docListeners = HashMap<String, ArrayList<JsFunction>>()
    private val timers = ArrayList<Timer>()
    private var frames = ArrayList<Pair<Int, JsFunction>>()
    private var nextId = 1
    private val started = System.currentTimeMillis()
    private val authorSheets = ArrayList<Stylesheet>()

    init {
        start()
    }

    // ---------- 基础 ----------

    fun report(msg: String) {
        if (errors.size < 30) errors.add(msg)
    }

    fun invalidate() {
        computedCache = null
        version++
        onChange?.invoke()
    }

    fun computed(): ComputedTree {
        val c = computedCache
        if (c != null) return c
        val t = computeStyles(docRoot, sheet, dark)
        computedCache = t
        return t
    }

    fun setDark(value: Boolean) {
        if (value == dark) return
        dark = value
        sheet = buildSheet()
        invalidate()
        for (cb in themeCallbacks.toList()) callGuarded(cb, Undefined, listOf(value))
    }

    private fun subst(text: String): String = PlaceRe.replace(text) { m ->
        val k = m.groupValues[1]
        cfg.images[k] ?: cfg.fields[k] ?: ""
    }

    private fun substOutsideScripts(src: String): String {
        val sb = StringBuilder()
        var last = 0
        for (m in ScriptRe.findAll(src)) {
            sb.append(subst(src.substring(last, m.range.first)))
            sb.append(m.value)
            last = m.range.last + 1
        }
        sb.append(subst(src.substring(last)))
        return sb.toString()
    }

    private fun uaCss(): String {
        val text = cfg.textColor
        return "body{margin:0;padding:0;width:100%;height:100%;box-sizing:border-box;font-family:sans-serif;color:$text}" +
            "h1{font-size:2em;margin:.67em 0}h2{font-size:1.5em;margin:.83em 0}h3{font-size:1.17em;margin:1em 0}" +
            "h4{margin:1.33em 0}h5{font-size:.83em;margin:1.67em 0}h6{font-size:.67em;margin:2.33em 0}" +
            "p{margin:1em 0}ul,ol{margin:1em 0;padding-left:40px}pre{margin:1em 0}hr{margin:8px 0;border-top:1px solid #88888866}" +
            "a{color:light-dark(#0066cc,#4da3ff);text-decoration:underline}" +
            "button{padding:4px 10px;border:1px solid light-dark(#767676,#636366);border-radius:6px;" +
            "background:light-dark(#efefef,#3a3a3c);color:light-dark(#000000,#ffffff);font-size:13.333px}" +
            "input,textarea,select{font-size:13.333px;padding:2px 4px;border:1px solid light-dark(#767676,#636366);" +
            "background:light-dark(#ffffff,#2c2c2e);color:light-dark(#000000,#ffffff)}"
    }

    private fun buildSheet(): Stylesheet {
        val list = ArrayList<Stylesheet>()
        list.add(parseStylesheet(uaCss()).sheet)
        list.addAll(authorSheets)
        return mergeStylesheets(list)
    }

    private fun addAuthorSheet(text: String) {
        if (text.isBlank()) return
        val r = parseStylesheet(subst(text))
        if (r.error != null) report("样式出错，" + r.error) else authorSheets.add(r.sheet)
    }

    private fun start() {
        if (cfg.stateJson.isNotBlank()) {
            try {
                val o = jsonParse(cfg.stateJson)
                if (o is JsObject) {
                    for (k in o.ownKeys()) {
                        val s = jsonStringify(o.getProp(k))
                        if (s != null) state[k] = s
                    }
                }
            } catch (t: JsThrow) {
                // 存档坏了就当没有
            }
        }
        docRoot.append(body)
        val parsed = HtmlParser.parse(substOutsideScripts(cfg.html))
        for (c in parsed.root.children.toList()) body.append(c)
        for (s in parsed.styles) addAuthorSheet(s)
        addAuthorSheet(cfg.css)
        sheet = buildSheet()
        installHostGlobals()
        for (src in parsed.scripts) {
            val err = interp.guarded { interp.run(src) }
            if (err != null) report("脚本出错，$err")
        }
        ready = true
        for (cb in readyCallbacks.toList()) callGuarded(cb, Undefined, emptyList())
        readyCallbacks.clear()
        fireDocument("DOMContentLoaded")
        fireDocument("load")
        invalidate()
    }

    private fun fireDocument(type: String) {
        val list = docListeners[type]?.toList() ?: return
        for (f in list) callGuarded(f, Undefined, listOf(makeEvent(type, null, emptyMap())))
    }

    fun callGuarded(f: JsFunction, thisVal: Any?, args: List<Any?>) {
        var result: Any? = Undefined
        val err = interp.guarded { result = interp.callValue(f, thisVal, args) }
        if (err != null) {
            report(err)
            return
        }
        val r = result
        if (r is JsPromise && r.rejected) report("脚本出错，" + errorText(r.value))
    }

    // ---------- 存档 ----------

    private fun stateJson(): String =
        "{" + state.entries.joinToString(",") { jsonQuote(it.key) + ":" + it.value } + "}"

    private fun markDirty() {
        if (dirtyAt < 0) dirtyAt = System.currentTimeMillis()
    }

    fun flush() {
        if (dirtyAt >= 0) {
            dirtyAt = -1
            onSave(stateJson())
        }
    }

    private fun putState(key: String, value: Any?) {
        val s = jsonStringify(value)
        if (s == null) state.remove(key) else state[key] = s
        markDirty()
    }

    private fun readState(key: String): Any? {
        val s = state[key] ?: return Undefined
        return try {
            jsonParse(s)
        } catch (t: JsThrow) {
            Undefined
        }
    }

    // ---------- 计时 ----------

    fun needsTick(): Boolean = timers.isNotEmpty() || frames.isNotEmpty() || dirtyAt >= 0
    fun hasFrames(): Boolean = frames.isNotEmpty()

    fun tick() {
        val now = System.currentTimeMillis()
        if (timers.isNotEmpty()) {
            val due = timers.filter { it.due <= now }.sortedBy { it.due }
            for (t in due) {
                if (!timers.contains(t)) continue
                if (t.interval >= 0) t.due = now + maxOf(1L, t.interval) else timers.remove(t)
                callGuarded(t.fn, Undefined, t.args)
            }
        }
        if (frames.isNotEmpty()) {
            val run = frames
            frames = ArrayList()
            val ts = (now - started).toDouble()
            for (p in run) callGuarded(p.second, Undefined, listOf(ts))
        }
        if (dirtyAt >= 0 && now - dirtyAt >= 500) flush()
    }

    // ---------- 事件 ----------

    private fun makeEvent(type: String, target: DomNode?, extra: Map<String, Any?>): JsObject {
        val ev = JsObject()
        ev.props["type"] = type
        ev.props["target"] = if (target == null) null else wrap(target)
        ev.props["bubbles"] = true
        ev.props["timeStamp"] = (System.currentTimeMillis() - started).toDouble()
        for ((k, v) in extra) ev.props[k] = v
        val x = extra["clientX"]
        val y = extra["clientY"]
        if (type.startsWith("touch") && x != null && y != null) {
            val t = JsObject()
            t.props["clientX"] = x
            t.props["clientY"] = y
            t.props["pageX"] = x
            t.props["pageY"] = y
            ev.props["touches"] = JsArray(arrayListOf<Any?>(t))
            ev.props["changedTouches"] = JsArray(arrayListOf<Any?>(t))
        }
        return ev
    }

    fun hasTapListeners(node: DomNode): Boolean {
        for (t in TapTypes) {
            if (node.listeners[t]?.isNotEmpty() == true) return true
            if (node.listeners["on:$t"]?.isNotEmpty() == true) return true
        }
        return false
    }

    fun dispatch(target: DomNode, type: String, extra: Map<String, Any?> = emptyMap()): Boolean {
        val ev = makeEvent(type, target, extra)
        var stopped = false
        var prevented = false
        ev.props["preventDefault"] = nf("preventDefault") { _, _ ->
            prevented = true
            Undefined
        }
        ev.props["stopPropagation"] = nf("stopPropagation") { _, _ ->
            stopped = true
            Undefined
        }
        ev.props["stopImmediatePropagation"] = nf("stopImmediatePropagation") { _, _ ->
            stopped = true
            Undefined
        }
        var n: DomNode? = target
        while (n != null && n.tag != "#root") {
            ev.props["currentTarget"] = wrap(n)
            val all = ArrayList<JsFunction>()
            n.listeners["on:$type"]?.let { all.addAll(it) }
            n.listeners[type]?.let { all.addAll(it) }
            for (f in all) callGuarded(f, wrap(n), listOf(ev))
            if (stopped) break
            n = n.parent
        }
        if (!stopped) {
            val docs = docListeners[type]?.toList()
            if (docs != null) {
                ev.props["currentTarget"] = null
                for (f in docs) callGuarded(f, Undefined, listOf(ev))
            }
        }
        return prevented
    }

    fun setPressed(node: DomNode, pressed: Boolean) {
        var n: DomNode? = node
        while (n != null && n.tag != "#root") {
            n.pressed = pressed
            n = n.parent
        }
        invalidate()
    }

    fun tap(node: DomNode, x: Float, y: Float) {
        val extra = mapOf<String, Any?>("clientX" to x.toDouble(), "clientY" to y.toDouble())
        val prevented = dispatch(node, "click", extra)
        if (prevented) return
        var n: DomNode? = node
        while (n != null && n.tag != "#root") {
            if (n.tag == "label") {
                val forId = n.attrs["for"]
                val target = if (forId != null) findById(forId) else firstControl(n)
                if (target != null && target !== node) toggleControl(target)
                return
            }
            if (n.tag == "input") {
                toggleControl(n)
                return
            }
            n = n.parent
        }
    }

    private fun firstControl(label: DomNode): DomNode? {
        var found: DomNode? = null
        label.walk { if (found == null && it.tag == "input") found = it }
        return found
    }

    private fun toggleControl(t: DomNode) {
        if (t.tag != "input") return
        val type = (t.attrs["type"] ?: "text").lowercase()
        if (type != "checkbox" && type != "radio") return
        if (t.attrs.containsKey("disabled")) return
        if (type == "checkbox") {
            t.checkedFlag = !t.isChecked()
        } else {
            val name = t.attrs["name"]
            if (name != null) {
                body.walk { if (it.tag == "input" && it.attrs["type"] == "radio" && it.attrs["name"] == name) it.checkedFlag = false }
            }
            t.checkedFlag = true
        }
        invalidate()
        dispatch(t, "input")
        dispatch(t, "change")
    }

    fun setInputValue(node: DomNode, text: String) {
        node.valueText = text
        dispatch(node, "input")
        dispatch(node, "change")
        invalidate()
    }

    // ---------- 查找 ----------

    fun findById(id: String): DomNode? {
        var found: DomNode? = null
        body.walk { if (found == null && it.attrs["id"] == id) found = it }
        return found
    }

    fun wrap(node: DomNode): JsElement {
        val w = node.wrapper
        if (w is JsElement) return w
        val e = JsElement(node, this)
        node.wrapper = e
        return e
    }

    private fun wrapOrNull(node: DomNode?): Any? = if (node == null || node.tag == "#root") null else wrap(node)

    private fun nodeArg(v: Any?, what: String): DomNode {
        if (v is JsElement) return v.node
        throw interp.typeError("$what 需要传入一个元素")
    }

    private fun wrapList(list: List<DomNode>): JsArray = JsArray(ArrayList<Any?>(list.map { wrap(it) }))

    private fun checkNoCycle(parent: DomNode, child: DomNode) {
        var p: DomNode? = parent
        while (p != null) {
            if (p === child) throw interp.typeError("不能把元素放进它自己里面")
            p = p.parent
        }
    }

    private fun cloneDom(n: DomNode, deep: Boolean): DomNode {
        val c = DomNode(n.tag)
        c.text = n.text
        c.attrs.putAll(n.attrs)
        c.style.putAll(n.style)
        c.checkedFlag = n.checkedFlag
        c.valueText = n.valueText
        if (deep) for (k in n.children) c.append(cloneDom(k, true))
        return c
    }

    private fun textNode(s: String): DomNode {
        val t = DomNode("#text")
        t.text = s
        return t
    }

    private fun appendArgs(parent: DomNode, args: List<Any?>, atStart: Boolean) {
        val nodes = args.map { if (it is JsElement) it.node else textNode(toStr(it)) }
        if (atStart) {
            for ((i, n) in nodes.withIndex()) parent.insertAt(i, n)
        } else {
            for (n in nodes) parent.append(n)
        }
    }

    // ---------- 元素属性 ----------

    fun elementGet(el: JsElement, name: String): Any? {
        val n = el.node
        when (name) {
            "nodeType" -> return if (n.isText) 3.0 else 1.0
            "nodeName", "tagName" -> return if (n.isText) "#text" else n.tag.uppercase()
            "textContent", "innerText", "nodeValue" -> return n.textContent()
            "data" -> return if (n.isText) n.text else Undefined
            "innerHTML" -> return n.children.joinToString("") { serializeNode(it) }
            "outerHTML" -> return serializeNode(n)
            "id" -> return n.attrs["id"] ?: ""
            "className" -> return n.attrs["class"] ?: ""
            "style" -> return JsStyle(n, this)
            "classList" -> return JsClassList(n, this)
            "dataset" -> return JsDataset(n, this)
            "value" -> return n.valueText ?: n.attrs["value"] ?: ""
            "checked" -> return n.isChecked()
            "disabled" -> return n.attrs.containsKey("disabled")
            "hidden" -> return n.attrs.containsKey("hidden")
            "parentNode", "parentElement" -> return wrapOrNull(n.parent)
            "children" -> return wrapList(n.elementChildren())
            "childNodes" -> return wrapList(n.children)
            "childElementCount" -> return n.elementChildren().size.toDouble()
            "firstChild" -> return wrapOrNull(n.children.firstOrNull())
            "lastChild" -> return wrapOrNull(n.children.lastOrNull())
            "firstElementChild" -> return wrapOrNull(n.elementChildren().firstOrNull())
            "lastElementChild" -> return wrapOrNull(n.elementChildren().lastOrNull())
            "nextSibling", "nextElementSibling", "previousSibling", "previousElementSibling" -> {
                val p = n.parent ?: return null
                val elementsOnly = name.contains("Element")
                val sibs = if (elementsOnly) p.elementChildren() else p.children
                val i = sibs.indexOf(n)
                val j = if (name.startsWith("next")) i + 1 else i - 1
                return wrapOrNull(sibs.getOrNull(j))
            }
            "offsetWidth", "clientWidth", "scrollWidth" -> return (sizes[n]?.get(0) ?: if (n === body) cfg.widthDp else 0f).toDouble()
            "offsetHeight", "clientHeight", "scrollHeight" -> return (sizes[n]?.get(1) ?: if (n === body) cfg.heightDp else 0f).toDouble()
            "offsetLeft", "offsetTop", "scrollLeft", "scrollTop" -> return 0.0
        }
        val attr = HandlerAttrs[name]
        if (attr != null) return n.attrs[attr] ?: ""
        if (name.length > 2 && name.startsWith("on") && name.all { it.isLowerCase() }) {
            return n.listeners["on:" + name.substring(2)]?.firstOrNull()
        }
        return elementMethod(el, name)
    }

    fun elementSet(el: JsElement, name: String, value: Any?): Boolean {
        val n = el.node
        when (name) {
            "textContent", "innerText", "nodeValue", "data" -> {
                if (n.isText) n.text = toStr(value) else n.setTextContent(toStr(value))
                invalidate()
                return true
            }
            "innerHTML" -> {
                n.clearChildren()
                val parsed = HtmlParser.parse(subst(toStr(value)))
                for (c in parsed.root.children.toList()) n.append(c)
                invalidate()
                return true
            }
            "className" -> {
                n.attrs["class"] = toStr(value)
                invalidate()
                return true
            }
            "style" -> {
                n.style.clear()
                for (d in parseDeclarations(toStr(value))) n.style[d.prop] = d.value
                invalidate()
                return true
            }
            "value" -> {
                n.valueText = toStr(value)
                invalidate()
                return true
            }
            "checked" -> {
                n.checkedFlag = truthy(value)
                invalidate()
                return true
            }
            "disabled", "hidden" -> {
                if (truthy(value)) n.attrs[name] = "" else n.attrs.remove(name)
                invalidate()
                return true
            }
        }
        val attr = HandlerAttrs[name]
        if (attr != null) {
            n.attrs[attr] = toStr(value)
            invalidate()
            return true
        }
        if (name.length > 2 && name.startsWith("on") && name.all { it.isLowerCase() }) {
            val key = "on:" + name.substring(2)
            n.listeners.remove(key)
            if (value is JsFunction) n.listeners[key] = arrayListOf(value)
            invalidate()
            return true
        }
        return false
    }

    private fun addListener(map: HashMap<String, ArrayList<JsFunction>>, type: String, f: Any?) {
        if (f !is JsFunction) return
        val l = map.getOrPut(type) { ArrayList() }
        if (!l.contains(f)) l.add(f)
    }

    private fun elementMethod(el: JsElement, name: String): Any? {
        val n = el.node
        return when (name) {
            "getAttribute" -> nf(name) { _, a -> n.attrs[toStr(a.arg(0)).lowercase()] }
            "setAttribute" -> nf(name) { _, a ->
                val k = toStr(a.arg(0)).lowercase()
                val v = toStr(a.arg(1))
                n.attrs[k] = v
                if (k == "style") {
                    n.style.clear()
                    for (d in parseDeclarations(v)) n.style[d.prop] = d.value
                }
                invalidate()
                Undefined
            }
            "removeAttribute" -> nf(name) { _, a ->
                val k = toStr(a.arg(0)).lowercase()
                n.attrs.remove(k)
                if (k == "style") n.style.clear()
                invalidate()
                Undefined
            }
            "hasAttribute" -> nf(name) { _, a -> n.attrs.containsKey(toStr(a.arg(0)).lowercase()) }
            "toggleAttribute" -> nf(name) { _, a ->
                val k = toStr(a.arg(0)).lowercase()
                val has = n.attrs.containsKey(k)
                val want = if (a.arg(1) === Undefined) !has else truthy(a.arg(1))
                if (want) n.attrs[k] = "" else n.attrs.remove(k)
                invalidate()
                want
            }
            "appendChild" -> nf(name) { _, a ->
                val c = nodeArg(a.arg(0), "appendChild")
                checkNoCycle(n, c)
                n.append(c)
                invalidate()
                a.arg(0)
            }
            "append" -> nf(name) { _, a ->
                appendArgs(n, a, false)
                invalidate()
                Undefined
            }
            "prepend" -> nf(name) { _, a ->
                appendArgs(n, a, true)
                invalidate()
                Undefined
            }
            "removeChild" -> nf(name) { _, a ->
                val c = nodeArg(a.arg(0), "removeChild")
                if (c.parent === n) c.detach()
                invalidate()
                a.arg(0)
            }
            "remove" -> nf(name) { _, _ ->
                n.detach()
                invalidate()
                Undefined
            }
            "insertBefore" -> nf(name) { _, a ->
                val c = nodeArg(a.arg(0), "insertBefore")
                checkNoCycle(n, c)
                val ref = a.arg(1)
                if (ref is JsElement && ref.node.parent === n) {
                    n.insertAt(n.children.indexOf(ref.node), c)
                } else {
                    n.append(c)
                }
                invalidate()
                a.arg(0)
            }
            "replaceChild" -> nf(name) { _, a ->
                val c = nodeArg(a.arg(0), "replaceChild")
                val old = nodeArg(a.arg(1), "replaceChild")
                if (old.parent === n) {
                    val i = n.children.indexOf(old)
                    old.detach()
                    n.insertAt(i, c)
                }
                invalidate()
                a.arg(1)
            }
            "cloneNode" -> nf(name) { _, a -> wrap(cloneDom(n, truthy(a.arg(0)))) }
            "contains" -> nf(name) { _, a ->
                val o = a.arg(0)
                var found = false
                if (o is JsElement) {
                    var p: DomNode? = o.node
                    while (p != null) {
                        if (p === n) {
                            found = true
                            break
                        }
                        p = p.parent
                    }
                }
                found
            }
            "querySelector" -> nf(name) { _, a -> wrapOrNull(querySelectorAll(n, toStr(a.arg(0)), true).firstOrNull()) }
            "querySelectorAll" -> nf(name) { _, a -> wrapList(querySelectorAll(n, toStr(a.arg(0)), false)) }
            "getElementsByClassName" -> nf(name) { _, a -> wrapList(querySelectorAll(n, "." + toStr(a.arg(0)), false)) }
            "getElementsByTagName" -> nf(name) { _, a -> wrapList(querySelectorAll(n, toStr(a.arg(0)), false)) }
            "matches" -> nf(name) { _, a -> parseSelectorList(toStr(a.arg(0))).any { selectorMatches(it, n) } }
            "closest" -> nf(name) { _, a ->
                val sels = parseSelectorList(toStr(a.arg(0)))
                var p: DomNode? = n
                var found: DomNode? = null
                while (p != null && p.tag != "#root") {
                    val cur: DomNode = p
                    if (sels.any { selectorMatches(it, cur) }) {
                        found = cur
                        break
                    }
                    p = cur.parent
                }
                wrapOrNull(found)
            }
            "addEventListener" -> nf(name) { _, a ->
                addListener(n.listeners, toStr(a.arg(0)), a.arg(1))
                invalidate()
                Undefined
            }
            "removeEventListener" -> nf(name) { _, a ->
                val f = a.arg(1)
                if (f is JsFunction) n.listeners[toStr(a.arg(0))]?.remove(f)
                invalidate()
                Undefined
            }
            "dispatchEvent" -> nf(name) { _, a ->
                val ev = a.arg(0)
                if (ev is JsObject) dispatch(n, toStr(ev.getProp("type"))) else false
            }
            "click" -> nf(name) { _, _ ->
                tap(n, 0f, 0f)
                Undefined
            }
            "focus", "blur", "scrollIntoView", "select" -> nf(name) { _, _ -> Undefined }
            "getBoundingClientRect" -> nf(name) { _, _ ->
                val s = sizes[n]
                val w = (s?.get(0) ?: if (n === body) cfg.widthDp else 0f).toDouble()
                val h = (s?.get(1) ?: if (n === body) cfg.heightDp else 0f).toDouble()
                val o = JsObject()
                o.props["x"] = 0.0
                o.props["y"] = 0.0
                o.props["left"] = 0.0
                o.props["top"] = 0.0
                o.props["width"] = w
                o.props["height"] = h
                o.props["right"] = w
                o.props["bottom"] = h
                o
            }
            "getContext" -> nf(name) { _, a ->
                val kind = toStr(a.arg(0))
                if (n.tag == "canvas" && kind == "2d") canvasFactory?.invoke(n) else null
            }
            else -> Unhandled
        }
    }

    // ---------- 样式对象 ----------

    fun styleGet(node: DomNode, name: String): Any? = when (name) {
        "setProperty" -> nf(name) { _, a ->
            styleSet(node, toStr(a.arg(0)), a.arg(1))
            Undefined
        }
        "getPropertyValue" -> nf(name) { _, a -> node.style[normProp(toStr(a.arg(0)))] ?: "" }
        "removeProperty" -> nf(name) { _, a ->
            val k = normProp(toStr(a.arg(0)))
            val old = node.style.remove(k) ?: ""
            invalidate()
            old
        }
        "cssText" -> node.style.entries.joinToString("; ") { it.key + ": " + it.value }
        "length" -> node.style.size.toDouble()
        else -> node.style[normProp(name)] ?: ""
    }

    private fun normProp(name: String): String =
        if (name.startsWith("--")) name else camelToKebab(name).lowercase()

    fun styleSet(node: DomNode, name: String, value: Any?) {
        if (name == "cssText") {
            node.style.clear()
            for (d in parseDeclarations(toStr(value))) node.style[d.prop] = d.value
            invalidate()
            return
        }
        val key = normProp(name)
        val v = if (value == null || value === Undefined) "" else toStr(value).trim()
        if (v.isEmpty()) node.style.remove(key) else node.style[key] = v
        if (key.startsWith("animation")) animEpoch[node] = (animEpoch[node] ?: 0) + 1
        invalidate()
    }

    // ---------- 全局对象 ----------

    private fun newTimer(a: List<Any?>, repeat: Boolean): Any? {
        val f = a.arg(0)
        if (f !is JsFunction) throw interp.typeError("定时器需要传入一个函数")
        val delay = toNum(a.arg(1)).let { if (it.isNaN() || it < 0) 0L else it.toLong() }
        val id = nextId++
        val extra = if (a.size > 2) a.drop(2) else emptyList()
        timers.add(Timer(id, f, extra, System.currentTimeMillis() + delay, if (repeat) maxOf(1L, delay) else -1L))
        return id.toDouble()
    }

    private fun installHostGlobals() {
        val g = interp.global.vars

        // document
        val doc = JsObject()
        doc.props["getElementById"] = nf("getElementById") { _, a -> wrapOrNull(findById(toStr(a.arg(0)))) }
        doc.props["querySelector"] = nf("querySelector") { _, a ->
            wrapOrNull(querySelectorAll(docRoot, toStr(a.arg(0)), true).firstOrNull())
        }
        doc.props["querySelectorAll"] = nf("querySelectorAll") { _, a -> wrapList(querySelectorAll(docRoot, toStr(a.arg(0)), false)) }
        doc.props["getElementsByClassName"] = nf("getElementsByClassName") { _, a ->
            wrapList(querySelectorAll(docRoot, "." + toStr(a.arg(0)), false))
        }
        doc.props["getElementsByTagName"] = nf("getElementsByTagName") { _, a -> wrapList(querySelectorAll(docRoot, toStr(a.arg(0)), false)) }
        doc.props["createElement"] = nf("createElement") { _, a -> wrap(DomNode(toStr(a.arg(0)).lowercase())) }
        doc.props["createTextNode"] = nf("createTextNode") { _, a -> wrap(textNode(toStr(a.arg(0)))) }
        doc.props["addEventListener"] = nf("addEventListener") { _, a ->
            addListener(docListeners, toStr(a.arg(0)), a.arg(1))
            Undefined
        }
        doc.props["removeEventListener"] = nf("removeEventListener") { _, a ->
            val f = a.arg(1)
            if (f is JsFunction) docListeners[toStr(a.arg(0))]?.remove(f)
            Undefined
        }
        doc.props["body"] = wrap(body)
        doc.props["documentElement"] = wrap(body)
        doc.props["readyState"] = "complete"
        doc.props["hidden"] = false
        doc.props["title"] = ""
        g["document"] = doc

        // widget：存档和组件信息
        val widget = JsObject()
        widget.props["onReady"] = nf("onReady") { _, a ->
            val f = a.arg(0)
            if (f is JsFunction) {
                if (ready) callGuarded(f, Undefined, emptyList()) else readyCallbacks.add(f)
            }
            Undefined
        }
        widget.props["getState"] = nf("getState") { _, a -> JsPromise(readState(toStr(a.arg(0)))) }
        widget.props["getAllState"] = nf("getAllState") { _, _ ->
            val o = JsObject()
            for ((k, s) in state) {
                try {
                    o.props[k] = jsonParse(s)
                } catch (t: JsThrow) {
                    // 跳过坏数据
                }
            }
            JsPromise(o)
        }
        widget.props["setState"] = nf("setState") { _, a ->
            putState(toStr(a.arg(0)), a.arg(1))
            JsPromise(Undefined)
        }
        widget.props["clearState"] = nf("clearState") { _, a ->
            if (a.arg(0) === Undefined) state.clear() else state.remove(toStr(a.arg(0)))
            markDirty()
            JsPromise(Undefined)
        }
        widget.props["isDark"] = dark
        widget.props["width"] = cfg.widthDp.toDouble()
        widget.props["height"] = cfg.heightDp.toDouble()
        widget.props["toast"] = nf("toast") { _, a ->
            onToast(toStr(a.arg(0)))
            Undefined
        }
        widget.props["onThemeChange"] = nf("onThemeChange") { _, a ->
            val f = a.arg(0)
            if (f is JsFunction) themeCallbacks.add(f)
            Undefined
        }
        g["widget"] = widget

        // 定时器
        g["setTimeout"] = nf("setTimeout") { _, a -> newTimer(a, false) }
        g["setInterval"] = nf("setInterval") { _, a -> newTimer(a, true) }
        val clear = nf("clearTimeout") { _, a ->
            val id = toNum(a.arg(0)).toInt()
            timers.removeAll { it.id == id }
            Undefined
        }
        g["clearTimeout"] = clear
        g["clearInterval"] = clear
        g["requestAnimationFrame"] = nf("requestAnimationFrame") { _, a ->
            val f = a.arg(0)
            if (f !is JsFunction) throw interp.typeError("requestAnimationFrame 需要传入一个函数")
            val id = nextId++
            frames.add(Pair(id, f))
            id.toDouble()
        }
        g["cancelAnimationFrame"] = nf("cancelAnimationFrame") { _, a ->
            val id = toNum(a.arg(0)).toInt()
            frames.removeAll { it.first == id }
            Undefined
        }
        g["queueMicrotask"] = nf("queueMicrotask") { _, a ->
            val f = a.arg(0)
            if (f is JsFunction) timers.add(Timer(nextId++, f, emptyList(), System.currentTimeMillis(), -1L))
            Undefined
        }

        // 提示
        g["alert"] = nf("alert") { _, a ->
            onToast(toStr(a.arg(0)))
            Undefined
        }
        g["confirm"] = nf("confirm") { _, _ -> true }
        g["prompt"] = nf("prompt") { _, _ -> null }

        // 页面信息
        val loc = JsObject()
        loc.props["reload"] = nf("reload") { _, _ ->
            flush()
            onReload()
            Undefined
        }
        loc.props["href"] = "about:blank"
        loc.props["search"] = ""
        loc.props["hash"] = ""
        loc.props["pathname"] = "/"
        g["location"] = loc
        val nav = JsObject()
        nav.props["userAgent"] = "IslandPhone"
        nav.props["language"] = "zh-CN"
        nav.props["onLine"] = false
        nav.props["vibrate"] = nf("vibrate") { _, _ -> true }
        g["navigator"] = nav
        val perf = JsObject()
        perf.props["now"] = nf("now") { _, _ -> (System.currentTimeMillis() - started).toDouble() }
        g["performance"] = perf
        g["innerWidth"] = cfg.widthDp.toDouble()
        g["innerHeight"] = cfg.heightDp.toDouble()
        g["devicePixelRatio"] = 1.0

        // 日夜间
        g["matchMedia"] = nf("matchMedia") { _, a ->
            val q = toStr(a.arg(0)).lowercase()
            val o = JsObject()
            o.props["media"] = q
            o.props["matches"] = if (q.contains("prefers-color-scheme")) (q.contains("dark") == dark) else false
            o.props["addEventListener"] = nf("addEventListener") { _, _ -> Undefined }
            o.props["removeEventListener"] = nf("removeEventListener") { _, _ -> Undefined }
            o.props["addListener"] = nf("addListener") { _, _ -> Undefined }
            o
        }

        // 查看最终样式
        g["getComputedStyle"] = nf("getComputedStyle") { _, a ->
            val e = a.arg(0)
            val o = JsObject()
            if (e is JsElement) {
                val map = computed().of(e.node)
                o.props["getPropertyValue"] = nf("getPropertyValue") { _, b -> map[normProp(toStr(b.arg(0)))] ?: "" }
                for ((k, v) in map) o.props[kebabToCamel(k)] = v
            }
            o
        }

        // 不能联网
        g["fetch"] = nf("fetch") { _, _ -> JsPromise(interp.makeError("TypeError", "组件里不能联网"), true) }

        // localStorage：直接存进组件存档
        for (storeName in listOf("localStorage", "sessionStorage")) {
            val st = JsObject()
            st.props["getItem"] = nf("getItem") { _, a ->
                val v = readState("ls:" + toStr(a.arg(0)))
                if (v is String) v else null
            }
            st.props["setItem"] = nf("setItem") { _, a ->
                putState("ls:" + toStr(a.arg(0)), toStr(a.arg(1)))
                Undefined
            }
            st.props["removeItem"] = nf("removeItem") { _, a ->
                state.remove("ls:" + toStr(a.arg(0)))
                markDirty()
                Undefined
            }
            st.props["clear"] = nf("clear") { _, _ ->
                state.keys.removeAll { it.startsWith("ls:") }
                markDirty()
                Undefined
            }
            g[storeName] = st
        }

        // 其它小工具
        g["structuredClone"] = nf("structuredClone") { _, a ->
            val s = jsonStringify(a.arg(0))
            if (s == null) Undefined else jsonParse(s)
        }
        g["encodeURIComponent"] = nf("encodeURIComponent") { _, a ->
            java.net.URLEncoder.encode(toStr(a.arg(0)), "UTF-8").replace("+", "%20")
        }
        g["decodeURIComponent"] = nf("decodeURIComponent") { _, a ->
            try {
                java.net.URLDecoder.decode(toStr(a.arg(0)).replace("+", "%2B"), "UTF-8")
            } catch (e: IllegalArgumentException) {
                throw JsThrow(JsError("URIError", "网址编码不对"))
            }
        }

        val win = JsGlobal(g)
        win.setProp("addEventListener", nf("addEventListener") { _, a ->
            addListener(docListeners, toStr(a.arg(0)), a.arg(1))
            Undefined
        })
        win.setProp("removeEventListener", nf("removeEventListener") { _, a ->
            val f = a.arg(1)
            if (f is JsFunction) docListeners[toStr(a.arg(0))]?.remove(f)
            Undefined
        })
        g["window"] = win
        g["self"] = win
        g["globalThis"] = win
        g["parent"] = win
        g["top"] = win
    }
}
