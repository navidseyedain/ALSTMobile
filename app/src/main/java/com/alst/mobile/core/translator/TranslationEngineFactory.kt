package com.alst.mobile.core.translator

import com.alst.mobile.domain.model.EngineType

object TranslationEngineFactory {

    fun create(engineType: EngineType, geminiApiKey: String = ""): TranslationEngine {
        return when (engineType) {
            EngineType.OFFLINE -> OfflineTranslationEngine()
            EngineType.ONLINE_GEMINI -> GeminiTranslationEngine(apiKey = geminiApiKey)
        }
    }
}
