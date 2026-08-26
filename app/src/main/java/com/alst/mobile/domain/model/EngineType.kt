package com.alst.mobile.domain.model

enum class EngineType(val key: String, val displayNameResId: Int) {
    OFFLINE("offline", com.alst.mobile.R.string.engine_offline),
    ONLINE_GEMINI("online_gemini", com.alst.mobile.R.string.engine_online);

    companion object {
        fun fromKey(key: String): EngineType {
            return entries.find { it.key == key } ?: OFFLINE
        }
    }
}
