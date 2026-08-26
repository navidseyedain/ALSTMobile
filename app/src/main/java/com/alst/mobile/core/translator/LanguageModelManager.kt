package com.alst.mobile.core.translator

import com.alst.mobile.domain.model.LanguageModelInfo
import com.alst.mobile.domain.model.SupportedLanguage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class LanguageModelManager {

    private val modelManager = RemoteModelManager.getInstance()

    private val _modelsState = MutableStateFlow<Map<String, LanguageModelInfo>>(emptyMap())
    val modelsState: StateFlow<Map<String, LanguageModelInfo>> = _modelsState.asStateFlow()

    /**
     * Refresh the download status of all supported languages.
     * Queries ML Kit RemoteModelManager to check which translation models are downloaded.
     */
    suspend fun refreshModelStatus() {
        val downloadedModels = try {
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        } catch (e: Exception) {
            emptySet()
        }

        val downloadedCodes = downloadedModels.map { it.language }.toSet()

        val updatedMap = SupportedLanguage.entries.associate { lang ->
            lang.code to LanguageModelInfo(
                language = lang,
                isDownloaded = lang.code in downloadedCodes,
                isDownloading = _modelsState.value[lang.code]?.isDownloading ?: false,
            )
        }
        _modelsState.value = updatedMap
    }

    /**
     * Get only the languages whose translation models have been downloaded.
     */
    fun getDownloadedLanguages(): List<SupportedLanguage> {
        return _modelsState.value.values
            .filter { it.isDownloaded }
            .map { it.language }
            .sortedBy { it.displayName }
    }

    /**
     * Download the translation model for a specific language.
     */
    suspend fun downloadModel(language: SupportedLanguage) {
        val model = TranslateRemoteModel.Builder(language.code).build()
        val conditions = DownloadConditions.Builder().build()

        // Mark as downloading
        _modelsState.update { current ->
            current.toMutableMap().apply {
                this[language.code] = LanguageModelInfo(
                    language = language,
                    isDownloaded = false,
                    isDownloading = true,
                )
            }
        }

        try {
            modelManager.download(model, conditions).await()
            // Mark as downloaded
            _modelsState.update { current ->
                current.toMutableMap().apply {
                    this[language.code] = LanguageModelInfo(
                        language = language,
                        isDownloaded = true,
                        isDownloading = false,
                    )
                }
            }
        } catch (e: Exception) {
            // Mark download failed
            _modelsState.update { current ->
                current.toMutableMap().apply {
                    this[language.code] = LanguageModelInfo(
                        language = language,
                        isDownloaded = false,
                        isDownloading = false,
                    )
                }
            }
            throw e
        }
    }

    /**
     * Delete the translation model for a specific language.
     */
    suspend fun deleteModel(language: SupportedLanguage) {
        val model = TranslateRemoteModel.Builder(language.code).build()
        try {
            modelManager.deleteDownloadedModel(model).await()
            _modelsState.update { current ->
                current.toMutableMap().apply {
                    this[language.code] = LanguageModelInfo(
                        language = language,
                        isDownloaded = false,
                        isDownloading = false,
                    )
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
