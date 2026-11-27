package com.island.phone.ui.image

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.island.phone.data.files.ImageSourceLoader

@Composable
fun SourceImage(source: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(
        initialValue = ImageSourceLoader.peek(source)?.asImageBitmap(),
        key1 = source,
    ) {
        if (source.isBlank()) {
            value = null
            return@produceState
        }
        val cached = ImageSourceLoader.peek(source)
        if (cached != null) {
            value = cached.asImageBitmap()
            return@produceState
        }
        value = null
        value = ImageSourceLoader.load(context, source)?.asImageBitmap()
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
