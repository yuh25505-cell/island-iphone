package com.island.phone.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Composable
fun IosTextEditDialog(
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
    val dismiss: () -> Unit = {
        focus.clearFocus()
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            for (i in fields.indices) fields[i] = values.getOrElse(i) { "" }
        }
    }
    BackHandler(enabled = visible, onBack = dismiss)

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

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.1f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
            ) {
                Column(
                    Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.secondaryBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = title,
                            style = TextStyle(
                                color = colors.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }

                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.tertiaryBackground),
                    ) {
                        fields.forEachIndexed { index, value ->
                            if (index > 0) {
                                Box(
                                    Modifier
                                        .padding(start = 12.dp)
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(colors.separator),
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .padding(horizontal = 12.dp),
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
                    DialogDivider()
                    DialogButton(
                        text = saveText,
                        color = colors.accent,
                        bold = true,
                        onClick = {
                            focus.clearFocus()
                            onSave(fields.toList())
                        },
                    )
                    DialogDivider()
                    DialogButton(text = resetText, color = colors.red, bold = false, onClick = onReset)
                    DialogDivider()
                    DialogButton(text = cancelText, color = colors.accent, bold = false, onClick = dismiss)
                }
            }
        }
    }
}

@Composable
private fun DialogDivider() {
    val colors = IslandTheme.colors
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
}

@Composable
private fun DialogButton(
    text: String,
    color: Color,
    bold: Boolean,
    onClick: () -> Unit,
) {
    val colors = IslandTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (pressed) colors.fill else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 17.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}
