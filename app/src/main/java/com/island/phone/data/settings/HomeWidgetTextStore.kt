package com.island.phone.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeWidgetDataStore by preferencesDataStore(name = "home_widgets")

class HomeWidgetTextStore(private val context: Context) {
    private fun key(name: String, copy: Int): Preferences.Key<String> =
        stringPreferencesKey(if (copy == 0) name else "${name}_c$copy")

    val dateLines: Flow<List<String>> = dateLinesOf(0)
    val quoteLines: Flow<List<String>> = quoteLinesOf(0)
    val weekTexts: Flow<List<String>> = weekTextsOf(0)
    val weekAvatar: Flow<String> = weekAvatarOf(0)
    val photoSources: Flow<List<String>> = photoSourcesOf(0)
    val photoCaptions: Flow<List<String>> = photoCaptionsOf(0)

    fun dateLinesOf(copy: Int): Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[key(DATE, copy)], DefaultDateLines)
    }

    fun quoteLinesOf(copy: Int): Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[key(QUOTE, copy)], DefaultQuoteLines)
    }

    fun weekTextsOf(copy: Int): Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        val saved = prefs[key(WEEK_GLYPH, copy)]
        if (saved != null) {
            decode(saved, DefaultWeekTexts)
        } else if (copy != 0) {
            DefaultWeekTexts
        } else {
            val old = prefs[key(WEEK_OLD, 0)]
            if (old == null) DefaultWeekTexts else upgradeOldWeekTexts(decode(old, EmptyWeekTexts))
        }
    }

    fun weekAvatarOf(copy: Int): Flow<String> = context.homeWidgetDataStore.data.map { prefs ->
        prefs[key(WEEK_AVATAR, copy)] ?: ""
    }

    fun photoSourcesOf(copy: Int): Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[key(PHOTO, copy)], DefaultPhotoSources)
    }

    fun photoCaptionsOf(copy: Int): Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[key(PHOTO_CAPTION, copy)], DefaultPhotoCaptions)
    }

    suspend fun setDateLines(lines: List<String>, copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs -> prefs[key(DATE, copy)] = encode(lines) }
    }

    suspend fun setQuoteLines(lines: List<String>, copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs -> prefs[key(QUOTE, copy)] = encode(lines) }
    }

    suspend fun setWeek(avatar: String, texts: List<String>, copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs ->
            prefs[key(WEEK_GLYPH, copy)] = encode(texts)
            prefs.remove(key(WEEK_OLD, copy))
            prefs[key(WEEK_AVATAR, copy)] = avatar
        }
    }

    suspend fun setPhotos(sources: List<String>, captions: List<String>? = null, copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs ->
            prefs[key(PHOTO, copy)] = encode(sources)
            if (captions != null) prefs[key(PHOTO_CAPTION, copy)] = encode(captions)
        }
    }

    suspend fun resetDateLines(copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(key(DATE, copy)) }
    }

    suspend fun resetQuoteLines(copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(key(QUOTE, copy)) }
    }

    suspend fun resetWeek(copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs ->
            prefs.remove(key(WEEK_OLD, copy))
            prefs.remove(key(WEEK_GLYPH, copy))
            prefs.remove(key(WEEK_AVATAR, copy))
        }
    }

    suspend fun resetPhotos(copy: Int = 0) {
        context.homeWidgetDataStore.edit { prefs ->
            prefs.remove(key(PHOTO, copy))
            prefs.remove(key(PHOTO_CAPTION, copy))
        }
    }

    private fun encode(lines: List<String>): String =
        lines.joinToString("\n") { it.replace("\n", " ") }

    private fun decode(raw: String?, default: List<String>): List<String> {
        if (raw == null) return default
        val parts = raw.split("\n")
        return List(default.size) { index -> parts.getOrElse(index) { "" } }
    }

    private fun upgradeOldWeekTexts(old: List<String>): List<String> {
        val left = old.getOrElse(0) { "" }
        val right = old.getOrElse(1) { "" }
        return listOf(
            GlyphHeadphone + (if (left.isEmpty()) "" else " $left"),
            GlyphPlay + " " + GlyphWave + (if (right.isEmpty()) "" else " $right"),
            old.getOrElse(2) { "" },
            old.getOrElse(3) { "" },
        )
    }

    companion object {
        private const val DATE = "date_lines"
        private const val QUOTE = "quote_lines"
        private const val WEEK_OLD = "week_texts"
        private const val WEEK_GLYPH = "week_texts_glyph"
        private const val WEEK_AVATAR = "week_avatar"
        private const val PHOTO = "photo_sources"
        private const val PHOTO_CAPTION = "photo_captions"

        const val GlyphHeadphone = "{耳机}"
        const val GlyphPlay = "{播放}"
        const val GlyphWave = "{波浪}"

        private val EmptyWeekTexts: List<String> = listOf("", "", "", "")

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
        val DefaultWeekTexts: List<String> = listOf(
            "$GlyphHeadphone 晚风与星光",
            "$GlyphPlay $GlyphWave 夜曲",
            "",
            "",
        )
        val DefaultPhotoSources: List<String> = listOf("", "", "", "")
        val DefaultPhotoCaptions: List<String> = listOf("something", "Sunny", "HELLO", "Mooncake")
    }
}
