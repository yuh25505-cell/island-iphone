package com.island.phone.ui.dialog

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.files.ImageSource
import com.island.phone.ui.image.SourceImage
import kotlinx.coroutines.launch

@Composable
fun IosMediaEditDialog(
    visible: Boolean,
    title: String,
    images: List<String>,
    texts: List<String>,
    textLabels: List<String>,
    maxLength: Int,
    linkHint: String,
    localHint: String,
    pickText: String,
    cancelText: String,
    saveText: String,
    resetText: String,
    onImport: suspend (Uri) -> String?,
    onSave: (List<String>, List<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = IslandTheme.colors
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val imageDrafts = remember(visible) { mutableStateListOf(*images.toTypedArray()) }
    val textDrafts = remember(visible) { mutableStateListOf(*texts.toTypedArray()) }
    var pickSlot by remember { mutableIntStateOf(-1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val slot = pickSlot
        if (uri != null && slot in imageDrafts.indices) {
            scope.launch {
                val path = onImport(uri)
                if (path != null && slot in imageDrafts.indices) imageDrafts[slot] = path
            }
        }
    }
    val dismiss: () -> Unit = {
        focus.clearFocus()
        onDismiss()
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
            val maxH = (maxHeight - 32.dp).coerceAtLeast(240.dp)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.1f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f),
            ) {
                Column(
                    Modifier
                        .width(320.dp)
                        .heightIn(max = maxH)
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

                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        if (imageDrafts.isNotEmpty()) {
                            MediaGroup {
                                imageDrafts.forEachIndexed { index, value ->
                                    if (index > 0) MediaDivider(12.dp)
                                    ImageRow(
                                        source = value,
                                        linkHint = linkHint,
                                        localHint = localHint,
                                        pickText = pickText,
                                        onChange = { imageDrafts[index] = it.replace("\n", "").trim() },
                                        onClear = { imageDrafts[index] = "" },
                                        onPick = {
                                            focus.clearFocus()
                                            pickSlot = index
                                            picker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        if (textDrafts.isNotEmpty()) {
                            if (imageDrafts.isNotEmpty()) Spacer(Modifier.height(12.dp))
                            MediaGroup {
                                textDrafts.forEachIndexed { index, value ->
                                    if (index > 0) MediaDivider(12.dp)
                                    TextRow(
                                        label = textLabels.getOrElse(index) { "" },
                                        value = value,
                                        onChange = { textDrafts[index] = it.replace("\n", "").take(maxLength) },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    MediaDivider(0.dp)
                    MediaButton(
                        text = saveText,
                        color = colors.accent,
                        bold = true,
                        onClick = {
                            focus.clearFocus()
                            onSave(imageDrafts.map { normalizeSource(it) }, textDrafts.toList())
                        },
                    )
                    MediaDivider(0.dp)
                    MediaButton(text = resetText, color = colors.red, bold = false, onClick = onReset)
                    MediaDivider(0.dp)
                    MediaButton(text = cancelText, color = colors.accent, bold = false, onClick = dismiss)
                }
            }
        }
    }
}

private fun normalizeSource(raw: String): String {
    val text = raw.trim()
    return if (text.isEmpty() || ImageSource.isLocal(text) || ImageSource.isRemote(text)) text else "https://$text"
}

@Composable
private fun MediaGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = IslandTheme.colors
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.tertiaryBackground),
        content = content,
    )
}

@Composable
private fun ImageRow(
    source: String,
    linkHint: String,
    localHint: String,
    pickText: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    onPick: () -> Unit,
) {
    val colors = IslandTheme.colors
    val local = ImageSource.isLocal(source)
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.fill),
        ) {
            if (source.isNotBlank()) SourceImage(source, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = if (local) "" else source,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.label, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (source.isEmpty() || local) {
                            BasicText(
                                text = if (local) localHint else linkHint,
                                style = TextStyle(
                                    color = if (local) colors.secondaryLabel else colors.tertiaryLabel,
                                    fontSize = 15.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (source.isNotBlank()) {
            val clearSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(44.dp)
                    .clickable(interactionSource = clearSource, indication = null, onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                ClearGlyph(20.dp)
            }
        }
        val pickSource = remember { MutableInteractionSource() }
        val pressed by pickSource.collectIsPressedAsState()
        Box(
            Modifier
                .height(44.dp)
                .background(if (pressed) colors.fill else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable(interactionSource = pickSource, indication = null, onClick = onPick)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = pickText,
                style = TextStyle(color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TextRow(label: String, value: String, onChange: (String) -> Unit) {
    val colors = IslandTheme.colors
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label.isNotEmpty()) {
            BasicText(
                text = label,
                modifier = Modifier.width(48.dp),
                style = TextStyle(color = colors.secondaryLabel, fontSize = 17.sp),
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.label, fontSize = 17.sp),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClearGlyph(size: Dp) {
    val colors = IslandTheme.colors
    val circle = colors.tertiaryLabel
    val cross = colors.tertiaryBackground
    Canvas(Modifier.size(size)) {
        drawCircle(color = circle)
        val c = this.size.width / 2f
        val d = this.size.width * 0.17f
        val stroke = this.size.width * 0.1f
        drawLine(cross, Offset(c - d, c - d), Offset(c + d, c + d), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(cross, Offset(c + d, c - d), Offset(c - d, c + d), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun MediaDivider(start: Dp) {
    val colors = IslandTheme.colors
    Box(Modifier.padding(start = start).fillMaxWidth().height(0.5.dp).background(colors.separator))
}

@Composable
private fun MediaButton(
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
