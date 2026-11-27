package com.island.phone.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.homeLayoutDataStore by preferencesDataStore(name = "home_layout")

class HomeLayoutStore(private val context: Context) {
    private val placementsKey = stringPreferencesKey("placements")

    val placements: Flow<String> = context.homeLayoutDataStore.data.map { prefs ->
        prefs[placementsKey] ?: ""
    }

    suspend fun save(raw: String) {
        context.homeLayoutDataStore.edit { prefs -> prefs[placementsKey] = raw }
    }

    suspend fun reset() {
        context.homeLayoutDataStore.edit { prefs -> prefs.remove(placementsKey) }
    }
}
