package com.island.phone.app.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.island.phone.app.home.widget.decodeWidgetPack
import com.island.phone.app.home.widget.encodeWidgetPack
import com.island.phone.app.home.widget.parseWidgetStyle
import com.island.phone.app.home.widget.widgetKindLabel
import com.island.phone.app.home.widget.widgetStyleTemplate
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.files.HomeWidgetStyleTransfer
import com.island.phone.data.settings.HomeWidgetTextStore
import com.island.phone.ui.dialog.IosActionSheet
import com.island.phone.ui.dialog.IosAlertDialog
import com.island.phone.ui.dialog.IosCodeEditDialog
import com.island.phone.ui.dialog.IosToast
import com.island.phone.ui.dialog.SheetAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val StyleMaxLength = 100_000

@Composable
internal fun HomeWidgetStyleBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = IslandTheme.colors
    val glyphColor = colors.background
    Box(
        modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            Modifier
                .padding(6.dp)
                .size(24.dp)
                .background(colors.label.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val w = 1.8.dp.toPx()
                val left = Path().apply {
                    moveTo(size.width * 0.38f, size.height * 0.15f)
                    lineTo(size.width * 0.04f, size.height * 0.5f)
                    lineTo(size.width * 0.38f, size.height * 0.85f)
                }
                val right = Path().apply {
                    moveTo(size.width * 0.62f, size.height * 0.15f)
                    lineTo(size.width * 0.96f, size.height * 0.5f)
                    lineTo(size.width * 0.62f, size.height * 0.85f)
                }
                val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(left, glyphColor, style = stroke)
                drawPath(right, glyphColor, style = stroke)
            }
        }
    }
}

@Composable
internal fun HomeWidgetStyleController(
    trigger: HomeWidgetItem?,
    store: HomeWidgetTextStore,
    onConsumed: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var kindName by rememberSaveable { mutableStateOf(HomeWidgetKind.DATE.name) }
    var copy by rememberSaveable { mutableStateOf(0) }
    val kind = HomeWidgetKind.values().firstOrNull { it.name == kindName } ?: HomeWidgetKind.DATE
    var step by remember { mutableIntStateOf(0) }
    var editorVisible by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf("") }
    var alertVisible by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf("") }

    LaunchedEffect(trigger) {
        if (trigger != null) {
            kindName = trigger.kind.name
            copy = trigger.copy
            step = 1
            onConsumed()
        }
    }

    val showToast: (String) -> Unit = { text ->
        toastText = text
        toastVisible = true
    }
    val showAlert: (String, String) -> Unit = { title, message ->
        alertTitle = title
        alertMessage = message
        alertVisible = true
    }
    val openEditor: (String) -> Unit = { text ->
        editorText = text
        editorVisible = true
    }
    val takeImported: (String?) -> Unit = { raw ->
        if (raw == null) {
            showAlert("导入失败", "没有读到内容")
        } else {
            val result = decodeWidgetPack(kind, raw)
            val problem = result.error
            if (problem != null) showAlert("导入失败", problem) else openEditor(result.css)
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { takeImported(HomeWidgetStyleTransfer.read(context, uri)) }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/css")) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = store.styleOf(kind.name, copy).first()
                val pack = encodeWidgetPack(kind, saved.ifBlank { widgetStyleTemplate(kind) })
                if (HomeWidgetStyleTransfer.write(context, uri, pack)) {
                    showToast("已保存")
                } else {
                    showAlert("保存失败", "文件没有写进去")
                }
            }
        }
    }

    IosActionSheet(
        visible = step == 1,
        actions = listOf(
            SheetAction(title = "编辑样式代码") {
                step = 0
                scope.launch {
                    val saved = store.styleOf(kind.name, copy).first()
                    openEditor(saved.ifBlank { widgetStyleTemplate(kind) })
                }
            },
            SheetAction(title = "导入样式") { step = 2 },
            SheetAction(title = "导出样式") { step = 3 },
            SheetAction(title = "恢复默认样式", destructive = true) { step = 4 },
        ),
        cancelText = "取消",
        onDismiss = { step = 0 },
    )

    IosActionSheet(
        visible = step == 2,
        actions = listOf(
            SheetAction(title = "从文件导入") {
                step = 0
                importPicker.launch(arrayOf("*/*"))
            },
            SheetAction(title = "从剪贴板导入") {
                step = 0
                takeImported(HomeWidgetStyleTransfer.pasteText(context))
            },
        ),
        cancelText = "取消",
        onDismiss = { step = 0 },
    )

    IosActionSheet(
        visible = step == 3,
        actions = listOf(
            SheetAction(title = "保存为文件") {
                step = 0
                exportPicker.launch("island-widget-${kind.name.lowercase()}.css")
            },
            SheetAction(title = "复制到剪贴板") {
                step = 0
                scope.launch {
                    val saved = store.styleOf(kind.name, copy).first()
                    val pack = encodeWidgetPack(kind, saved.ifBlank { widgetStyleTemplate(kind) })
                    if (HomeWidgetStyleTransfer.copyText(context, pack)) {
                        showToast("已复制")
                    } else {
                        showAlert("复制失败", "没有复制成功")
                    }
                }
            },
        ),
        cancelText = "取消",
        onDismiss = { step = 0 },
    )

    IosActionSheet(
        visible = step == 4,
        actions = listOf(
            SheetAction(title = "恢复默认样式", destructive = true) {
                step = 0
                scope.launch {
                    store.resetStyle(kind.name, copy)
                    showToast("已恢复默认")
                }
            },
        ),
        cancelText = "取消",
        onDismiss = { step = 0 },
    )

    IosCodeEditDialog(
        visible = editorVisible,
        title = widgetKindLabel(kind) + "样式",
        text = editorText,
        maxLength = StyleMaxLength,
        saveText = "保存",
        cancelText = "取消",
        validate = { code -> parseWidgetStyle(code).error },
        onSave = { code ->
            editorVisible = false
            scope.launch {
                if (code.isBlank()) {
                    store.resetStyle(kind.name, copy)
                } else {
                    store.setStyle(kind.name, copy, code.trim())
                }
                showToast("已保存")
            }
        },
        onDismiss = { editorVisible = false },
    )

    IosAlertDialog(
        visible = alertVisible,
        title = alertTitle,
        message = alertMessage,
        okText = "好",
        onDismiss = { alertVisible = false },
    )

    IosToast(
        visible = toastVisible,
        text = toastText,
        onDismiss = { toastVisible = false },
    )
}
