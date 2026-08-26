package com.alst.mobile.domain.model

data class LanguageModelInfo(
    val language: SupportedLanguage,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
)
