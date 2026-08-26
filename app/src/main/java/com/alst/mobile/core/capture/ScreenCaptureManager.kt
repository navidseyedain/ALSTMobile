package com.alst.mobile.core.capture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureManager(
    private val context: Context,
    private val onCaptureStopped: () -> Unit = {}
) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false

    private var reusableBitmap: Bitmap? = null
    private var lastProcessTime = 0L
    private var screenWidth = 0
    private var screenHeight = 0

    @SuppressLint("WrongConstant")
    fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) return

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopCapture()
                onCaptureStopped()
            }
        }, null)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val currentTime = System.currentTimeMillis()
                    // Throttle to ~3 FPS to drastically save CPU/Battery
                    if (currentTime - lastProcessTime > 300) {
                        try {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * screenWidth

                            val requiredWidth = screenWidth + rowPadding / pixelStride
                            if (reusableBitmap == null || reusableBitmap!!.width != requiredWidth) {
                                reusableBitmap = Bitmap.createBitmap(
                                    requiredWidth,
                                    screenHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                            }
                            reusableBitmap!!.copyPixelsFromBuffer(buffer)
                            lastProcessTime = currentTime
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    image.close()
                }
            }, null)
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ALST_ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        isCapturing = true
    }

    fun getLatestScreenshot(): Bitmap? {
        val current = reusableBitmap ?: return null
        return try {
            Bitmap.createBitmap(current, 0, 0, screenWidth, screenHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        reusableBitmap = null
    }
}
