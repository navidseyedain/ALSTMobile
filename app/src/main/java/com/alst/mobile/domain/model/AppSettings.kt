package com.alst.mobile.domain.model

data class AppSettings(
    val isServiceEnabled: Boolean = false,
    val engineType: EngineType = EngineType.OFFLINE,
    val targetLanguage: String = "en",
    val geminiApiKey: String = "",
)
