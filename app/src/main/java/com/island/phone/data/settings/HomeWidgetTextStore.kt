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

    val dateLines: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[dateKey], DefaultDateLines)
    }

    val quoteLines: Flow<List<String>> = context.homeWidgetDataStore.data.map { prefs ->
        decode(prefs[quoteKey], DefaultQuoteLines)
    }

    suspend fun setDateLines(lines: List<String>) {
        context.homeWidgetDataStore.edit { prefs -> prefs[dateKey] = encode(lines) }
    }

    suspend fun setQuoteLines(lines: List<String>) {
        context.homeWidgetDataStore.edit { prefs -> prefs[quoteKey] = encode(lines) }
    }

    suspend fun resetDateLines() {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(dateKey) }
    }

    suspend fun resetQuoteLines() {
        context.homeWidgetDataStore.edit { prefs -> prefs.remove(quoteKey) }
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
            "Own the moment",
            "★ Find your fire",
            "♥ Rise and shine",
            "Shine like the stars",
        )
        val DefaultQuoteLines: List<String> = listOf(
            "Make magic happen",
            "Own the moment",
            "New adventures",
            "Find your fire",
        )
    }
}
