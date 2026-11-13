package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PaperThemeMode(val key: String, val labelRu: String) {
    SYSTEM("system", "Системная"),
    LIGHT("light", "Светлая бумага"),
    DARK("dark", "Тёмная бумага");

    companion object {
        fun fromKey(key: String?): PaperThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

private val Context.themeDataStore by preferencesDataStore(name = "chronicles_preferences")

class ThemePreferencesRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("paper_theme_mode")

    val themeMode: Flow<PaperThemeMode> = context.themeDataStore.data.map { prefs ->
        PaperThemeMode.fromKey(prefs[themeModeKey])
    }

    suspend fun setThemeMode(mode: PaperThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.key
        }
    }
}
