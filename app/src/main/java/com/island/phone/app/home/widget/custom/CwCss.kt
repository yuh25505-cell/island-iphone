package com.island.phone.app.home.widget.custom

internal class AttrCond(val name: String, val op: String, val value: String)
internal class Pseudo(val name: String, val arg: String)
internal class Simple(
    val tag: String?,
    val id: String?,
    val classes: List<String>,
    val attrs: List<AttrCond>,
    val pseudos: List<Pseudo>,
    val nots: List<Simple>,
)
internal class Compound(val simple: Simple, val comb: Char)
internal class Selector(val parts: List<Compound>, val spec: Int)
internal class CssDecl(val prop: String, val value: String, val important: Boolean)
internal class CssRule(val selector: Selector, val decls: List<CssDecl>, val media: String?, val order: Int)
internal class KeyframeStep(val at: Float, val decls: Map<String, String>)
internal class Keyframes(val name: String, val steps: List<KeyframeStep>)
internal class Stylesheet(val rules: List<CssRule>, val keyframes: Map<String, Keyframes>) {
    companion object {
        val Empty = Stylesheet(emptyList(), emptyMap())
    }
}
internal class CssParseResult(val sheet: Stylesheet, val error: String?)

// ---------- 通用工具 ----------

internal fun splitTopLevel(s: String, sep: Char): List<String> {
    val out = ArrayList<String>()
    var depth = 0
    var quote = '\u0000'
    var start = 0
    for (i in s.indices) {
        val c = s[i]
        if (quote != '\u0000') {
            if (c == quote) quote = '\u0000'
            continue
        }
        if (c == '"' || c == '\'') {
            quote = c
        } else if (c == '(' || c == '[') {
            depth++
        } else if (c == ')' || c == ']') {
            if (depth > 0) depth--
        } else if (c == sep && depth == 0) {
            out.add(s.substring(start, i))
            start = i + 1
        }
    }
    out.add(s.substring(start))
    return out
}

// 按空格拆开，括号和引号里的空格不拆
internal fun splitSpaces(s: String): List<String> {
    val out = ArrayList<String>()
    var depth = 0
    var quote = '\u0000'
    val sb = StringBuilder()
    for (c in s) {
        if (quote != '\u0000') {
            sb.append(c)
            if (c == quote) quote = '\u0000'
            continue
        }
        if (c == '"' || c == '\'') {
            quote = c
            sb.append(c)
        } else if (c == '(') {
            depth++
            sb.append(c)
        } else if (c == ')') {
            if (depth > 0) depth--
            sb.append(c)
        } else if (c.isWhitespace() && depth == 0) {
            if (sb.isNotEmpty()) {
                out.add(sb.toString())
                sb.setLength(0)
            }
        } else {
            sb.append(c)
        }
    }
    if (sb.isNotEmpty()) out.add(sb.toString())
    return out
}

internal fun parseDeclarations(text: String): List<CssDecl> {
    val out = ArrayList<CssDecl>()
    for (piece in splitTopLevel(text, ';')) {
        val t = piece.trim()
        if (t.isEmpty()) continue
        val colon = t.indexOf(':')
        if (colon <= 0) continue
        var key = t.substring(0, colon).trim()
        if (!key.startsWith("--")) key = key.lowercase()
        var value = t.substring(colon + 1).trim()
        var imp = false
        val bang = value.lastIndexOf('!')
        if (bang >= 0 && value.substring(bang + 1).trim().equals("important", true)) {
            imp = true
            value = value.substring(0, bang).trim()
        }
        if (key.isNotEmpty() && value.isNotEmpty()) out.add(CssDecl(key, value, imp))
    }
    return out
}

// ---------- 选择器 ----------

private fun isIdentChar(c: Char): Boolean = c.isLetterOrDigit() || c == '-' || c == '_'

private fun readIdent(s: String, from: Int): Int {
    var i = from
    while (i < s.length && isIdentChar(s[i])) i++
    return i
}

private fun parseAttrCond(body: String): AttrCond? {
    val t = body.trim()
    if (t.isEmpty()) return null
    var opAt = -1
    var op = ""
    for (k in t.indices) {
        if (t[k] == '=') {
            if (k > 0 && t[k - 1] in "~^$*|") {
                opAt = k - 1
                op = t.substring(k - 1, k + 1)
            } else {
                opAt = k
                op = "="
            }
            break
        }
    }
    if (opAt < 0) return AttrCond(t.lowercase(), "", "")
    val name = t.substring(0, opAt).trim().lowercase()
    var v = t.substring(opAt + op.length).trim()
    if (v.length >= 2 && (v[0] == '"' || v[0] == '\'') && v.last() == v[0]) v = v.substring(1, v.length - 1)
    return AttrCond(name, op, v)
}

private fun parseCompound(s: String, from: Int, out: Array<Simple?>): Int {
    var i = from
    var tag: String? = null
    var id: String? = null
    val classes = ArrayList<String>()
    val attrs = ArrayList<AttrCond>()
    val pseudos = ArrayList<Pseudo>()
    val nots = ArrayList<Simple>()
    if (i < s.length && s[i] == '*') {
        i++
    } else if (i < s.length && isIdentChar(s[i])) {
        val e = readIdent(s, i)
        tag = s.substring(i, e).lowercase()
        i = e
    }
    while (i < s.length) {
        val ch = s[i]
        if (ch == '#') {
            val e = readIdent(s, i + 1)
            id = s.substring(i + 1, e)
            i = e
        } else if (ch == '.') {
            val e = readIdent(s, i + 1)
            classes.add(s.substring(i + 1, e))
            i = e
        } else if (ch == '[') {
            val e = s.indexOf(']', i)
            if (e < 0) return -1
            val cond = parseAttrCond(s.substring(i + 1, e)) ?: return -1
            attrs.add(cond)
            i = e + 1
        } else if (ch == ':') {
            if (s.startsWith("::", i)) return -1
            val e = readIdent(s, i + 1)
            val name = s.substring(i + 1, e).lowercase()
            i = e
            var arg = ""
            if (i < s.length && s[i] == '(') {
                var depth = 0
                var j = i
                while (j < s.length) {
                    if (s[j] == '(') depth++
                    if (s[j] == ')') {
                        depth--
                        if (depth == 0) break
                    }
                    j++
                }
                if (j >= s.length) return -1
                arg = s.substring(i + 1, j)
                i = j + 1
            }
            if (name == "not") {
                val inner = parseSelectorOne(arg)
                if (inner == null || inner.parts.size != 1) return -1
                nots.add(inner.parts[0].simple)
            } else {
                pseudos.add(Pseudo(name, arg.trim().lowercase()))
            }
        } else {
            break
        }
    }
    out[0] = Simple(tag, id, classes, attrs, pseudos, nots)
    return i
}

private fun specOf(sm: Simple): Int {
    var v = 0
    if (sm.id != null) v += 10000
    v += (sm.classes.size + sm.attrs.size + sm.pseudos.size) * 100
    if (sm.tag != null) v += 1
    for (n in sm.nots) v += specOf(n)
    return v
}

private fun parseSelectorOne(text: String): Selector? {
    val s = text.trim()
    if (s.isEmpty()) return null
    val parts = ArrayList<Compound>()
    var i = 0
    var comb = '\u0000'
    var spec = 0
    while (i < s.length) {
        while (i < s.length && s[i].isWhitespace()) i++
        if (i >= s.length) break
        val c = s[i]
        if (c == '>' || c == '+' || c == '~') {
            comb = c
            i++
            continue
        }
        if (parts.isNotEmpty() && comb == '\u0000') comb = ' '
        val box = arrayOfNulls<Simple>(1)
        val next = parseCompound(s, i, box)
        val sm = box[0]
        if (next < 0 || sm == null || next == i) return null
        parts.add(Compound(sm, if (parts.isEmpty()) '\u0000' else comb))
        spec += specOf(sm)
        comb = '\u0000'
        i = next
    }
    if (parts.isEmpty()) return null
    return Selector(parts, spec)
}

internal fun parseSelectorList(text: String): List<Selector> {
    val out = ArrayList<Selector>()
    for (piece in splitTopLevel(text, ',')) {
        val sel = parseSelectorOne(piece)
        if (sel != null) out.add(sel)
    }
    return out
}

private fun elementSiblings(n: DomNode): List<DomNode> {
    val p = n.parent ?: return listOf(n)
    return p.children.filter { !it.isText }
}

private fun prevElement(n: DomNode): DomNode? {
    val sibs = elementSiblings(n)
    val k = sibs.indexOf(n)
    return if (k > 0) sibs[k - 1] else null
}

private fun matchAttr(a: AttrCond, n: DomNode): Boolean {
    val v = n.attrs[a.name] ?: return false
    return when (a.op) {
        "" -> true
        "=" -> v == a.value
        "~=" -> v.split(' ').contains(a.value)
        "^=" -> a.value.isNotEmpty() && v.startsWith(a.value)
        "$=" -> a.value.isNotEmpty() && v.endsWith(a.value)
        "*=" -> a.value.isNotEmpty() && v.contains(a.value)
        "|=" -> v == a.value || v.startsWith(a.value + "-")
        else -> false
    }
}

private val NthRe = Regex("^([+-]?\\d*)n\\s*([+-]\\s*\\d+)?$")

private fun matchNth(arg: String, index1: Int): Boolean {
    var a = 0
    var b = 0
    when {
        arg == "odd" -> {
            a = 2
            b = 1
        }
        arg == "even" -> {
            a = 2
            b = 0
        }
        else -> {
            val m = NthRe.find(arg)
            if (m != null) {
                val ag = m.groupValues[1]
                a = if (ag.isEmpty() || ag == "+") 1 else if (ag == "-") -1 else ag.toInt()
                b = if (m.groupValues[2].isEmpty()) 0 else m.groupValues[2].replace(" ", "").toInt()
            } else {
                val v = arg.toIntOrNull() ?: return false
                a = 0
                b = v
            }
        }
    }
    if (a == 0) return index1 == b
    val diff = index1 - b
    return diff % a == 0 && diff / a >= 0
}

private fun matchPseudo(p: Pseudo, n: DomNode): Boolean {
    val sibs = elementSiblings(n)
    val idx = sibs.indexOf(n)
    return when (p.name) {
        "hover", "active" -> n.pressed
        "focus" -> false
        "checked" -> n.isChecked()
        "disabled" -> n.attrs.containsKey("disabled")
        "enabled" -> !n.attrs.containsKey("disabled")
        "first-child" -> idx == 0
        "last-child" -> idx == sibs.size - 1
        "only-child" -> sibs.size == 1
        "nth-child" -> matchNth(p.arg, idx + 1)
        "nth-last-child" -> matchNth(p.arg, sibs.size - idx)
        "empty" -> n.children.isEmpty()
        "root" -> n.parent?.tag == "#root"
        else -> false
    }
}

private fun matchSimple(sm: Simple, n: DomNode): Boolean {
    if (n.isText) return false
    if (sm.tag != null && sm.tag != n.tag) return false
    if (sm.id != null && n.attrs["id"] != sm.id) return false
    if (sm.classes.isNotEmpty()) {
        val cl = n.classes()
        for (c in sm.classes) if (c !in cl) return false
    }
    for (a in sm.attrs) if (!matchAttr(a, n)) return false
    for (p in sm.pseudos) if (!matchPseudo(p, n)) return false
    for (nt in sm.nots) if (matchSimple(nt, n)) return false
    return true
}

private fun matchParts(parts: List<Compound>, idx: Int, node: DomNode): Boolean {
    if (!matchSimple(parts[idx].simple, node)) return false
    if (idx == 0) return true
    when (parts[idx].comb) {
        '>' -> {
            val p = node.parent ?: return false
            return p.tag != "#root" && matchParts(parts, idx - 1, p)
        }
        ' ' -> {
            var p = node.parent
            while (p != null && p.tag != "#root") {
                if (matchParts(parts, idx - 1, p)) return true
                p = p.parent
            }
            return false
        }
        '+' -> {
            val prev = prevElement(node) ?: return false
            return matchParts(parts, idx - 1, prev)
        }
        '~' -> {
            var prev = prevElement(node)
            while (prev != null) {
                if (matchParts(parts, idx - 1, prev)) return true
                prev = prevElement(prev)
            }
            return false
        }
    }
    return false
}

internal fun selectorMatches(sel: Selector, node: DomNode): Boolean = matchParts(sel.parts, sel.parts.size - 1, node)

internal fun querySelectorAll(root: DomNode, selectorText: String, onlyFirst: Boolean): List<DomNode> {
    val sels = parseSelectorList(selectorText)
    val out = ArrayList<DomNode>()
    if (sels.isEmpty()) return out
    fun visit(n: DomNode) {
        if (onlyFirst && out.isNotEmpty()) return
        if (!n.isText && n.tag != "#root") {
            if (sels.any { selectorMatches(it, n) }) out.add(n)
        }
        for (c in n.children) visit(c)
    }
    for (c in root.children) visit(c)
    return out
}

// ---------- 样式表解析 ----------

private class CssCtx(val rules: ArrayList<CssRule>, val kfs: LinkedHashMap<String, Keyframes>) {
    var order = 0
}

private fun stripCssComments(raw: String): String? {
    val out = StringBuilder()
    var i = 0
    while (i < raw.length) {
        if (raw.startsWith("/*", i)) {
            val end = raw.indexOf("*/", i + 2)
            if (end < 0) return null
            for (k in i until end + 2) out.append(if (raw[k] == '\n') '\n' else ' ')
            i = end + 2
        } else {
            out.append(raw[i])
            i++
        }
    }
    return out.toString()
}

private fun matchBrace(text: String, open: Int): Int {
    var depth = 0
    var quote = '\u0000'
    var i = open
    while (i < text.length) {
        val c = text[i]
        if (quote != '\u0000') {
            if (c == quote) quote = '\u0000'
        } else if (c == '"' || c == '\'') {
            quote = c
        } else if (c == '{') {
            depth++
        } else if (c == '}') {
            depth--
            if (depth == 0) return i
        }
        i++
    }
    return -1
}

private fun mediaKind(prelude: String): String {
    val p = prelude.lowercase()
    if (p.contains("prefers-color-scheme")) {
        return if (p.contains("dark")) "dark" else if (p.contains("light")) "light" else "skip"
    }
    if (p.contains("print")) return "skip"
    return ""
}

private fun parseKeyframes(name: String, body: String, ctx: CssCtx) {
    val steps = ArrayList<KeyframeStep>()
    var i = 0
    while (i < body.length) {
        while (i < body.length && body[i].isWhitespace()) i++
        if (i >= body.length) break
        val brace = body.indexOf('{', i)
        if (brace < 0) break
        val end = matchBrace(body, brace)
        if (end < 0) break
        val decls = LinkedHashMap<String, String>()
        for (d in parseDeclarations(body.substring(brace + 1, end))) decls[d.prop] = d.value
        for (sel in body.substring(i, brace).split(',')) {
            val t = sel.trim().lowercase()
            val at = when {
                t == "from" -> 0f
                t == "to" -> 1f
                t.endsWith("%") -> (t.removeSuffix("%").toFloatOrNull() ?: -1f) / 100f
                else -> -1f
            }
            if (at >= 0f && at <= 1f) steps.add(KeyframeStep(at, decls))
        }
        i = end + 1
    }
    steps.sortBy { it.at }
    if (name.isNotEmpty() && steps.isNotEmpty()) ctx.kfs[name] = Keyframes(name, steps)
}

private fun lineOf(text: String, pos: Int): Int {
    var n = 1
    val end = pos.coerceIn(0, text.length)
    for (k in 0 until end) if (text[k] == '\n') n++
    return n
}

private fun parseRules(text: String, media: String?, ctx: CssCtx, lineBase: Int): String? {
    var i = 0
    while (i < text.length) {
        while (i < text.length && text[i].isWhitespace()) i++
        if (i >= text.length) break
        if (text[i] == '@') {
            var j = i + 1
            while (j < text.length && (text[j].isLetter() || text[j] == '-')) j++
            val name = text.substring(i + 1, j).lowercase()
            val brace = text.indexOf('{', j)
            val semi = text.indexOf(';', j)
            if (semi >= 0 && (brace < 0 || semi < brace)) {
                i = semi + 1
                continue
            }
            if (brace < 0) return "第${lineBase + lineOf(text, i) - 1}行：缺少 {"
            val end = matchBrace(text, brace)
            if (end < 0) return "第${lineBase + lineOf(text, brace) - 1}行：缺少 }"
            val prelude = text.substring(j, brace).trim()
            val body = text.substring(brace + 1, end)
            if (name == "media") {
                val kind = mediaKind(prelude)
                if (kind != "skip") {
                    val inner = if (kind.isEmpty()) media else kind
                    val err = parseRules(body, inner, ctx, lineBase + lineOf(text, brace) - 1)
                    if (err != null) return err
                }
            } else if (name == "keyframes" || name == "-webkit-keyframes") {
                parseKeyframes(prelude, body, ctx)
            }
            i = end + 1
            continue
        }
        val brace = text.indexOf('{', i)
        if (brace < 0) return "第${lineBase + lineOf(text, i) - 1}行：缺少 {"
        val end = matchBrace(text, brace)
        if (end < 0) return "第${lineBase + lineOf(text, brace) - 1}行：缺少 }"
        val decls = parseDeclarations(text.substring(brace + 1, end))
        for (sel in parseSelectorList(text.substring(i, brace))) {
            ctx.rules.add(CssRule(sel, decls, media, ctx.order))
            ctx.order++
        }
        i = end + 1
    }
    return null
}

internal fun parseStylesheet(raw: String): CssParseResult {
    val text = stripCssComments(raw) ?: return CssParseResult(Stylesheet.Empty, "注释没有写完，缺少 */")
    val ctx = CssCtx(ArrayList(), LinkedHashMap())
    val err = parseRules(text, null, ctx, 1)
    if (err != null) return CssParseResult(Stylesheet.Empty, err)
    return CssParseResult(Stylesheet(ctx.rules, ctx.kfs), null)
}

internal fun mergeStylesheets(list: List<Stylesheet>): Stylesheet {
    if (list.size == 1) return list[0]
    val rules = ArrayList<CssRule>()
    val kfs = LinkedHashMap<String, Keyframes>()
    var order = 0
    for (s in list) {
        for (r in s.rules) {
            rules.add(CssRule(r.selector, r.decls, r.media, order))
            order++
        }
        kfs.putAll(s.keyframes)
    }
    return Stylesheet(rules, kfs)
}

// ---------- 函数替换（var / light-dark） ----------

internal fun replaceFunc(value: String, fname: String, f: (List<String>) -> String): String {
    var s = value
    val key = fname + "("
    var guard = 0
    while (guard < 20) {
        guard++
        var at = -1
        var from = 0
        while (true) {
            val k = s.indexOf(key, from, true)
            if (k < 0) break
            if (k == 0 || !(isIdentChar(s[k - 1]))) {
                at = k
                break
            }
            from = k + 1
        }
        if (at < 0) break
        var depth = 0
        var j = at + fname.length
        var close = -1
        while (j < s.length) {
            if (s[j] == '(') depth++
            if (s[j] == ')') {
                depth--
                if (depth == 0) {
                    close = j
                    break
                }
            }
            j++
        }
        if (close < 0) break
        val inner = s.substring(at + key.length, close)
        val args = splitTopLevel(inner, ',').map { it.trim() }
        s = s.substring(0, at) + f(args) + s.substring(close + 1)
    }
    return s
}

// ---------- 级联计算 ----------

private val Inherited = setOf(
    "color", "font-family", "font-size", "font-weight", "font-style", "line-height", "letter-spacing",
    "text-align", "white-space", "text-shadow", "visibility", "cursor", "word-break", "text-transform",
)

private val Sides = listOf("top", "right", "bottom", "left")

private fun box4(tokens: List<String>): List<String> = when (tokens.size) {
    1 -> listOf(tokens[0], tokens[0], tokens[0], tokens[0])
    2 -> listOf(tokens[0], tokens[1], tokens[0], tokens[1])
    3 -> listOf(tokens[0], tokens[1], tokens[2], tokens[1])
    else -> listOf(tokens[0], tokens[1], tokens[2], tokens[3])
}

private val BorderStyles = setOf("none", "solid", "dashed", "dotted", "double", "hidden", "groove", "ridge", "inset", "outset")

private fun isLengthToken(t: String): Boolean {
    val v = t.lowercase()
    if (v == "thin" || v == "medium" || v == "thick" || v == "0") return true
    return v.firstOrNull()?.let { it.isDigit() || it == '.' || it == '-' || it == '+' } == true
}

private fun putBorderSide(out: MutableMap<String, String>, side: String, value: String) {
    var w = "medium"
    var st = "solid"
    var col = "currentcolor"
    var none = false
    for (t in splitSpaces(value)) {
        val lt = t.lowercase()
        if (lt in BorderStyles) {
            st = lt
            if (lt == "none" || lt == "hidden") none = true
        } else if (isLengthToken(lt)) {
            w = t
        } else {
            col = t
        }
    }
    out["border-$side-width"] = if (none) "0px" else w
    out["border-$side-style"] = st
    out["border-$side-color"] = col
}

private fun expandDecl(out: MutableMap<String, String>, prop: String, value: String) {
    val toks = splitSpaces(value)
    when (prop) {
        "margin", "padding" -> if (toks.isNotEmpty() && toks.size <= 4) {
            val b = box4(toks)
            for (k in 0 until 4) out[prop + "-" + Sides[k]] = b[k]
        }
        "inset" -> if (toks.isNotEmpty() && toks.size <= 4) {
            val b = box4(toks)
            for (k in 0 until 4) out[Sides[k]] = b[k]
        }
        "border" -> for (s in Sides) putBorderSide(out, s, value)
        "border-top", "border-right", "border-bottom", "border-left" -> putBorderSide(out, prop.removePrefix("border-"), value)
        "border-width", "border-color", "border-style" -> if (toks.isNotEmpty() && toks.size <= 4) {
            val b = box4(toks)
            val suffix = prop.removePrefix("border-")
            for (k in 0 until 4) out["border-" + Sides[k] + "-" + suffix] = b[k]
        }
        "border-radius" -> {
            val first = value.substringBefore('/').trim()
            val t = splitSpaces(first)
            if (t.isNotEmpty() && t.size <= 4) {
                val b = box4(t)
                out["border-top-left-radius"] = b[0]
                out["border-top-right-radius"] = b[1]
                out["border-bottom-right-radius"] = b[2]
                out["border-bottom-left-radius"] = b[3]
            }
        }
        "gap" -> if (toks.isNotEmpty()) {
            out["row-gap"] = toks[0]
            out["column-gap"] = if (toks.size > 1) toks[1] else toks[0]
        }
        "overflow" -> if (toks.isNotEmpty()) {
            out["overflow-x"] = toks[0]
            out["overflow-y"] = if (toks.size > 1) toks[1] else toks[0]
        }
        "flex" -> expandFlex(out, value.lowercase(), toks)
        "flex-flow" -> for (t in toks) {
            val lt = t.lowercase()
            if (lt.startsWith("row") || lt.startsWith("column")) out["flex-direction"] = lt
            if (lt == "wrap" || lt == "nowrap" || lt == "wrap-reverse") out["flex-wrap"] = lt
        }
        "place-items" -> if (toks.isNotEmpty()) {
            out["align-items"] = toks[0]
            out["justify-items"] = if (toks.size > 1) toks[1] else toks[0]
        }
        "place-content" -> if (toks.isNotEmpty()) {
            out["align-content"] = toks[0]
            out["justify-content"] = if (toks.size > 1) toks[1] else toks[0]
        }
        "background" -> expandBackground(out, value)
        "font" -> expandFont(out, toks)
        else -> out[prop] = value
    }
}

private fun expandFlex(out: MutableMap<String, String>, lower: String, toks: List<String>) {
    when {
        lower == "none" -> {
            out["flex-grow"] = "0"
            out["flex-shrink"] = "0"
            out["flex-basis"] = "auto"
        }
        lower == "auto" -> {
            out["flex-grow"] = "1"
            out["flex-shrink"] = "1"
            out["flex-basis"] = "auto"
        }
        toks.size == 1 && toks[0].toFloatOrNull() != null -> {
            out["flex-grow"] = toks[0]
            out["flex-shrink"] = "1"
            out["flex-basis"] = "0px"
        }
        toks.size == 1 -> {
            out["flex-grow"] = "1"
            out["flex-shrink"] = "1"
            out["flex-basis"] = toks[0]
        }
        toks.size >= 2 -> {
            out["flex-grow"] = toks[0]
            val second = toks[1]
            if (second.toFloatOrNull() != null) {
                out["flex-shrink"] = second
                out["flex-basis"] = if (toks.size > 2) toks[2] else "0px"
            } else {
                out["flex-shrink"] = "1"
                out["flex-basis"] = second
            }
        }
    }
}

private val RepeatWords = setOf("repeat", "no-repeat", "repeat-x", "repeat-y", "space", "round")
private val PosWords = setOf("left", "right", "top", "bottom", "center")

private fun expandBackground(out: MutableMap<String, String>, value: String) {
    val layers = splitTopLevel(value, ',')
    val first = layers[0].trim()
    out["background-color"] = "transparent"
    out.remove("background-image")
    var posTokens = ArrayList<String>()
    var sizeTokens = ArrayList<String>()
    var afterSlash = false
    for (t in splitSpaces(first)) {
        val lt = t.lowercase()
        when {
            lt == "/" -> afterSlash = true
            lt.startsWith("url(") || lt.contains("gradient(") -> out["background-image"] = t
            lt == "none" -> {}
            lt in RepeatWords -> out["background-repeat"] = lt
            lt == "cover" || lt == "contain" || lt == "auto" -> sizeTokens.add(lt)
            lt in PosWords -> if (afterSlash) sizeTokens.add(lt) else posTokens.add(lt)
            lt.startsWith("/") -> {
                afterSlash = true
                val rest = t.substring(1)
                if (rest.isNotEmpty()) sizeTokens.add(rest)
            }
            lt.endsWith("/") -> {
                posTokens.add(t.dropLast(1))
                afterSlash = true
            }
            lt.contains('/') && !lt.startsWith("rgb") && !lt.startsWith("hsl") && !lt.startsWith("#") -> {
                posTokens.add(t.substringBefore('/'))
                sizeTokens.add(t.substringAfter('/'))
                afterSlash = true
            }
            isLengthToken(lt) && !lt.startsWith("#") -> if (afterSlash) sizeTokens.add(t) else posTokens.add(t)
            else -> out["background-color"] = t
        }
    }
    if (posTokens.isNotEmpty()) out["background-position"] = posTokens.joinToString(" ")
    if (sizeTokens.isNotEmpty()) out["background-size"] = sizeTokens.joinToString(" ")
    if (layers.size > 1) {
        val last = splitSpaces(layers.last().trim()).lastOrNull()
        if (last != null && !last.lowercase().startsWith("url(") && !last.lowercase().contains("gradient(")) {
            out["background-color"] = last
        }
    }
}

private fun expandFont(out: MutableMap<String, String>, toks: List<String>) {
    var i = 0
    while (i < toks.size) {
        val lt = toks[i].lowercase()
        if (lt == "italic" || lt == "oblique") {
            out["font-style"] = lt
        } else if (lt == "normal") {
            // 忽略
        } else if (lt == "bold" || lt == "bolder" || lt == "lighter" || (lt.toIntOrNull() != null && lt.length == 3)) {
            out["font-weight"] = lt
        } else {
            break
        }
        i++
    }
    if (i < toks.size) {
        val sizePart = toks[i]
        if (sizePart.contains('/')) {
            out["font-size"] = sizePart.substringBefore('/')
            out["line-height"] = sizePart.substringAfter('/')
        } else {
            out["font-size"] = sizePart
        }
        i++
    }
    if (i < toks.size) out["font-family"] = toks.subList(i, toks.size).joinToString(" ")
}

private fun fontPx(value: String, parentPx: Float): Float {
    val v = value.trim().lowercase()
    val num = v.takeWhile { it.isDigit() || it == '.' || it == '-' }.toFloatOrNull()
    return when {
        v == "xx-small" -> 9f
        v == "x-small" -> 10f
        v == "small" -> 13f
        v == "medium" -> 16f
        v == "large" -> 18f
        v == "x-large" -> 24f
        v == "xx-large" -> 32f
        v == "smaller" -> parentPx * 0.83f
        v == "larger" -> parentPx * 1.2f
        num == null -> parentPx
        v.endsWith("px") -> num
        v.endsWith("rem") -> num * 16f
        v.endsWith("em") -> num * parentPx
        v.endsWith("%") -> parentPx * num / 100f
        v.endsWith("pt") -> num * 4f / 3f
        v.endsWith("vw") || v.endsWith("vh") -> num
        else -> num
    }
}

internal class ComputedTree(val map: HashMap<DomNode, HashMap<String, String>>) {
    fun of(n: DomNode): Map<String, String> = map[n] ?: emptyMap()
}

private class Entry(val imp: Int, val spec: Int, val order: Int, val prop: String, val value: String)

private fun resolveVars(value: String, custom: Map<String, String>): String {
    if (!value.contains("var(", true)) return value
    var v = value
    for (pass in 0 until 6) {
        v = replaceFunc(v, "var") { args ->
            val name = args.getOrElse(0) { "" }
            custom[name] ?: (if (args.size > 1) args.drop(1).joinToString(",") else "")
        }
        if (!v.contains("var(", true)) break
    }
    return v
}

private fun resolveLightDark(value: String, dark: Boolean): String {
    if (!value.contains("light-dark(", true)) return value
    return replaceFunc(value, "light-dark") { args ->
        if (args.size >= 2) (if (dark) args[1] else args[0]) else ""
    }
}

internal fun computeStyles(root: DomNode, sheet: Stylesheet, dark: Boolean): ComputedTree {
    val result = HashMap<DomNode, HashMap<String, String>>()
    val rules = sheet.rules.filter { it.media == null || (it.media == "dark") == dark }
    val rootDefaults = HashMap<String, String>()
    rootDefaults["font-size"] = "16px"
    rootDefaults["color"] = "#000000"

    fun visit(n: DomNode, parent: Map<String, String>) {
        if (n.isText) return
        val entries = ArrayList<Entry>()
        for (r in rules) {
            if (selectorMatches(r.selector, n)) {
                for (d in r.decls) entries.add(Entry(if (d.important) 1 else 0, r.selector.spec, r.order, d.prop, d.value))
            }
        }
        for ((k, v) in n.style) entries.add(Entry(0, 1_000_000, 0, k, v))
        entries.sortWith(compareBy({ it.imp }, { it.spec }, { it.order }))

        val custom = HashMap<String, String>()
        for ((k, v) in parent) if (k.startsWith("--")) custom[k] = v
        for (e in entries) {
            if (e.prop.startsWith("--")) custom[e.prop] = resolveVars(e.value, custom)
        }

        val own = HashMap<String, String>()
        for (e in entries) {
            if (e.prop.startsWith("--")) continue
            val v = resolveLightDark(resolveVars(e.value, custom), dark).trim()
            if (v.isEmpty()) continue
            val lv = v.lowercase()
            if (lv == "inherit") {
                val pv = parent[e.prop]
                if (pv != null) own[e.prop] = pv
                continue
            }
            if (lv == "initial" || lv == "unset") {
                own.remove(e.prop)
                continue
            }
            expandDecl(own, e.prop, v)
        }

        val parentPx = parent["font-size"]?.removeSuffix("px")?.toFloatOrNull() ?: 16f
        val full = HashMap<String, String>()
        for (k in Inherited) {
            val pv = parent[k]
            if (pv != null) full[k] = pv
        }
        for ((k, v) in custom) full[k] = v
        for ((k, v) in own) full[k] = v
        val fs = own["font-size"]
        full["font-size"] = if (fs != null) {
            val px = fontPx(fs, parentPx)
            numToPx(px)
        } else {
            numToPx(parentPx)
        }
        if (n.tag == "b" || n.tag == "strong" || (n.tag.length == 2 && n.tag[0] == 'h' && n.tag[1] in '1'..'6')) {
            if (own["font-weight"] == null) full["font-weight"] = "bold"
        }
        if (n.tag == "i" || n.tag == "em") {
            if (own["font-style"] == null) full["font-style"] = "italic"
        }
        result[n] = full
        for (c in n.children) visit(c, full)
    }

    for (c in root.children) visit(c, rootDefaults)
    return ComputedTree(result)
}

private fun numToPx(v: Float): String {
    val r = Math.round(v * 100f) / 100f
    return if (r == Math.floor(r.toDouble()).toFloat()) r.toInt().toString() + "px" else r.toString() + "px"
}
