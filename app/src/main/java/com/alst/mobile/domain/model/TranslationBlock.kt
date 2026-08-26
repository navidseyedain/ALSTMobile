package com.alst.mobile.domain.model

import android.graphics.Rect

data class TranslationBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,
    val backgroundColor: Int = 0xFF202124.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val lineHeightPx: Int = 0
)
