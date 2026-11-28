package com.island.phone.app.home.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

internal val StyleProps = setOf("color", "background", "size", "weight", "radius", "opacity")

@Immutable
class WidgetStyle internal constructor(private val rules: Map<String, Map<String, String>>) {
    internal fun find(classes: List<String>, key: String, dark: Boolean): String? {
        var found: String? = null
        for (name in classes) {
            val rule = rules[name] ?: continue
            val themed = if (dark) rule["$key-dark"] else rule["$key-light"]
            val value = themed ?: rule[key]
            if (value != null) found = value
        }
        return found
    }

    companion object {
        val Empty = WidgetStyle(emptyMap())

        fun parse(text: String): WidgetStyle {
            val result = parseWidgetStyle(text)
            return if (result.error == null) WidgetStyle(result.rules) else Empty
        }
    }
}

internal class StyleParseResult(val rules: Map<String, Map<String, String>>, val error: String?)

private fun failStyle(message: String) = StyleParseResult(emptyMap(), message)

private fun stripStyleComments(raw: String): String? {
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

private fun validStyleName(name: String): Boolean =
    name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '-' || it == '_' }

internal fun parseWidgetStyle(raw: String): StyleParseResult {
    val text = stripStyleComments(raw) ?: return failStyle("注释没有写完，缺少 */")
    val rules = LinkedHashMap<String, MutableMap<String, String>>()
    fun lineAt(pos: Int): Int = text.substring(0, pos.coerceIn(0, text.length)).count { it == '\n' } + 1
    var i = 0
    while (i < text.length) {
        while (i < text.length && text[i].isWhitespace()) i++
        if (i >= text.length) break
        val open = text.indexOf('{', i)
        if (open < 0) return failStyle("第${lineAt(i)}行：缺少 {")
        val close = text.indexOf('}', open)
        if (close < 0) return failStyle("第${lineAt(open)}行：缺少 }")
        val names = text.substring(i, open).split(",").map { it.trim().removePrefix(".") }.filter { it.isNotEmpty() }
        if (names.isEmpty() || names.any { !validStyleName(it) }) {
            return failStyle("第${lineAt(i)}行：名字写得不对")
        }
        val body = text.substring(open + 1, close)
        if (body.contains('{')) return failStyle("第${lineAt(open)}行：{ 多了")
        val parsed = LinkedHashMap<String, String>()
        var offset = open + 1
        for (piece in body.split(";")) {
            val trimmed = piece.trim()
            if (trimmed.isNotEmpty()) {
                val at = offset + (piece.length - piece.trimStart().length)
                val colon = trimmed.indexOf(':')
                if (colon <= 0) return failStyle("第${lineAt(at)}行：「$trimmed」缺少冒号")
                val key = trimmed.substring(0, colon).trim().lowercase()
                val value = trimmed.substring(colon + 1).trim()
                val problem = checkStyleDeclaration(key, value)
                if (problem != null) return failStyle("第${lineAt(at)}行：$problem")
                parsed[key] = value
            }
            offset += piece.length + 1
        }
        names.forEach { n -> rules.getOrPut(n) { LinkedHashMap() }.putAll(parsed) }
        i = close + 1
    }
    return StyleParseResult(rules, null)
}

private fun checkStyleDeclaration(key: String, value: String): String? {
    val base = key.removeSuffix("-dark").removeSuffix("-light")
    if (base !in StyleProps) return "不认识「$key」"
    val ok = when (base) {
        "color", "background" -> parseStyleColor(value) != null
        "size" -> parseStyleNumber(value)?.let { it in 1f..200f } == true
        "weight" -> parseStyleWeight(value) != null
        "radius" -> parseStyleNumber(value)?.let { it in 0f..200f } == true
        "opacity" -> parseStyleNumber(value)?.let { it in 0f..1f } == true
        else -> false
    }
    return if (ok) null else "「$key」的值「$value」不对"
}

internal fun parseStyleNumber(raw: String): Float? =
    raw.trim().lowercase().removeSuffix("sp").removeSuffix("dp").removeSuffix("px").trim().toFloatOrNull()

internal fun parseStyleColor(raw: String): Color? {
    val v = raw.trim().lowercase()
    if (v == "transparent") return Color.Transparent
    if (!v.startsWith("#")) return null
    val hex = v.substring(1)
    if (hex.isEmpty() || !hex.all { it in '0'..'9' || it in 'a'..'f' }) return null
    val full = when (hex.length) {
        3 -> "ff" + hex.map { "$it$it" }.joinToString("")
        6 -> "ff$hex"
        8 -> hex
        else -> return null
    }
    return Color(full.toLong(16))
}

internal fun parseStyleWeight(raw: String): FontWeight? = when (raw.trim().lowercase()) {
    "thin" -> FontWeight.Thin
    "light" -> FontWeight.Light
    "normal" -> FontWeight.Normal
    "medium" -> FontWeight.Medium
    "semibold" -> FontWeight.SemiBold
    "bold" -> FontWeight.Bold
    "black" -> FontWeight.Black
    else -> raw.trim().toIntOrNull()?.takeIf { it in 100..900 && it % 100 == 0 }?.let { FontWeight(it) }
}

internal val LocalWidgetStyle = staticCompositionLocalOf { WidgetStyle.Empty }

@Composable
internal fun wsColor(base: Color, classes: List<String>, key: String = "color"): Color {
    val style = LocalWidgetStyle.current
    val dark = IslandTheme.colors.isDark
    var c = style.find(classes, key, dark)?.let { parseStyleColor(it) } ?: base
    val o = style.find(classes, "opacity", dark)?.let { parseStyleNumber(it) }
    if (o != null) c = c.copy(alpha = c.alpha * o)
    return c
}

@Composable
internal fun wsBackground(base: Color, classes: List<String>): Color = wsColor(base, classes, "background")

@Composable
internal fun wsShape(classes: List<String>, default: Shape): Shape {
    val style = LocalWidgetStyle.current
    val dark = IslandTheme.colors.isDark
    val r = style.find(classes, "radius", dark)?.let { parseStyleNumber(it) }
    return if (r != null) RoundedCornerShape(r.dp) else default
}

@Composable
internal fun wsText(base: TextStyle, classes: List<String>): TextStyle {
    val style = LocalWidgetStyle.current
    val dark = IslandTheme.colors.isDark
    var out = base.copy(color = wsColor(base.color, classes))
    val size = style.find(classes, "size", dark)?.let { parseStyleNumber(it) }
    if (size != null) {
        var lineHeight = base.lineHeight
        if (base.fontSize.isSp && lineHeight.isSp && base.fontSize.value > 0f) {
            lineHeight = (lineHeight.value * size / base.fontSize.value).sp
        }
        out = out.copy(fontSize = size.sp, lineHeight = lineHeight)
    }
    val weight = style.find(classes, "weight", dark)?.let { parseStyleWeight(it) }
    if (weight != null) out = out.copy(fontWeight = weight)
    return out
}
