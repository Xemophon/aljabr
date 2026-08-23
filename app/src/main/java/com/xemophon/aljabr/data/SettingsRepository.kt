package com.xemophon.aljabr.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    AUTO, LIGHT, DARK
}

enum class ColorSchemeType {
    DEFAULT, BLUE, GREEN, RED, YELLOW, ORANGE, TEAL, PINK, BROWN
}

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val dynamicColorKey = booleanPreferencesKey("use_dynamic_color")
    private val colorSchemeKey = stringPreferencesKey("color_scheme")
    private val useRadiansKey = booleanPreferencesKey("use_radians")
    private val useRationalizeKey = booleanPreferencesKey("use_rationalize")
    private val precisionKey = intPreferencesKey("decimal_precision")
    private val showStepsKey = booleanPreferencesKey("show_steps")
    private val autoClearCacheKey = booleanPreferencesKey("auto_clear_cache")

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[themeKey] ?: AppTheme.AUTO.name
        AppTheme.valueOf(themeName)
    }

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[dynamicColorKey] ?: true
    }

    val colorSchemeFlow: Flow<ColorSchemeType> = context.dataStore.data.map { preferences ->
        val schemeName = preferences[colorSchemeKey] ?: ColorSchemeType.DEFAULT.name
        ColorSchemeType.valueOf(schemeName)
    }

    val useRadiansFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[useRadiansKey] ?: false
    }

    val useRationalizeFlow : Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[useRationalizeKey] ?: false
    }

    val precisionFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[precisionKey] ?: 4
    }

    val showStepsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[showStepsKey] ?: false
    }

    val autoClearCacheFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[autoClearCacheKey] ?: false
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }

    suspend fun setColorScheme(scheme: ColorSchemeType) {
        context.dataStore.edit { preferences ->
            preferences[colorSchemeKey] = scheme.name
        }
    }

    suspend fun setUseRadians(useRadians: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useRadiansKey] = useRadians
        }
    }

    suspend fun setPrecision(precision: Int) {
        context.dataStore.edit { preferences ->
            preferences[precisionKey] = precision
        }
    }

    suspend fun setShowSteps(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showStepsKey] = enabled
        }
    }

    suspend fun setAutoClearCache(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoClearCacheKey] = enabled
        }
    }

    suspend fun setUseRationalize(useRationalize: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useRationalizeKey] = useRationalize
        }
    }
}
