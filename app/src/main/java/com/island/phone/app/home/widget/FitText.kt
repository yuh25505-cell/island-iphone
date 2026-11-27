package com.island.phone.app.home.widget

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Stable
class FitScale {
    var value by mutableFloatStateOf(1f)
}

@Composable
fun rememberFitScale(key: Any): FitScale = remember(key) { FitScale() }

@Composable
fun FitText(
    text: String,
    fit: FitScale,
    size: Float,
    lineHeight: Float,
    color: Color,
    weight: FontWeight? = null,
) {
    val scale = fit.value
    BasicText(
        text = text.ifEmpty { " " },
        style = TextStyle(
            color = color,
            fontSize = (size * scale).sp,
            lineHeight = (lineHeight * scale).sp,
            fontWeight = weight,
        ),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { layout ->
            val need = layout.multiParagraph.maxIntrinsicWidth
            val have = layout.size.width.toFloat()
            if (need > have && need > 0f) {
                val next = (scale * have / need * 0.98f).coerceAtLeast(0.5f)
                if (next < fit.value) fit.value = next
            }
        },
    )
}
