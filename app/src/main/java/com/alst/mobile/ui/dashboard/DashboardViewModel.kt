package com.alst.mobile.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alst.mobile.core.translator.LanguageModelManager
import com.alst.mobile.data.preferences.AppPreferencesRepository
import com.alst.mobile.data.preferences.appDataStore
import com.alst.mobile.domain.model.EngineType
import com.alst.mobile.domain.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = AppPreferencesRepository(application)
    val languageModelManager = LanguageModelManager()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences
        viewModelScope.launch {
            prefsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        isServiceEnabled = settings.isServiceEnabled,
                        engineType = settings.engineType,
                        targetLanguage = settings.targetLanguage,
                        geminiApiKey = settings.geminiApiKey,
                    )
                }
            }
        }

        // Observe language model status
        viewModelScope.launch {
            languageModelManager.modelsState.collect { models ->
                val downloaded = models.values
                    .filter { it.isDownloaded }
                    .map { it.language }
                    .sortedBy { it.displayName }
                _uiState.update {
                    it.copy(
                        downloadedLanguages = downloaded,
                        allLanguageModels = models.values.toList().sortedBy { m -> m.language.displayName },
                    )
                }
            }
        }

        // Initial model status refresh
        viewModelScope.launch {
            checkPermissions()
            languageModelManager.refreshModelStatus()
        }
    }

    fun checkPermissions() {
        val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(getApplication())
        } else {
            true
        }
        _uiState.update { it.copy(hasOverlayPermission = hasOverlay) }
    }

    fun refreshModels() {
        viewModelScope.launch {
            languageModelManager.refreshModelStatus()
        }
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setServiceEnabled(enabled)
        }
    }

    fun setEngineType(engineType: EngineType) {
        viewModelScope.launch {
            prefsRepository.setEngineType(engineType)
        }
    }

    fun setTargetLanguage(languageCode: String) {
        viewModelScope.launch {
            prefsRepository.setTargetLanguage(languageCode)
        }
    }

    fun setGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            prefsRepository.setGeminiApiKey(apiKey)
        }
    }

    fun downloadLanguageModel(language: SupportedLanguage) {
        viewModelScope.launch {
            try {
                languageModelManager.downloadModel(language)
            } catch (e: Exception) {
                // Error is reflected in modelsState (isDownloading = false, isDownloaded = false)
            }
        }
    }

    fun deleteLanguageModel(language: SupportedLanguage) {
        viewModelScope.launch {
            try {
                languageModelManager.deleteModel(language)
                // If the deleted language was the selected target, clear it
                if (_uiState.value.targetLanguage == language.code) {
                    prefsRepository.setTargetLanguage("")
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleLanguageManager(show: Boolean) {
        _uiState.update { it.copy(showLanguageManager = show) }
    }

    fun updateOverlayPermission(granted: Boolean) {
        _uiState.update { it.copy(hasOverlayPermission = granted) }
    }
}
