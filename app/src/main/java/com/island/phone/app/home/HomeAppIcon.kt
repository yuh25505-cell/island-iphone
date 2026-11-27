package com.island.phone.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.app.home.widget.WidgetLightBackground
import com.island.phone.core.theme.IslandTheme

@Composable
fun HomeAppIcon(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    val colors = IslandTheme.colors
    val shape = RoundedCornerShape(size * 0.24f)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val borderColor = if (colors.isDark) colors.gray4 else WidgetLightBackground
    val fontSize = size.value * 0.215f

    Box(
        modifier
            .size(size)
            .alpha(if (pressed) 0.55f else 1f)
            .background(colors.groupedCard, shape)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = name,
            style = TextStyle(
                color = colors.label,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.2f).sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
