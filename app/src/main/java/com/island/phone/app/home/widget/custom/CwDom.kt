package com.island.phone.app.home.widget.custom

internal class DomNode(val tag: String) {
    var text: String = ""
    val attrs = LinkedHashMap<String, String>()
    val style = LinkedHashMap<String, String>()
    val children = ArrayList<DomNode>()
    var parent: DomNode? = null
    val listeners = HashMap<String, ArrayList<JsFunction>>()
    var wrapper: Any? = null
    var checkedFlag: Boolean? = null
    var valueText: String? = null
    var pressed: Boolean = false

    val isText: Boolean get() = tag == "#text"

    fun classes(): List<String> =
        (attrs["class"] ?: "").split(' ', '\t', '\n', '\r').filter { it.isNotEmpty() }

    fun isChecked(): Boolean = checkedFlag ?: attrs.containsKey("checked")

    fun elementChildren(): List<DomNode> = children.filter { !it.isText }

    fun detach() {
        val p = parent
        if (p != null) p.children.remove(this)
        parent = null
    }

    fun append(c: DomNode) {
        c.detach()
        c.parent = this
        children.add(c)
    }

    fun insertAt(index: Int, c: DomNode) {
        c.detach()
        c.parent = this
        children.add(index.coerceIn(0, children.size), c)
    }

    fun textContent(): String = if (isText) text else children.joinToString("") { it.textContent() }

    fun setTextContent(t: String) {
        clearChildren()
        if (t.isNotEmpty()) {
            val n = DomNode("#text")
            n.text = t
            append(n)
        }
    }

    fun clearChildren() {
        for (c in children) c.parent = null
        children.clear()
    }

    fun walk(visit: (DomNode) -> Unit) {
        visit(this)
        for (c in children.toList()) c.walk(visit)
    }
}

internal class ParsedHtml(val root: DomNode, val styles: List<String>, val scripts: List<String>)

private val Entities = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0",
    "copy" to "©", "reg" to "®", "hellip" to "…", "mdash" to "—", "ndash" to "–", "times" to "×",
    "middot" to "·", "bull" to "•", "deg" to "°", "yen" to "¥", "euro" to "€", "pound" to "£",
    "laquo" to "«", "raquo" to "»", "larr" to "←", "rarr" to "→", "uarr" to "↑", "darr" to "↓",
    "hearts" to "♥", "star" to "☆", "check" to "✓",
)

private fun codePointText(code: Int): String? =
    if (code in 1..0x10FFFF) String(Character.toChars(code)) else null

internal fun decodeEntities(s: String): String {
    if (s.indexOf('&') < 0) return s
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '&') {
            val semi = s.indexOf(';', i + 1)
            if (semi >= i + 2 && semi <= i + 10) {
                val name = s.substring(i + 1, semi)
                val rep: String? = when {
                    name.startsWith("#x") || name.startsWith("#X") -> name.substring(2).toIntOrNull(16)?.let { codePointText(it) }
                    name.startsWith("#") -> name.substring(1).toIntOrNull()?.let { codePointText(it) }
                    else -> Entities[name]
                }
                if (rep != null) {
                    sb.append(rep)
                    i = semi + 1
                    continue
                }
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

private fun escapeText(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun escapeAttr(s: String): String =
    s.replace("&", "&amp;").replace("\"", "&quot;")

internal fun serializeNode(n: DomNode): String {
    if (n.isText) return escapeText(n.text)
    val sb = StringBuilder()
    sb.append('<').append(n.tag)
    for ((k, v) in n.attrs) {
        if (k == "style") continue
        sb.append(' ').append(k).append("=\"").append(escapeAttr(v)).append('"')
    }
    if (n.style.isNotEmpty()) {
        val css = n.style.entries.joinToString("; ") { it.key + ": " + it.value }
        sb.append(" style=\"").append(escapeAttr(css)).append('"')
    }
    sb.append('>')
    if (n.tag in HtmlParser.voids) return sb.toString()
    for (c in n.children) sb.append(serializeNode(c))
    sb.append("</").append(n.tag).append('>')
    return sb.toString()
}

private class TagInfo(val name: String, val attrs: LinkedHashMap<String, String>, val selfClose: Boolean, val end: Int)

internal object HtmlParser {
    val voids = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr")

    private fun isNameChar(c: Char): Boolean = c.isLetterOrDigit() || c == '-' || c == ':' || c == '_'

    private fun readTag(src: String, start: Int): TagInfo? {
        val n = src.length
        var i = start + 1
        val ns = i
        while (i < n && isNameChar(src[i])) i++
        val name = src.substring(ns, i).lowercase()
        val attrs = LinkedHashMap<String, String>()
        while (i < n) {
            while (i < n && src[i].isWhitespace()) i++
            if (i >= n) return null
            val c = src[i]
            if (c == '>') return TagInfo(name, attrs, false, i + 1)
            if (c == '/') {
                if (i + 1 < n && src[i + 1] == '>') return TagInfo(name, attrs, true, i + 2)
                i++
                continue
            }
            val ks = i
            while (i < n && !src[i].isWhitespace() && src[i] != '=' && src[i] != '>' && src[i] != '/') i++
            if (i == ks) {
                i++
                continue
            }
            val key = src.substring(ks, i).lowercase()
            while (i < n && src[i].isWhitespace()) i++
            var value = ""
            if (i < n && src[i] == '=') {
                i++
                while (i < n && src[i].isWhitespace()) i++
                if (i < n && (src[i] == '"' || src[i] == '\'')) {
                    val q = src[i]
                    val e = src.indexOf(q, i + 1)
                    if (e < 0) return null
                    value = decodeEntities(src.substring(i + 1, e))
                    i = e + 1
                } else {
                    val vs = i
                    while (i < n && !src[i].isWhitespace() && src[i] != '>') i++
                    value = decodeEntities(src.substring(vs, i))
                }
            }
            attrs[key] = value
        }
        return null
    }

    fun parse(src: String): ParsedHtml {
        val root = DomNode("#root")
        val styles = ArrayList<String>()
        val scripts = ArrayList<String>()
        var cur = root
        val n = src.length
        val text = StringBuilder()
        var i = 0

        fun flush() {
            if (text.isNotEmpty()) {
                val t = DomNode("#text")
                t.text = decodeEntities(text.toString())
                cur.append(t)
                text.setLength(0)
            }
        }

        while (i < n) {
            val c = src[i]
            if (c != '<') {
                text.append(c)
                i++
                continue
            }
            if (src.startsWith("<!--", i)) {
                val e = src.indexOf("-->", i + 4)
                flush()
                i = if (e < 0) n else e + 3
                continue
            }
            if (src.startsWith("<!", i) || src.startsWith("<?", i)) {
                val e = src.indexOf('>', i)
                flush()
                i = if (e < 0) n else e + 1
                continue
            }
            if (i + 1 < n && src[i + 1] == '/') {
                val e = src.indexOf('>', i)
                if (e < 0) {
                    text.append(c)
                    i++
                    continue
                }
                flush()
                val name = src.substring(i + 2, e).trim().lowercase()
                var p: DomNode? = cur
                while (p != null && p !== root && p.tag != name) p = p.parent
                if (p != null && p !== root) cur = p.parent ?: root
                i = e + 1
                continue
            }
            if (i + 1 < n && src[i + 1].isLetter()) {
                val info = readTag(src, i)
                if (info == null) {
                    text.append(c)
                    i++
                    continue
                }
                flush()
                val tag = info.name
                if (tag == "script" || tag == "style") {
                    val closeAt = src.indexOf("</$tag", info.end, true)
                    val bodyEnd = if (closeAt < 0) n else closeAt
                    val body = src.substring(info.end, bodyEnd)
                    if (tag == "script") scripts.add(body) else styles.add(body)
                    val gt = if (closeAt < 0) -1 else src.indexOf('>', closeAt)
                    i = if (gt < 0) n else gt + 1
                    continue
                }
                if ((tag == "li" && cur.tag == "li") || (tag == "option" && cur.tag == "option")) {
                    cur = cur.parent ?: root
                }
                val node = DomNode(tag)
                node.attrs.putAll(info.attrs)
                val inline = info.attrs["style"]
                if (inline != null) {
                    for (d in parseDeclarations(inline)) node.style[d.prop] = d.value
                }
                cur.append(node)
                if (tag !in voids && !info.selfClose) cur = node
                i = info.end
                continue
            }
            text.append(c)
            i++
        }
        flush()
        return ParsedHtml(root, styles, scripts)
    }
}
