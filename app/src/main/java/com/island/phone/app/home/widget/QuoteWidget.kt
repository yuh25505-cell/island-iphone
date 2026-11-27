package com.island.phone.app.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

private val quoteAlphas = listOf(0.78f, 0.3f, 0.66f, 0.3f)

@Composable
fun QuoteWidget(
    lines: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IslandTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .widgetTap(onClick)
            .background(colors.gray5, RoundedCornerShape(30.dp)),
    ) {
        val k = (maxHeight.value / 166f).coerceIn(0.78f, 1f)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = (14 * k).dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                text = "“",
                style = TextStyle(
                    color = colors.label.copy(alpha = 0.5f),
                    fontSize = (44 * k).sp,
                    lineHeight = (44 * k).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy((7 * k).dp)) {
                lines.forEachIndexed { index, text ->
                    BasicText(
                        text = text.ifEmpty { " " },
                        style = TextStyle(
                            color = colors.label.copy(alpha = quoteAlphas.getOrElse(index) { 0.3f }),
                            fontSize = (14 * k).sp,
                            lineHeight = (18 * k).sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
