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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme

@Composable
fun IosAlertDialog(
    visible: Boolean,
    title: String,
    message: String,
    okText: String,
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

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.1f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
            ) {
                Column(
                    Modifier
                        .width(270.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.secondaryBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 20.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                        if (message.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            BasicText(
                                text = message,
                                style = TextStyle(
                                    color = colors.label,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                    val source = remember { MutableInteractionSource() }
                    val pressed by source.collectIsPressedAsState()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(if (pressed) colors.fill else Color.Transparent)
                            .clickable(interactionSource = source, indication = null, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = okText,
                            style = TextStyle(
                                color = colors.accent,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
