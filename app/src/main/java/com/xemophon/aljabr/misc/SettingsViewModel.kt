package com.xemophon.aljabr.misc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.AppTheme
import com.xemophon.aljabr.data.ColorSchemeType
import com.xemophon.aljabr.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val theme: StateFlow<AppTheme> = repository.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.AUTO
    )

    val dynamicColor: StateFlow<Boolean> = repository.dynamicColorFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val colorScheme: StateFlow<ColorSchemeType> = repository.colorSchemeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ColorSchemeType.DEFAULT
    )

    val useRadians: StateFlow<Boolean> = repository.useRadiansFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val precision: StateFlow<Int> = repository.precisionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 4
    )

    val showSteps: StateFlow<Boolean> = repository.showStepsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val autoClearCache: StateFlow<Boolean> = repository.autoClearCacheFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun setColorScheme(scheme: ColorSchemeType) {
        viewModelScope.launch {
            repository.setColorScheme(scheme)
        }
    }

    fun setUseRadians(useRadians: Boolean) {
        viewModelScope.launch {
            repository.setUseRadians(useRadians)
        }
    }

    fun setPrecision(precision: Int) {
        viewModelScope.launch {
            repository.setPrecision(precision)
        }
    }

    fun setShowSteps(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowSteps(enabled)
        }
    }

    fun setAutoClearCache(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoClearCache(enabled)
        }
    }
}
