package com.island.phone.app.lock

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.island.phone.core.theme.IslandTheme
import com.island.phone.data.notice.LockNoticeStore

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val colors = IslandTheme.colors
    val finish by rememberUpdatedState(onUnlocked)
    val notices by LockNoticeStore.notices.collectAsStateWithLifecycle()
    var heightPx by remember { mutableFloatStateOf(1f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var unlocking by remember { mutableStateOf(false) }

    val dragState = rememberDraggableState { delta ->
        if (!unlocking) offsetY = (offsetY + delta).coerceAtMost(0f)
    }
    val backdrop = Brush.verticalGradient(
        listOf(lerp(colors.background, colors.accent, 0.22f), colors.background),
    )

    Box(
        Modifier
            .fillMaxSize()
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (!unlocking) {
                        val open = offsetY < -heightPx * 0.25f || velocity < -1200f
                        if (open) {
                            unlocking = true
                            animate(
                                initialValue = offsetY,
                                targetValue = -heightPx,
                                initialVelocity = velocity,
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
                            ) { value, _ -> offsetY = value.coerceAtMost(0f) }
                            finish()
                        } else {
                            animate(
                                initialValue = offsetY,
                                targetValue = 0f,
                                initialVelocity = velocity,
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
                            ) { value, _ -> offsetY = value.coerceAtMost(0f) }
                        }
                    }
                },
            )
            .onSizeChanged { heightPx = it.height.toFloat() }
            .graphicsLayer { translationY = offsetY }
            .background(backdrop),
    ) {
        LockClock(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
        )
        LockNoticeList(
            notices = notices,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 36.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
                .size(width = 134.dp, height = 5.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.label.copy(alpha = 0.45f)),
        )
    }
}
