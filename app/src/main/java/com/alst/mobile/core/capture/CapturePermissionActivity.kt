package com.alst.mobile.core.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.alst.mobile.service.ScreenTranslatorService

class CapturePermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_PERM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CAPTURE_PERM) {
            if (resultCode == RESULT_OK && data != null) {
                // Pass the permission token to the service
                val serviceIntent = Intent(this, ScreenTranslatorService::class.java).apply {
                    action = ScreenTranslatorService.ACTION_START_CAPTURE
                    putExtra(ScreenTranslatorService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenTranslatorService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(serviceIntent)
            } else {
                // Permission denied, stop the service
                val stopIntent = Intent(this, ScreenTranslatorService::class.java).apply {
                    action = ScreenTranslatorService.ACTION_STOP_SERVICE
                }
                startService(stopIntent)
            }
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE_CAPTURE_PERM = 1001
    }
}
