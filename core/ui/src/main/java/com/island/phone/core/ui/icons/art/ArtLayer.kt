package com.island.phone.core.ui.icons.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

class ArtLayer(
    private val d: String,
    private val c0: Long,
    private val c1: Long,
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    val space: Int = 256
) {
    val path: Path by lazy {
        PathParser().parsePathString(d).toPath().also { it.fillType = PathFillType.EvenOdd }
    }

    val brush: Brush by lazy {
        if (x0 == x1 && y0 == y1) {
            SolidColor(Color(c0))
        } else {
            Brush.linearGradient(
                colors = listOf(Color(c0), Color(c1)),
                start = Offset(x0.toFloat(), y0.toFloat()),
                end = Offset(x1.toFloat(), y1.toFloat())
            )
        }
    }
}

internal fun F(d: String, c: Long) = ArtLayer(d, c, c, 0, 0, 0, 0)

internal fun L(d: String, c0: Long, c1: Long, x0: Int, y0: Int, x1: Int, y1: Int) =
    ArtLayer(d, c0, c1, x0, y0, x1, y1)

// 512 坐标系的图层：第一层是图标外轮廓，后面的层都被它裁切
internal fun F5(d: String, c: Long) = ArtLayer(d, c, c, 0, 0, 0, 0, 512)

internal fun L5(d: String, c0: Long, c1: Long, x0: Int, y0: Int, x1: Int, y1: Int) =
    ArtLayer(d, c0, c1, x0, y0, x1, y1, 512)

fun DrawScope.drawArt(layers: List<ArtLayer>) {
    val space = layers.firstOrNull()?.space ?: 256
    scale(size.minDimension / space.toFloat(), pivot = Offset.Zero) {
        if (space == 512) {
            clipPath(layers[0].path) {
                for (layer in layers) {
                    drawPath(layer.path, layer.brush)
                }
            }
        } else {
            for (layer in layers) {
                drawPath(layer.path, layer.brush)
            }
        }
    }
}
