package com.island.phone.app.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberWobbleAngle(active: Boolean): State<Float> {
    return if (active) {
        rememberInfiniteTransition(label = "homeWobble").animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(140, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "homeWobbleAngle",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
}

internal fun Modifier.homeWobble(item: HomeItem, editing: Boolean, angle: State<Float>): Modifier {
    if (!editing) return this
    val factor = if (item is HomeWidgetItem) 0.35f else 1f
    val direction = if (item.id.hashCode() % 2 == 0) 1f else -1f
    return this.graphicsLayer { rotationZ = angle.value * factor * direction }
}

@Composable
internal fun HomePage(
    page: Int,
    layout: HomeLayoutState,
    metrics: HomeGridMetrics,
    hiddenId: String?,
    editing: Boolean,
    angle: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable (HomeItem, Boolean, Dp) -> Unit,
) {
    Box(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        layout.onPage(page).forEach { p ->
            val item = HomeCatalog.items[p.id]
            if (item != null) {
                val r = homeItemRect(item, p.column, p.row, metrics)
                key(p.id) {
                    Box(
                        Modifier
                            .offset(x = r.x.dp, y = r.y.dp)
                            .size(width = r.w.dp, height = r.h.dp)
                            .homeWobble(item, editing, angle),
                    ) {
                        if (p.id != hiddenId) content(item, false, metrics.iconSize)
                    }
                }
            }
        }
    }
}
