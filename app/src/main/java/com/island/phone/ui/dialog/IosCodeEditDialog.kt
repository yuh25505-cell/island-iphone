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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Composable
fun IosCodeEditDialog(
    visible: Boolean,
    title: String,
    text: String,
    maxLength: Int,
    saveText: String,
    cancelText: String,
    validate: (String) -> String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IslandTheme.colors
    val focus = LocalFocusManager.current
    var draft by remember { mutableStateOf(text) }
    var error by remember { mutableStateOf<String?>(null) }
    val dismiss: () -> Unit = {
        focus.clearFocus()
        onDismiss()
    }

    LaunchedEffect(visible) {
        if (visible) {
            draft = text
            error = null
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

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val editorMax = (maxHeight - 220.dp).coerceAtLeast(120.dp)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.1f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
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

                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.tertiaryBackground)
                            .heightIn(min = 140.dp, max = editorMax)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = {
                                draft = it.take(maxLength)
                                error = null
                            },
                            textStyle = TextStyle(
                                color = colors.label,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 116.dp)
                                .padding(12.dp),
                        )
                    }

                    val problem = error
                    if (problem != null) {
                        Box(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = problem,
                                style = TextStyle(
                                    color = colors.red,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    CodeDivider()
                    CodeButton(
                        text = saveText,
                        color = colors.accent,
                        bold = true,
                        onClick = {
                            focus.clearFocus()
                            val found = validate(draft)
                            if (found != null) error = found else onSave(draft)
                        },
                    )
                    CodeDivider()
                    CodeButton(text = cancelText, color = colors.accent, bold = false, onClick = dismiss)
                }
            }
        }
    }
}

@Composable
private fun CodeDivider() {
    val colors = IslandTheme.colors
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
}

@Composable
private fun CodeButton(
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
