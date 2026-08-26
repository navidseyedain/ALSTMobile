package com.alst.mobile.core.translator

interface TranslationEngine {
    /**
     * Translate a list of text blocks to the target language.
     * Source language is auto-detected.
     * @param texts List of text strings to translate.
     * @param targetLanguage BCP-47 language code (e.g., "fa", "en", "de").
     * @return List of translated strings in the same order.
     */
    suspend fun translate(texts: List<String>, targetLanguage: String): List<String>

    /**
     * Check if this engine is available and ready to translate.
     */
    suspend fun isAvailable(): Boolean

    suspend fun recognizeAndTranslate(
        bitmap: android.graphics.Bitmap,
        targetLanguage: String
    ): List<com.alst.mobile.domain.model.TranslationBlock>? {
        return null
    }
}
