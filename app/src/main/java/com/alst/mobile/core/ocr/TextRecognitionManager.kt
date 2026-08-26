package com.alst.mobile.core.ocr

import android.graphics.Bitmap
import android.graphics.Color
import com.alst.mobile.domain.model.TranslationBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognitionManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap, rotationDegrees: Int): List<TranslationBlock> {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        return try {
            val result = recognizer.process(inputImage).await()
            val blocks = mutableListOf<TranslationBlock>()

            for (textBlock in result.textBlocks) {
                val boundingBox = textBlock.boundingBox
                val text = textBlock.text
                if (boundingBox != null && text.isNotBlank()) {
                    
                    val bgColor = extractBackgroundColor(bitmap, boundingBox)
                    val isBgDark = isColorDark(bgColor)
                    val textColor = if (isBgDark) Color.WHITE else Color.BLACK
                    
                    val lineCount = maxOf(1, textBlock.lines.size)
                    val lineHeightPx = boundingBox.height() / lineCount

                    blocks.add(
                        TranslationBlock(
                            originalText = text,
                            translatedText = "",
                            boundingBox = boundingBox,
                            backgroundColor = bgColor,
                            textColor = textColor,
                            lineHeightPx = lineHeightPx
                        )
                    )
                }
            }
            blocks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun extractBackgroundColor(bitmap: Bitmap, rect: android.graphics.Rect): Int {
        val colors = mutableListOf<Int>()
        
        val left = maxOf(0, rect.left - 2)
        val right = minOf(bitmap.width - 1, rect.right + 2)
        val top = maxOf(0, rect.top - 2)
        val bottom = minOf(bitmap.height - 1, rect.bottom + 2)
        
        if (left >= right || top >= bottom) return Color.parseColor("#202124")
        
        for (x in left..right step 4) {
            colors.add(bitmap.getPixel(x, top))
            colors.add(bitmap.getPixel(x, bottom))
        }
        for (y in top..bottom step 4) {
            colors.add(bitmap.getPixel(left, y))
            colors.add(bitmap.getPixel(right, y))
        }
        
        if (colors.isEmpty()) return Color.parseColor("#202124")
        
        val colorCounts = mutableMapOf<Int, Int>()
        for (c in colors) {
            // Quantize colors to group similar ones (strip lower 3 bits)
            val quantized = c and 0xFFF8F8F8.toInt()
            colorCounts[quantized] = colorCounts.getOrDefault(quantized, 0) + 1
        }
        
        return colorCounts.maxByOrNull { it.value }?.key ?: Color.parseColor("#202124")
    }

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = Math.sqrt(
            0.299 * (r * r) + 0.587 * (g * g) + 0.114 * (b * b)
        )
        return luminance < 130.0
    }
}
