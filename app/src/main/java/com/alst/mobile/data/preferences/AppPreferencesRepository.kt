package com.alst.mobile.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.alst.mobile.domain.model.AppSettings
import com.alst.mobile.domain.model.EngineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "alst_settings")

class AppPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.appDataStore.data.map { prefs ->
        AppSettings(
            isServiceEnabled = prefs[PreferencesKeys.IS_SERVICE_ENABLED] ?: false,
            engineType = EngineType.fromKey(
                prefs[PreferencesKeys.ENGINE_TYPE] ?: EngineType.OFFLINE.key
            ),
            targetLanguage = prefs[PreferencesKeys.TARGET_LANGUAGE] ?: "en",
            geminiApiKey = prefs[PreferencesKeys.GEMINI_API_KEY] ?: "",
        )
    }

    val isServiceEnabledFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_SERVICE_ENABLED] ?: false
    }

    val engineTypeFlow: Flow<EngineType> = context.appDataStore.data.map { prefs ->
        EngineType.fromKey(prefs[PreferencesKeys.ENGINE_TYPE] ?: EngineType.OFFLINE.key)
    }

    val targetLanguageFlow: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[PreferencesKeys.TARGET_LANGUAGE] ?: "en"
    }

    val geminiApiKeyFlow: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[PreferencesKeys.GEMINI_API_KEY] ?: ""
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setEngineType(engineType: EngineType) {
        context.appDataStore.edit { prefs ->
            prefs[PreferencesKeys.ENGINE_TYPE] = engineType.key
        }
    }

    suspend fun setTargetLanguage(language: String) {
        context.appDataStore.edit { prefs ->
            prefs[PreferencesKeys.TARGET_LANGUAGE] = language
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.appDataStore.edit { prefs ->
            prefs[PreferencesKeys.GEMINI_API_KEY] = apiKey
        }
    }
}
