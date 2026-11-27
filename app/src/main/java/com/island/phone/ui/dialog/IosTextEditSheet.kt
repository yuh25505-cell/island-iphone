package com.island.phone.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Composable
fun IosTextEditSheet(
    visible: Boolean,
    title: String,
    values: List<String>,
    maxLength: Int,
    cancelText: String,
    saveText: String,
    resetText: String,
    onSave: (List<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IslandTheme.colors
    val focus = LocalFocusManager.current
    val fields = remember(title) { mutableStateListOf(*values.toTypedArray()) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val dismiss: () -> Unit = {
        focus.clearFocus()
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            dragY = 0f
            for (i in fields.indices) fields[i] = values.getOrElse(i) { "" }
        }
    }
    BackHandler(enabled = visible, onBack = dismiss)

    val dragState = rememberDraggableState { delta -> dragY = (dragY + delta).coerceAtLeast(0f) }

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
                        onClick = dismiss,
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
                    .graphicsLayer { translationY = dragY }
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(colors.secondaryBackground)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(
                    Modifier.draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (dragY > 140f || velocity > 1500f) {
                                dismiss()
                            } else {
                                animate(
                                    initialValue = dragY,
                                    targetValue = 0f,
                                    initialVelocity = velocity,
                                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
                                ) { value, _ -> dragY = value }
                            }
                        },
                    ),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                            .size(width = 36.dp, height = 5.dp)
                            .background(colors.gray3, RoundedCornerShape(50)),
                    )
                    Box(Modifier.fillMaxWidth().height(52.dp)) {
                        SheetTextButton(
                            text = cancelText,
                            bold = false,
                            modifier = Modifier.align(Alignment.CenterStart),
                            onClick = dismiss,
                        )
                        BasicText(
                            text = title,
                            modifier = Modifier.align(Alignment.Center),
                            style = TextStyle(
                                color = colors.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        SheetTextButton(
                            text = saveText,
                            bold = true,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onClick = {
                                focus.clearFocus()
                                onSave(fields.toList())
                            },
                        )
                    }
                }

                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.tertiaryBackground),
                ) {
                    fields.forEachIndexed { index, value ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.separator),
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = { fields[index] = it.replace("\n", "").take(maxLength) },
                                singleLine = true,
                                textStyle = TextStyle(color = colors.label, fontSize = 17.sp),
                                cursorBrush = SolidColor(colors.accent),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                ResetRow(text = resetText, onClick = onReset)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SheetTextButton(
    text: String,
    bold: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = IslandTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        modifier
            .fillMaxHeight()
            .alpha(if (pressed) 0.4f else 1f)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = colors.accent,
                fontSize = 17.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun ResetRow(text: String, onClick: () -> Unit) {
    val colors = IslandTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) colors.fill else colors.tertiaryBackground)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = colors.red, fontSize = 17.sp),
        )
    }
}
