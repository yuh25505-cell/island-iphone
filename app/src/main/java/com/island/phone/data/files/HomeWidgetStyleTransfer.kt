package com.island.phone.data.files

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HomeWidgetStyleTransfer {
    private const val MaxChars = 100_000

    suspend fun read(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val reader = input.bufferedReader(Charsets.UTF_8)
                val buffer = CharArray(MaxChars + 1)
                var total = 0
                while (total < buffer.size) {
                    val n = reader.read(buffer, total, buffer.size - total)
                    if (n < 0) break
                    total += n
                }
                String(buffer, 0, total).removePrefix("\uFEFF")
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun write(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val out = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun copyText(context: Context, text: String): Boolean {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        return try {
            manager.setPrimaryClip(ClipData.newPlainText("island-widget", text))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun pasteText(context: Context): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        return try {
            val clip = manager.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(context)?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
