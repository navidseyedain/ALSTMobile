package com.alst.mobile.core.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

class OfflineTranslationEngine : TranslationEngine {

    override suspend fun translate(texts: List<String>, targetLanguage: String): List<String> {
        if (texts.isEmpty()) return emptyList()

        // ML Kit auto-detects source language when source is not specified.
        // However, the API requires a source language. We use identify + translate pattern.
        // For simplicity, we'll use English as fallback source and let ML Kit handle it.
        // A more robust approach would use LanguageIdentification first.
        val results = mutableListOf<String>()

        for (text in texts) {
            val sourceLanguage = identifyLanguage(text)
            if (sourceLanguage == targetLanguage) {
                results.add(text) // No need to translate
                continue
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            val translator = Translation.getClient(options)

            try {
                val translated = translator.translate(text).await()
                results.add(translated)
            } catch (e: Exception) {
                // If translation fails, return original text
                results.add(text)
            } finally {
                translator.close()
            }
        }

        return results
    }

    private suspend fun identifyLanguage(text: String): String {
        return try {
            val identifier = com.google.mlkit.nl.languageid.LanguageIdentification.getClient()
            val langCode = identifier.identifyLanguage(text).await()
            identifier.close()
            if (langCode == "und") TranslateLanguage.ENGLISH else langCode
        } catch (e: Exception) {
            TranslateLanguage.ENGLISH
        }
    }

    override suspend fun isAvailable(): Boolean = true
}
