package com.island.phone.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.island.phone.net.HttpClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ImageSource {
    fun isRemote(source: String): Boolean =
        source.startsWith("http://", ignoreCase = true) || source.startsWith("https://", ignoreCase = true)

    fun isLocal(source: String): Boolean = source.startsWith("/")
}

object ImageSourceLoader {
    private const val MAX_EDGE = 1080
    private const val MAX_DOWNLOAD = 15L * 1024 * 1024

    private val memory = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).toInt().coerceAtLeast(4 * 1024 * 1024),
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val client by lazy {
        HttpClients.shared.newBuilder()
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    fun peek(source: String): Bitmap? = if (source.isBlank()) null else memory.get(source)

    suspend fun load(context: Context, source: String): Bitmap? = withContext(Dispatchers.IO) {
        memory.get(source)?.let { return@withContext it }
        val remote = ImageSource.isRemote(source)
        val file = when {
            remote -> download(context, source)
            ImageSource.isLocal(source) -> File(source).takeIf { it.isFile }
            else -> null
        } ?: return@withContext null
        val bitmap = decode(file)
        if (bitmap == null) {
            if (remote) file.delete()
            return@withContext null
        }
        memory.put(source, bitmap)
        bitmap
    }

    private fun download(context: Context, url: String): File? {
        val dir = File(context.cacheDir, "home_images").apply { mkdirs() }
        val target = File(dir, sha1(url))
        if (target.isFile && target.length() > 0) return target
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body ?: return null
                if (body.contentLength() > MAX_DOWNLOAD) return null
                val tmp = File(dir, target.name + ".tmp")
                body.byteStream().use { input ->
                    tmp.outputStream().use { out ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > MAX_DOWNLOAD) {
                                tmp.delete()
                                return null
                            }
                            out.write(buffer, 0, n)
                        }
                    }
                }
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    return null
                }
                target
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.path, options)
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
