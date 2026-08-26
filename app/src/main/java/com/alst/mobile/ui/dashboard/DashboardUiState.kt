package com.alst.mobile.ui.dashboard

import com.alst.mobile.domain.model.EngineType
import com.alst.mobile.domain.model.LanguageModelInfo
import com.alst.mobile.domain.model.SupportedLanguage

data class DashboardUiState(
    val isServiceEnabled: Boolean = false,
    val engineType: EngineType = EngineType.OFFLINE,
    val targetLanguage: String = "",
    val geminiApiKey: String = "",
    val downloadedLanguages: List<SupportedLanguage> = emptyList(),
    val allLanguageModels: List<LanguageModelInfo> = emptyList(),
    val hasOverlayPermission: Boolean = false,
    val showLanguageManager: Boolean = false,
)
