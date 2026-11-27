package com.island.phone.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeWidgetDataStore by preferencesDataStore(name = "home_widgets")

class HomeWidgetTextStore(private val context: Context) {
    private val dateKey = stringPreferencesKey("date_lines")
    private val quoteKey = stringPreferencesKey("quote_lines")
    private val weekTextKey = stringPreferencesKey("week_texts")
    private val weekAvatarKey = stringPreferencesKey("week_avatar")
    private val photoKey = stringPreferencesKey("photo_sources")

    val dateLines: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[dateKey], DefaultDateLines)
    }

    val quoteLines: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[quoteKey], DefaultQuoteLines)
    }

    val weekTexts: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[weekTextKey], DefaultWeekTexts)
    }

    val weekAvatar: Flow<String> = context.homeWidgetDataStore.data.map { prefs ->
        prefs[weekAvatarKey] ?: ""
    }

    val photoSources: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[photoKey], DefaultPhotoSources)
    }

    suspend fun setDateLines(lines: List<String>) {
        context.homeWidgetDataStore.edit { prefs -> prefs[dateKey] = encode(lines) }
    }

    suspend fun setQuoteLines(lines: List<String>) {
        context.homeWidgetDataStore.edit { prefs -> prefs[quoteKey] = encode(lines) }
    }

    suspend fun setWeek(avatar: String, texts: List<String>) {
        context.homeWidgetDataStore.edit { prefs ->
            prefs[weekTextKey] = encode(texts)
            prefs[weekAvatarKey] = avatar
        }
    }

    suspend fun setPhotos(sources: List<String>) {
        context.homeWidgetDataStore.edit { prefs -> prefs[photoKey] = encode(sources) }
    }

    suspend fun resetDateLines() {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(dateKey) }
    }

    suspend fun resetQuoteLines() {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(quoteKey) }
    }

    suspend fun resetWeek() {
        context.homeWidgetDataStore.edit { prefs ->
            prefs.remove(weekTextKey)
            prefs.remove(weekAvatarKey)
        }
    }

    suspend fun resetPhotos() {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(photoKey) }
    }

    private fun encode(lines: List<String>): String =
        lines.joinToString("\n") { it.replace("\n", " ") }

    private fun decode(raw: String?, default: List<String>): List<String> {
        if (raw == null) return default
        val parts = raw.split("\n")
        return List(default.size) { index -> parts.getOrElse(index) { "" } }
    }

    companion object {
        val DefaultDateLines: List<String> = listOf(
            "晚风吹过旧时光",
            "★ 日子慢慢变好",
            "♥ 山海皆可平",
            "星河落满你肩头",
        )
        val DefaultQuoteLines: List<String> = listOf(
            "人间烟火",
            "星河滚烫",
            "山海自有归期",
            "万事皆可期",
        )
        val DefaultWeekTexts: List<String> = listOf("晚风与星光", "夜曲", "", "")
        val DefaultPhotoSources: List<String> = listOf("", "", "", "")
    }
}
