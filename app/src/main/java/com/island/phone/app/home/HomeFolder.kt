package com.island.phone.app.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.app.home.widget.WidgetLightBackground
import com.island.phone.core.theme.IslandTheme

internal const val FOLDER_ICON_PREFIX = "folder_"
internal const val FOLDER_ID_PREFIX = "app_folder_"
internal const val FOLDER_PAGE = -2
internal const val FOLDER_MAX = 9
internal const val FOLDER_DEFAULT_NAME = "文件夹"

internal fun HomeItem.isFolder(): Boolean = this is HomeAppItem && iconName.startsWith(FOLDER_ICON_PREFIX)

internal fun folderNumber(id: String): Int? =
    if (id.startsWith(FOLDER_ID_PREFIX)) id.removePrefix(FOLDER_ID_PREFIX).toIntOrNull() else null

internal fun folderIdOf(number: Int): String = "$FOLDER_ID_PREFIX$number"

@Composable
internal fun HomeFolderIcon(
    apps: List<String>,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = IslandTheme.colors
    val shape = RoundedCornerShape(iconSize * 0.24f)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val borderColor = if (colors.isDark) colors.gray4 else WidgetLightBackground
    val dotColor = colors.label.copy(alpha = 0.22f)

    Box(
        modifier
            .size(iconSize)
            .alpha(if (pressed) 0.55f else 1f)
            .background(colors.groupedCard, shape)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize().padding(iconSize * 0.14f)) {
            val gap = size.width * 0.07f
            val cell = (size.width - gap * 2f) / 3f
            val count = minOf(apps.size, FOLDER_MAX)
            for (i in 0 until count) {
                val x = (i % 3) * (cell + gap)
                val y = (i / 3) * (cell + gap)
                drawRoundRect(
                    color = dotColor,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.26f),
                )
            }
        }
    }
}

@Composable
internal fun HomeFolderOverlay(
    visible: Boolean,
    name: String,
    apps: List<String>,
    onNameClick: () -> Unit,
    onAppLongPress: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IslandTheme.colors

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.85f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(colors.secondaryBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .height(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNameClick,
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = name,
                            style = TextStyle(
                                color = colors.label,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    apps.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { id ->
                                val item = HomeCatalog.items[id] as? HomeAppItem
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                        .pointerInput(id) {
                                            detectTapGestures(onLongPress = { onAppLongPress(id) })
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (item != null) {
                                        HomeAppIcon(name = item.iconName, size = 60.dp)
                                        Spacer(Modifier.height(4.dp))
                                        BasicText(
                                            text = item.label,
                                            style = TextStyle(
                                                color = colors.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Center,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
