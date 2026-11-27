package com.island.phone.app.home.widget

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun WidgetText(
    text: String,
    size: Float,
    lineHeight: Float,
    color: Color,
    weight: FontWeight? = null,
) {
    BasicText(
        text = text.ifEmpty { " " },
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = weight,
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}
