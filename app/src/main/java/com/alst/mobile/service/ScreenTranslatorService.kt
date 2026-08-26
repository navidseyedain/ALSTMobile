package com.alst.mobile.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.alst.mobile.R
import com.alst.mobile.core.capture.CapturePermissionActivity
import com.alst.mobile.core.capture.ScreenCaptureManager
import com.alst.mobile.core.ocr.TextRecognitionManager
import com.alst.mobile.core.overlay.OverlayManager
import com.alst.mobile.core.translator.TranslationEngineFactory
import com.alst.mobile.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenTranslatorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var captureManager: ScreenCaptureManager
    private lateinit var ocrManager: TextRecognitionManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var prefsRepository: AppPreferencesRepository

    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        captureManager = ScreenCaptureManager(this) {
            // Stop the foreground service if user revokes capture from System UI
            stopSelf()
        }
        ocrManager = TextRecognitionManager()
        prefsRepository = AppPreferencesRepository(this)
        
        overlayManager = OverlayManager(
            context = this,
            onTranslateClicked = { triggerTranslation() }
        )
        
        // Show FAB when service is created
        overlayManager.showFab()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()

        when (intent?.action) {
            ACTION_INIT -> {
                val permIntent = Intent(this, CapturePermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(permIntent)
            }
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                
                if (resultData != null) {
                    captureManager.startCapture(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun triggerTranslation() {
        if (overlayManager.isProcessing) return
        
        // If overlay is already showing, just hide it and do nothing else
        if (overlayManager.isOverlayShowing()) {
            overlayManager.hideTranslationOverlay()
            return
        }

        overlayManager.isProcessing = true
        
        val bitmap = captureManager.getLatestScreenshot()
        if (bitmap != null) {
            serviceScope.launch {
                processImage(bitmap)
            }
        } else {
            Toast.makeText(this, "Failed to capture screen. Please try again.", Toast.LENGTH_SHORT).show()
            overlayManager.isProcessing = false
        }
    }

    private suspend fun processImage(bitmap: Bitmap) {
        try {
            val settings = prefsRepository.settingsFlow.first()
            if (settings.targetLanguage.isBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScreenTranslatorService, "Please select a target language first.", Toast.LENGTH_LONG).show()
                }
                overlayManager.isProcessing = false
                return
            }

            val engine = com.alst.mobile.core.translator.TranslationEngineFactory.create(settings.engineType, settings.geminiApiKey)
            if (!engine.isAvailable()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScreenTranslatorService, "Gemini API Key missing! Please enter your key in settings.", Toast.LENGTH_LONG).show()
                }
                overlayManager.isProcessing = false
                return
            }

            var finalBlocks: List<com.alst.mobile.domain.model.TranslationBlock>? = null

            if (settings.engineType == com.alst.mobile.domain.model.EngineType.ONLINE_GEMINI) {
                // Online mode: Single pass multimodal OCR + Translation with Gemini
                finalBlocks = engine.recognizeAndTranslate(bitmap, settings.targetLanguage)
                if (finalBlocks.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScreenTranslatorService, "No text detected on screen.", Toast.LENGTH_SHORT).show()
                    }
                    overlayManager.isProcessing = false
                    return
                }
            } else {
                // Offline mode: ML Kit on-device OCR + ML Kit Translate
                val blocks = ocrManager.recognizeText(bitmap, 0)
                if (blocks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScreenTranslatorService, "No text detected on screen.", Toast.LENGTH_SHORT).show()
                    }
                    overlayManager.isProcessing = false
                    return
                }

                val originalTexts = blocks.map { it.originalText }
                val translatedTexts = engine.translate(originalTexts, settings.targetLanguage)

                finalBlocks = blocks.mapIndexed { index, block ->
                    block.copy(translatedText = translatedTexts.getOrNull(index) ?: block.originalText)
                }
            }

            // Show Translation Overlay
            withContext(Dispatchers.Main) {
                overlayManager.showTranslationOverlay(finalBlocks)
                overlayManager.isProcessing = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = e.message ?: "Translation processing failed."
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ScreenTranslatorService, errorMsg, Toast.LENGTH_LONG).show()
            }
            overlayManager.isProcessing = false
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Screen translation service is active")
            .setSmallIcon(R.drawable.ic_translate)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.stopCapture()
        overlayManager.cleanup()
        serviceScope.launch {
            prefsRepository.setServiceEnabled(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Translator Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_INIT = "ACTION_INIT"
        const val ACTION_START_CAPTURE = "ACTION_START_CAPTURE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        private const val CHANNEL_ID = "alst_service_channel"
        private const val NOTIFICATION_ID = 1
    }
}
