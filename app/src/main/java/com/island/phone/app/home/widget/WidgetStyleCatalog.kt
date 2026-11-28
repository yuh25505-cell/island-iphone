package com.island.phone.app.home.widget

import com.island.phone.app.home.HomeWidgetKind

private const val PackMark = "island-widget:v1"
private const val PackMaxLength = 100_000
private val PackHeader = Regex("""/\*\s*island-widget:v1\s+kind=([A-Za-z]+)\s*\*/""")

internal fun widgetKindLabel(kind: HomeWidgetKind): String = when (kind) {
    HomeWidgetKind.DATE -> "日期"
    HomeWidgetKind.QUOTE -> "语录"
    HomeWidgetKind.WORD -> "单词"
    HomeWidgetKind.WEEK -> "周历"
    HomeWidgetKind.PHOTOS -> "相片"
}

internal fun widgetClassNames(kind: HomeWidgetKind): List<String> = when (kind) {
    HomeWidgetKind.DATE -> listOf(
        "date-card", "date-day", "date-month", "date-weekday", "date-divider",
        "date-line", "date-line-1", "date-line-2", "date-line-3", "date-line-4",
    )
    HomeWidgetKind.QUOTE -> listOf(
        "quote-card", "quote-mark",
        "quote-line", "quote-line-1", "quote-line-2", "quote-line-3", "quote-line-4",
    )
    HomeWidgetKind.WORD -> listOf(
        "word-card", "word-title", "word-word", "word-meaning", "word-divider",
        "word-example-title", "word-example", "word-example-zh", "word-button", "word-button-text",
    )
    HomeWidgetKind.WEEK -> listOf(
        "week-avatar", "week-pill", "week-pill-text", "week-bar", "week-name",
        "week-days", "week-day", "week-today",
    )
    HomeWidgetKind.PHOTOS -> listOf(
        "photo-card", "photo-polaroid",
        "photo-caption", "photo-caption-1", "photo-caption-2", "photo-caption-3", "photo-caption-4",
    )
}

internal fun widgetStyleTemplate(kind: HomeWidgetKind): String {
    val out = StringBuilder()
    out.append("/* color background size weight radius opacity\n")
    out.append("   夜间在后面加 -dark，例如 color-dark: #ffffff */\n\n")
    widgetClassNames(kind).forEach { name -> out.append('.').append(name).append(" {\n}\n\n") }
    return out.toString()
}

internal fun encodeWidgetPack(kind: HomeWidgetKind, css: String): String =
    "/* $PackMark kind=${kind.name} */\n" + css.replace(PackHeader, "").trim() + "\n"

internal class WidgetPackResult(val css: String, val error: String?)

internal fun decodeWidgetPack(kind: HomeWidgetKind, text: String): WidgetPackResult {
    if (text.isBlank()) return WidgetPackResult("", "内容是空的")
    if (text.length > PackMaxLength) return WidgetPackResult("", "内容太长了")
    val header = PackHeader.find(text)
    if (header != null) {
        val name = header.groupValues[1]
        val from = HomeWidgetKind.values().firstOrNull { it.name == name }
            ?: return WidgetPackResult("", "不认识这份样式的组件类型")
        if (from != kind) {
            return WidgetPackResult("", "这份样式是给「${widgetKindLabel(from)}」做的，不能用在「${widgetKindLabel(kind)}」上")
        }
    }
    val css = text.replace(PackHeader, "").trim()
    val parsed = parseWidgetStyle(css)
    if (parsed.error != null) return WidgetPackResult("", parsed.error)
    return WidgetPackResult(css, null)
}
