package com.island.phone.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Immutable
data class SheetAction(
    val title: String,
    val destructive: Boolean = false,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun IosActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    cancelText: String,
    onDismiss: () -> Unit,
) {
    val colors = IslandTheme.colors
    BackHandler(enabled = visible, onBack = onDismiss)

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

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(250),
                targetOffsetY = { it },
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.tertiaryBackground),
                ) {
                    actions.forEachIndexed { index, action ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.separator),
                            )
                        }
                        SheetRow(
                            title = action.title,
                            color = if (action.destructive) colors.red else colors.accent,
                            bold = false,
                            selected = action.selected,
                            onClick = action.onClick,
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.tertiaryBackground),
                ) {
                    SheetRow(
                        title = cancelText,
                        color = colors.accent,
                        bold = true,
                        selected = false,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetRow(
    title: String,
    color: Color,
    bold: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = IslandTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .height(57.dp)
            .background(if (pressed) colors.fill else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = color,
                fontSize = 20.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
        if (selected) {
            Canvas(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(16.dp),
            ) {
                val path = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.55f)
                    lineTo(size.width * 0.4f, size.height * 0.85f)
                    lineTo(size.width * 0.92f, size.height * 0.15f)
                }
                drawPath(
                    path = path,
                    color = colors.accent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}
