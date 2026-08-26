package com.alst.mobile.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
    val ENGINE_TYPE = stringPreferencesKey("engine_type")
    val TARGET_LANGUAGE = stringPreferencesKey("target_language")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
}
