package com.island.phone.core.ui.theme

import androidx.compose.ui.graphics.Color

data class GlassTokens(
    val tint: Color,
    val tintFallback: Color,
    val highlight: Color,
    val rimLight: Color,
    val rimDark: Color,
    val shadow: Color,
    val innerShade: Color = Color(0x1F000000),
    val edgeDark: Color = Color(0x4D000000),
    val ambient: Color = Color(0x66FFFFFF),
)

data class WallpaperTokens(
    val top: Color,
    val bottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
)

data class IconTokens(
    val glyph: Color,
    val glyphShadow: Color,
    val tileTint: Color,
    val label: Color,
    val labelShadow: Color,
)

data class PageDotTokens(
    val active: Color,
    val inactive: Color,
)

data class FolderTokens(
    val scrim: Color,
    val panelFill: Color,
    val title: Color,
    val cursor: Color,
    val miniTile: Color,
)

data class WidgetTokens(
    val clockText: Color,
    val ringGreen: Color,
    val ringRed: Color,
    val ringTrack: Color,
)

val DayGlass = GlassTokens(
    tint = Color(0x1FFFFFFF),
    tintFallback = Color(0x66FFFFFF),
    highlight = Color(0x40FFFFFF),
    rimLight = Color(0xD9FFFFFF),
    rimDark = Color(0x26FFFFFF),
    shadow = Color(0x33000000),
    innerShade = Color(0x22203040),
    edgeDark = Color(0x4D000000),
    ambient = Color(0x66FFFFFF),
)

val NightGlass = GlassTokens(
    tint = Color(0x24FFFFFF),
    tintFallback = Color(0x40FFFFFF),
    highlight = Color(0x24FFFFFF),
    rimLight = Color(0x80FFFFFF),
    rimDark = Color(0x14FFFFFF),
    shadow = Color(0x66000000),
    innerShade = Color(0x33000000),
    edgeDark = Color(0x4D000000),
    ambient = Color(0x4DFFFFFF),
)

val DayWallpaper = WallpaperTokens(
    top = Color(0xFFE4E5E8),
    bottom = Color(0xFFE4E5E8),
    blobA = Color(0xFFE4E5E8),
    blobB = Color(0xFFE4E5E8),
    blobC = Color(0xFFE4E5E8),
)

val NightWallpaper = WallpaperTokens(
    top = Color(0xFF2C2D31),
    bottom = Color(0xFF2C2D31),
    blobA = Color(0xFF2C2D31),
    blobB = Color(0xFF2C2D31),
    blobC = Color(0xFF2C2D31),
)

val DayIcon = IconTokens(
    glyph = Color(0xF5FFFFFF),
    glyphShadow = Color(0x33203040),
    tileTint = Color(0x14FFFFFF),
    label = Color(0xFF2B2F38),
    labelShadow = Color(0x00000000),
)

val NightIcon = IconTokens(
    glyph = Color(0xF2FFFFFF),
    glyphShadow = Color(0x66000000),
    tileTint = Color(0x12FFFFFF),
    label = Color(0xFFFFFFFF),
    labelShadow = Color(0x66000000),
)

val DayDots = PageDotTokens(
    active = Color(0xF2FFFFFF),
    inactive = Color(0x73FFFFFF),
)

val NightDots = PageDotTokens(
    active = Color(0xF2FFFFFF),
    inactive = Color(0x66FFFFFF),
)

val DayFolder = FolderTokens(
    scrim = Color(0x40000000),
    panelFill = Color(0x59FFFFFF),
    title = Color(0xFF2B2F38),
    cursor = Color(0xFF2B2F38),
    miniTile = Color(0x2E5A6C8A),
)

val NightFolder = FolderTokens(
    scrim = Color(0x80000000),
    panelFill = Color(0x331B2540),
    title = Color(0xFFFFFFFF),
    cursor = Color(0xFFFFFFFF),
    miniTile = Color(0x1FFFFFFF),
)

val DayWidget = WidgetTokens(
    clockText = Color(0xD9505763),
    ringGreen = Color(0xFF34C759),
    ringRed = Color(0xFFFF3B30),
    ringTrack = Color(0x26000000),
)

val NightWidget = WidgetTokens(
    clockText = Color(0xE6FFFFFF),
    ringGreen = Color(0xFF30D158),
    ringRed = Color(0xFFFF453A),
    ringTrack = Color(0x40FFFFFF),
)
