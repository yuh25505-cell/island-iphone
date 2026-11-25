package com.island.phone.core.ui.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.island.phone.core.ui.appearance.AppearanceStore
import com.island.phone.core.ui.appearance.drawImageCover
import com.island.phone.core.ui.apps.AppId
import com.island.phone.core.ui.glass.GlassSurface
import com.island.phone.core.ui.icons.art.IconArt
import com.island.phone.core.ui.icons.art.drawArt
import com.island.phone.core.ui.text.BasicText
import com.island.phone.core.ui.theme.IslandTheme

@Composable
fun AppIcon(app: AppId, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val tokens = IslandTheme.icon
    val custom = AppearanceStore.get(LocalContext.current).iconOf(app)
    val art = IconArt.of(app)
    if (custom == null && art != null) {
        Box(
            modifier = modifier
                .size(size)
                .drawBehind { drawArt(art) }
        )
    } else {
        GlassSurface(
            modifier = modifier.size(size),
            shape = RoundedCornerShape(percent = 22),
            elevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (custom != null) {
                            drawImageCover(custom)
                        } else {
                            drawRect(tokens.tileTint)
                            translate(0f, this.size.minDimension * 0.015f) {
                                drawAppGlyph(app, tokens.glyphShadow)
                            }
                            drawAppGlyph(app, tokens.glyph)
                        }
                    }
            )
        }
    }
}

@Composable
fun AppIconWithLabel(app: AppId, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val tokens = IslandTheme.icon
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AppIcon(app = app, size = size)
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = tokens.label,
                fontSize = 11.sp,
                shadow = Shadow(tokens.labelShadow, Offset(0f, 1f), 3f)
            )
        )
    }
}
