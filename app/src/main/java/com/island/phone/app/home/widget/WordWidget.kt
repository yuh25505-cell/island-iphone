package com.island.phone.app.home.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import java.time.LocalDate

@Immutable
private class WordEntry(
    val word: String,
    val meaning: String,
    val example: String,
    val exampleZh: String,
)

private val words = listOf(
    WordEntry("discover", "v. 发现；找到", "Discover a new place.", "发现一个新地方。"),
    WordEntry("gentle", "adj. 温柔的", "She has a gentle voice.", "她的声音很温柔。"),
    WordEntry("horizon", "n. 地平线", "The sun sets at the horizon.", "太阳落在地平线上。"),
    WordEntry("wander", "v. 漫步；游荡", "We wander along the shore.", "我们沿着海岸漫步。"),
    WordEntry("harbor", "n. 港湾", "The boat rests in the harbor.", "小船停靠在港湾里。"),
    WordEntry("breeze", "n. 微风", "A breeze moves the leaves.", "微风吹动树叶。"),
    WordEntry("twilight", "n. 黄昂", "Twilight paints the sky.", "黄昂染红了天空。"),
    WordEntry("whisper", "v. 低语", "He likes to whisper.", "他喜欢轻声低语。"),
    WordEntry("serene", "adj. 宁静的", "The lake is serene.", "湖面很宁静。"),
    WordEntry("cherish", "v. 珍惜", "Cherish every moment.", "珍惜每一刻。"),
)

@Composable
fun WordWidget(modifier: Modifier = Modifier) {
    val colors = IslandTheme.colors
    var index by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfYear % words.size) }
    val entry = words[index % words.size]
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(widgetBackground(colors.gray6), RoundedCornerShape(30.dp)),
    ) {
        BasicText(
            text = "Today's Word",
            modifier = Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 18.dp),
            style = TextStyle(
                color = colors.secondaryLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(0.46f).padding(end = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = entry.word,
                    style = TextStyle(
                        color = colors.label,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = entry.meaning,
                    style = TextStyle(
                        color = colors.secondaryLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.label.copy(alpha = 0.12f)),
            )
            Column(
                Modifier.weight(0.54f).padding(start = 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = "例句",
                    style = TextStyle(
                        color = colors.secondaryLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = entry.example,
                    style = TextStyle(
                        color = colors.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = entry.exampleZh,
                    style = TextStyle(
                        color = colors.secondaryLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 16.dp)
                .alpha(if (pressed) 0.55f else 1f)
                .background(colors.gray5, RoundedCornerShape(50))
                .clickable(interactionSource = source, indication = null) {
                    index = (index + 1) % words.size
                }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "下一词",
                style = TextStyle(
                    color = colors.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}
