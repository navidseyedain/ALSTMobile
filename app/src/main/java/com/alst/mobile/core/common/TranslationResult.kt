package com.alst.mobile.core.common

sealed class TranslationResult<out T> {
    data class Success<T>(val data: T) : TranslationResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : TranslationResult<Nothing>()
    data object Loading : TranslationResult<Nothing>()
}
