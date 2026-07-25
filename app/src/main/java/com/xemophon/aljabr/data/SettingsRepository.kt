package com.xemophon.aljabr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    AUTO, LIGHT, DARK
}

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val useRadiansKey = booleanPreferencesKey("use_radians")

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.AUTO.name
        AppTheme.valueOf(themeName)
    }

    val useRadiansFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[useRadiansKey] ?: false
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun setUseRadians(useRadians: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useRadiansKey] = useRadians
        }
    }
}
