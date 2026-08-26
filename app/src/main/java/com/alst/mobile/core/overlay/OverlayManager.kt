package com.alst.mobile.core.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alst.mobile.R
import com.alst.mobile.domain.model.TranslationBlock
import com.alst.mobile.ui.theme.OverlayColors
import kotlin.math.roundToInt

class OverlayManager(
    private val context: Context,
    private val onTranslateClicked: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Get actual screen density for px->dp conversion
    private val screenDensity: Float by lazy {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.density
    }
    
    private var fabView: ComposeView? = null
    private var overlayView: ComposeView? = null
    
    private var fabLifecycleOwner: ComposeOverlayLifecycleOwner? = null
    private var overlayContainer: android.widget.FrameLayout? = null
    private var overlayLifecycleOwner: ComposeOverlayLifecycleOwner? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { hideTranslationOverlay() }

    private var translationBlocks by mutableStateOf<List<TranslationBlock>>(emptyList())
    var isProcessing by mutableStateOf(false)

    private val systemDialogsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS || intent?.action == Intent.ACTION_SCREEN_OFF) {
                hideTranslationOverlay()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(systemDialogsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(systemDialogsReceiver, filter)
        }
    }

    fun isOverlayShowing(): Boolean = overlayContainer != null
    
    /** Convert raw pixel value from VirtualDisplay/MLKit to Compose Dp */
    private fun Int.pxToDp(): Dp = (this / screenDensity).dp

    fun showFab() {
        if (fabView != null) return

        fabView = ComposeView(context).apply {
            setContent {
                FabContent()
            }
        }

        fabLifecycleOwner = ComposeOverlayLifecycleOwner().apply { attachToView(fabView!!) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager.addView(fabView, params)
    }

    fun hideFab() {
        fabView?.let {
            windowManager.removeView(it)
            fabLifecycleOwner?.destroy()
            fabView = null
            fabLifecycleOwner = null
        }
    }

    private var windowOffsetX by mutableStateOf(0)
    private var windowOffsetY by mutableStateOf(0)

    fun showTranslationOverlay(blocks: List<TranslationBlock>) {
        translationBlocks = blocks

        if (overlayContainer == null) {
            overlayContainer = object : android.widget.FrameLayout(context) {
                override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                    if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                        hideTranslationOverlay()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            
            // Calculate exact physical location of the window on the screen
            // to offset any system insets, notch push-downs, or status bar gaps.
            overlayContainer!!.viewTreeObserver.addOnGlobalLayoutListener {
                val location = IntArray(2)
                overlayContainer!!.getLocationOnScreen(location)
                windowOffsetX = location[0]
                windowOffsetY = location[1]
            }

            overlayView = ComposeView(context).apply {
                setContent {
                    TranslationOverlayContent(
                        blocks = translationBlocks,
                        winOffsetX = windowOffsetX,
                        winOffsetY = windowOffsetY
                    )
                }
            }
            
            overlayContainer!!.addView(overlayView)
            
            overlayLifecycleOwner = ComposeOverlayLifecycleOwner().apply { attachToView(overlayContainer!!) }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            windowManager.addView(overlayContainer, params)
        }
        
        // Reset 30-second timeout
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, 30_000)
    }

    fun hideTranslationOverlay() {
        mainHandler.removeCallbacks(timeoutRunnable)
        overlayContainer?.let {
            windowManager.removeView(it)
            overlayLifecycleOwner?.destroy()
            overlayContainer = null
            overlayView = null
            overlayLifecycleOwner = null
        }
    }

    @Composable
    private fun FabContent() {
        var showCloseOptions by androidx.compose.runtime.remember { mutableStateOf(false) }

        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp) // padding so the shadow/ripple doesn't get cut off by Window bounds
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)) // Modern sleek dark glass
                    .border(1.5.dp, Color(0x33FFFFFF), CircleShape) // Subtle white border
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            updateFabPosition(dragAmount.x, dragAmount.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { 
                                if (showCloseOptions) {
                                    showCloseOptions = false
                                } else {
                                    onTranslateClicked()
                                }
                            },
                            onLongPress = { showCloseOptions = true }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_translate),
                        contentDescription = "Translate",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showCloseOptions,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xEEFF3B30)) // Sleek red glass
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable {
                            val intent = Intent(context, com.alst.mobile.service.ScreenTranslatorService::class.java).apply {
                                action = com.alst.mobile.service.ScreenTranslatorService.ACTION_STOP_SERVICE
                            }
                            context.startService(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    private fun updateFabPosition(dx: Float, dy: Float) {
        fabView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x += dx.roundToInt()
            params.y += dy.roundToInt()
            windowManager.updateViewLayout(view, params)
        }
    }

    @Composable
    private fun TranslationOverlayContent(
        blocks: List<TranslationBlock>,
        winOffsetX: Int,
        winOffsetY: Int
    ) {
        // LTR root to ensure absolute offset(x, y) maps accurately to physical screen coordinates
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { hideTranslationOverlay() }
                    )
                }
        ) {
            blocks.forEach { block ->
                if (block.translatedText.isNotBlank()) {
                    val rect = block.boundingBox
                    // Subtract the window offset to map absolute screen pixels to window-relative pixels
                    val xDp = with(LocalDensity.current) { (rect.left - winOffsetX).toDp() }
                    val yDp = with(LocalDensity.current) { (rect.top - winOffsetY).toDp() }
                    val widthDp = with(LocalDensity.current) { rect.width().toDp() }
                    val minHeightDp = with(LocalDensity.current) { rect.height().toDp() }
                    
                    // Dynamically calculate font size from pixel line height using the real density
                    // We multiply by 0.65f because a font's line-height is typically larger than its SP value
                    // e.g., if a line takes 50px, the actual text height is around 32px
                    val fontSizeSp = with(LocalDensity.current) { (block.lineHeightPx * 0.65f).toSp() }
                    val clampedSp = fontSizeSp.value.coerceIn(10f, 22f).sp
                    val dynamicLineHeight = clampedSp * 1.3f

                    val extraH = 4.dp
                    val extraV = 2.dp

                    Box(
                        modifier = Modifier
                            .offset(x = xDp - extraH, y = yDp - extraV)
                            .width(widthDp + (extraH * 2))
                            .defaultMinSize(minHeight = minHeightDp + (extraV * 2))
                            .background(Color(block.backgroundColor), RoundedCornerShape(4.dp))
                            .padding(horizontal = extraH, vertical = extraV),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = block.translatedText,
                                color = Color(block.textColor),
                                fontSize = clampedSp,
                                textAlign = TextAlign.Right,
                                lineHeight = dynamicLineHeight,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun cleanup() {
        hideFab()
        hideTranslationOverlay()
        try {
            context.unregisterReceiver(systemDialogsReceiver)
        } catch (e: Exception) {}
    }
}
