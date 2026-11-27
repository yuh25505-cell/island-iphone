package com.island.phone.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object HomeWidgetImages {
    private const val MAX_EDGE = 1080
    private const val PICK_DIR = "home_pick"
    private const val KEEP_DIR = "home_widget"

    suspend fun importPicked(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            val degrees = resolver.openInputStream(uri)?.use { readRotation(it) } ?: 0
            var bitmap = decoded
            if (degrees != 0) {
                val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longest
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            }
            val dir = File(context.cacheDir, PICK_DIR).apply { mkdirs() }
            val alpha = bitmap.hasAlpha()
            val file = File(dir, UUID.randomUUID().toString() + if (alpha) ".png" else ".jpg")
            file.outputStream().use { out ->
                bitmap.compress(if (alpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.path
        } catch (e: Exception) {
            null
        }
    }

    suspend fun commit(context: Context, sources: List<String>, previous: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            val pickDir = File(context.cacheDir, PICK_DIR)
            val keepDir = File(context.mediaDir(), KEEP_DIR).apply { mkdirs() }
            val result = sources.map { source ->
                val file = File(source)
                if (ImageSource.isLocal(source) && file.parentFile?.path == pickDir.path) {
                    val dest = File(keepDir, file.name)
                    try {
                        file.copyTo(dest, overwrite = true)
                        file.delete()
                        dest.path
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    source
                }
            }
            previous
                .filter { ImageSource.isLocal(it) && it !in result && File(it).parentFile?.path == keepDir.path }
                .forEach { File(it).delete() }
            pickDir.listFiles()?.forEach { it.delete() }
            result
        }

    private fun readRotation(stream: java.io.InputStream): Int = try {
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}
