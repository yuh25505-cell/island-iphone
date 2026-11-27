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
    val fit = rememberFitScale(lines)
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
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = (12 * k).dp,
                    bottom = (20 * k).dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                text = "“",
                style = TextStyle(
                    color = colors.label.copy(alpha = 0.5f),
                    fontSize = (34 * k).sp,
                    lineHeight = (34 * k).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy((5 * k).dp)) {
                lines.forEachIndexed { index, text ->
                    FitText(
                        text = text,
                        fit = fit,
                        size = 14f * k,
                        lineHeight = 17f * k,
                        color = colors.label.copy(alpha = quoteAlphas.getOrElse(index) { 0.3f }),
                        weight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
