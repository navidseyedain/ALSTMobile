package com.alst.mobile.core.translator

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.Color
import com.alst.mobile.domain.model.TranslationBlock
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONArray
import org.json.JSONObject

class GeminiTranslationEngine(
    private val apiKey: String,
) : TranslationEngine {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            },
            systemInstruction = content {
                text(
                    "You are a professional translator that produces natural, fluent translations like Google Lens. " +
                    "You will receive a JSON array of paragraph text strings extracted from a screen. " +
                    "For each paragraph, produce a complete, natural translation into the target language. " +
                    "Rules: " +
                    "1. Translate the MEANING, not word-by-word. Produce natural flowing text in the target language. " +
                    "2. Keep proper nouns, brand names, model names (e.g. MAI-Image-2.6, GPT, Gemini) in their original form. " +
                    "3. Keep numbers and percentages as-is. " +
                    "4. Do NOT keep any English words that have a natural equivalent in the target language. " +
                    "5. Each translated paragraph should read as if it was originally written in the target language. " +
                    "6. Return ONLY a JSON array of translated strings in the exact same order. " +
                    "Do NOT add any explanation or wrapper."
                )
            },
        )
    }

    override suspend fun translate(texts: List<String>, targetLanguage: String): List<String> {
        if (texts.isEmpty()) return emptyList()

        val languageName = com.alst.mobile.domain.model.SupportedLanguage.fromCode(targetLanguage)?.displayName ?: targetLanguage
        val inputJson = JSONArray(texts).toString()
        val prompt = """
            You are a professional translator. Translate the following JSON array of texts into $languageName.
            Return ONLY a valid JSON array of strings containing the translations, in the exact same order.
            Do not include markdown, backticks, or any other explanations.
            
            Texts:
            $inputJson
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            var responseText = response.text?.trim() ?: return texts
            
            // Clean markdown formatting if present
            if (responseText.startsWith("```json", ignoreCase = true)) {
                responseText = responseText.substring(7)
            } else if (responseText.startsWith("```")) {
                responseText = responseText.substring(3)
            }
            if (responseText.endsWith("```")) {
                responseText = responseText.substring(0, responseText.length - 3)
            }
            responseText = responseText.trim()

            val jsonArray = JSONArray(responseText)
            val results = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                results.add(jsonArray.getString(i))
            }

            if (results.size != texts.size) {
                texts
            } else {
                results
            }
        } catch (e: Exception) {
            throw Exception(parseErrorMessage(e), e)
        }
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun recognizeAndTranslate(
        bitmap: Bitmap,
        targetLanguage: String
    ): List<TranslationBlock>? {
        val languageName = com.alst.mobile.domain.model.SupportedLanguage.fromCode(targetLanguage)?.displayName ?: targetLanguage
        val prompt = """
            You are a professional translator and OCR engine.
            Analyze this image, extract all text blocks, and translate them to $languageName.
            Detect any source language automatically (including Persian, Japanese, etc.).
            Return ONLY a valid JSON array of objects, with NO markdown formatting.
            CRITICAL: Do NOT group completely separate paragraphs into a single giant block. Keep distinct text areas separate!
            Each object MUST have:
            1. "text": the original extracted text
            2. "translated": the translated text in $languageName
            3. "lines_count": the exact number of physical lines this text occupies in the original image
            4. "box_2d": an array of 4 integers [ymin, xmin, ymax, xmax] normalized to 0-1000 representing the bounding box.
            
            Example format:
            [
              {"text": "Hello\nWorld", "translated": "سلام دنیا", "lines_count": 2, "box_2d": [100, 200, 150, 400]}
            ]
        """.trimIndent()

        val promptContent = content {
            image(bitmap)
            text(prompt)
        }

        return try {
            val response = generativeModel.generateContent(promptContent)
            var responseText = response.text?.trim() ?: return emptyList()
            
            // Clean markdown formatting if present
            if (responseText.startsWith("```json", ignoreCase = true)) {
                responseText = responseText.substring(7)
            } else if (responseText.startsWith("```")) {
                responseText = responseText.substring(3)
            }
            if (responseText.endsWith("```")) {
                responseText = responseText.substring(0, responseText.length - 3)
            }
            responseText = responseText.trim()

            val jsonArray = JSONArray(responseText)
            val blocks = mutableListOf<TranslationBlock>()
            
            val width = bitmap.width
            val height = bitmap.height

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val originalText = obj.optString("text", "")
                val translated = obj.optString("translated", "")
                val linesCount = maxOf(1, obj.optInt("lines_count", 1))
                val boxArray = obj.optJSONArray("box_2d")
                
                if (boxArray != null && boxArray.length() == 4) {
                    val ymin = boxArray.getInt(0)
                    val xmin = boxArray.getInt(1)
                    val ymax = boxArray.getInt(2)
                    val xmax = boxArray.getInt(3)
                    
                    val rect = Rect(
                        (xmin * width / 1000),
                        (ymin * height / 1000),
                        (xmax * width / 1000),
                        (ymax * height / 1000)
                    )
                    
                    blocks.add(
                        TranslationBlock(
                            originalText = originalText,
                            translatedText = translated,
                            boundingBox = rect,
                            backgroundColor = Color.parseColor("#E6202124"),
                            textColor = Color.WHITE,
                            lineHeightPx = maxOf(1, rect.height() / linesCount)
                        )
                    )
                }
            }
            
            blocks
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception(parseErrorMessage(e), e)
        }
    }

    companion object {
        fun parseErrorMessage(e: Throwable): String {
            val message = e.message ?: ""
            val lower = message.lowercase()

            return when {
                lower.contains("api_key_invalid") || 
                lower.contains("api key not valid") || 
                lower.contains("invalid api key") ||
                lower.contains("api_key") -> {
                    "Invalid Gemini API Key. Please verify your key in settings."
                }
                lower.contains("resource_exhausted") || 
                lower.contains("429") || 
                lower.contains("quota") -> {
                    "API rate limit exceeded. Please wait a minute and try again."
                }
                lower.contains("user_location_blocked") || 
                lower.contains("location_not_supported") || 
                lower.contains("not supported in your country") -> {
                    "Gemini is restricted in your region. Please connect to a VPN."
                }
                e is java.net.UnknownHostException || 
                lower.contains("unable to resolve host") || 
                lower.contains("no address associated with hostname") -> {
                    "Cannot reach Gemini servers. Please check your internet or VPN."
                }
                e is java.net.SocketTimeoutException || lower.contains("timeout") -> {
                    "Connection timed out. Please check your network speed."
                }
                lower.contains("safety") || lower.contains("blocked") -> {
                    "Content was blocked by AI safety policies."
                }
                else -> {
                    "Translation service error: ${if (message.length > 80) message.take(80) + "..." else message}"
                }
            }
        }
    }
}
