package com.island.phone.app.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import com.island.phone.ui.image.SourceImage

@Immutable
private class PhotoSlot(val x: Float, val y: Float)

private val photoSlots = listOf(
    PhotoSlot(x = 0.059f, y = 0.14f),
    PhotoSlot(x = 0.270f, y = 0.24f),
    PhotoSlot(x = 0.487f, y = 0.12f),
    PhotoSlot(x = 0.700f, y = 0.215f),
)

@Composable
fun PhotoWidget(
    sources: List<String>,
    captions: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IslandTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .widgetTap(onClick)
            .background(widgetBackground(colors.gray6), RoundedCornerShape(30.dp)),
    ) {
        val cardW = maxWidth * 0.238f
        val cardH = maxHeight * 0.68f
        photoSlots.forEachIndexed { index, slot ->
            PhotoCard(
                caption = captions.getOrElse(index) { "" },
                source = sources.getOrElse(index) { "" },
                modifier = Modifier
                    .offset(x = maxWidth * slot.x, y = maxHeight * slot.y)
                    .size(width = cardW, height = cardH),
            )
        }
    }
}

@Composable
private fun PhotoCard(caption: String, source: String, modifier: Modifier) {
    val colors = IslandTheme.colors
    val shape = RoundedCornerShape(3.dp)
    val borderColor = if (colors.isDark) colors.gray4 else colors.gray5
    Column(
        modifier
            .background(colors.groupedCard, shape)
            .border(0.5.dp, borderColor, shape)
            .padding(start = 5.dp, end = 5.dp, top = 5.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.76f)
                .background(colors.gray4),
        ) {
            if (source.isNotBlank()) SourceImage(source, Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxWidth().weight(0.24f),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = caption,
                style = TextStyle(
                    color = colors.label,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
