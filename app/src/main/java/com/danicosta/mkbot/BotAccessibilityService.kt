package com.danicosta.mkbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast
import kotlin.math.abs

class BotAccessibilityService : AccessibilityService() {

    companion object {
        var instance: BotAccessibilityService? = null
        var isRunning: Boolean = false
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null

    @Volatile var lastAction: String = "-"
    @Volatile var lastGestureStatus: String = ""

    private val labelTicker = object : Runnable {
        override fun run() {
            updateOverlayLabel()
            mainHandler.postDelayed(this, 300L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        MemoryManager.init(applicationContext)
        addOverlayButton()
        toast("MK Bot: serviço conectado")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        BotBrain.stop()
        mainHandler.removeCallbacks(labelTicker)
        removeOverlayButton()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) startBot()
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) stopBot()
        }
        return true
    }

    private fun addOverlayButton() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        val button = Button(this).apply {
            text = "▶"
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC2E7D32"))
        }
        overlayButton = button

        var startX = 0f
        var startY = 0f
        var startTouchX = 0f
        var startTouchY = 0f
        var moved = false

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x.toFloat()
                    startY = params.y.toFloat()
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val dy = event.rawY - startTouchY
                    if (abs(dx) > 12 || abs(dy) > 12) moved = true
                    params.x = (startX + dx).toInt()
                    params.y = (startY + dy).toInt()
                    windowManager?.updateViewLayout(button, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleBot()
                    true
                }
                else -> false
            }
        }

        wm.addView(button, params)
    }

    private fun removeOverlayButton() {
        overlayButton?.let { windowManager?.removeView(it) }
        overlayButton = null
    }

    private fun toggleBot() {
        if (isRunning) stopBot() else startBot()
    }

    private fun updateOverlayLabel() {
        mainHandler.post {
            overlayButton?.text = if (isRunning) "$lastAction$lastGestureStatus" else "▶"
            overlayButton?.setBackgroundColor(
                Color.parseColor(if (isRunning) "#CCC62828" else "#CC2E7D32")
            )
        }
    }

    private fun startBot() {
        if (isRunning) return
        isRunning = true
        lastAction = "..."
        lastGestureStatus = ""
        BotBrain.start()
        mainHandler.post(labelTicker)
        toast("MK Bot: jogando")
    }

    private fun stopBot() {
        if (!isRunning) return
        isRunning = false
        BotBrain.stop()
        mainHandler.removeCallbacks(labelTicker)
        MemoryManager.save()
        updateOverlayLabel()
        toast("MK Bot: parado — progresso salvo")
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show() }
    }

    fun tap(point: PointF, durationMs: Long = 50L) {
        mainHandler.post {
            val path = Path().apply { moveTo(point.x, point.y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            val queued = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    lastGestureStatus = ""
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    lastGestureStatus = " X"
                }
            }, null)
            if (!queued) lastGestureStatus = " X!"
        }
    }

    fun tap(action: Action, durationMs: Long = 50L) = tap(action.toPoint(), durationMs)

    fun multiTap(points: List<PointF>, durationMs: Long = 50L) {
        mainHandler.post {
            val builder = GestureDescription.Builder()
            points.forEach { p ->
                val path = Path().apply { moveTo(p.x, p.y) }
                builder.addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            }
            dispatchGesture(builder.build(), null, null)
        }
    }

    fun sequence(actions: List<Action>, stepDelayMs: Long = 120L) {
        actions.forEachIndexed { index, action ->
            mainHandler.postDelayed({ tap(action) }, index * stepDelayMs)
        }
    }

    fun directionThenButton(
        direction: PointF,
        button: PointF,
        holdMs: Long = 220L,
        pressAtMs: Long = 100L,
        pressDurationMs: Long = 50L
    ) {
        mainHandler.post {
            val dirPath = Path().apply { moveTo(direction.x, direction.y) }
            val dirStroke = GestureDescription.StrokeDescription(dirPath, 0, holdMs)
            val btnPath = Path().apply { moveTo(button.x, button.y) }
            val btnStroke = GestureDescription.StrokeDescription(btnPath, pressAtMs, pressDurationMs)
            val gesture = GestureDescription.Builder().addStroke(dirStroke).addStroke(btnStroke).build()
            dispatchGesture(gesture, null, null)
        }
    }
}
