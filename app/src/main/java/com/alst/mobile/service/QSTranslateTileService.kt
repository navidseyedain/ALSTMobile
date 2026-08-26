package com.alst.mobile.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alst.mobile.MainActivity
import com.alst.mobile.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QSTranslateTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var listeningJob: Job? = null
    private lateinit var prefsRepository: AppPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        prefsRepository = AppPreferencesRepository(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob = serviceScope.launch {
            prefsRepository.settingsFlow.collect { settings ->
                updateTileState(settings.isServiceEnabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentState = prefsRepository.settingsFlow.first().isServiceEnabled
            val newState = !currentState

            if (newState) {
                // User wants to turn ON
                if (Settings.canDrawOverlays(this@QSTranslateTileService)) {
                    prefsRepository.setServiceEnabled(true)
                    val initIntent = Intent(this@QSTranslateTileService, ScreenTranslatorService::class.java).apply {
                        action = ScreenTranslatorService.ACTION_INIT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(initIntent)
                    } else {
                        startService(initIntent)
                    }
                } else {
                    // Needs permission, redirect to app
                    prefsRepository.setServiceEnabled(false)
                    
                    val intent = Intent(this@QSTranslateTileService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pendingIntent = PendingIntent.getActivity(
                            this@QSTranslateTileService,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
            } else {
                // User wants to turn OFF
                prefsRepository.setServiceEnabled(false)
                val stopIntent = Intent(this@QSTranslateTileService, ScreenTranslatorService::class.java).apply {
                    action = ScreenTranslatorService.ACTION_STOP_SERVICE
                }
                startService(stopIntent)
            }
        }
    }

    private fun updateTileState(isEnabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
