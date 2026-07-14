package com.danicosta.mkbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class BotAccessibilityService : AccessibilityService() {

    companion object {
        var instance: BotAccessibilityService? = null
        var isRunning: Boolean = false
            private set
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        toast("MK Bot: serviço conectado")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Não usamos a árvore de acessibilidade — a visão vem da captura de tela (próximo arquivo).
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) startBot()
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) stopBot()
        }
        return true // consome os dois pra não mexer no volume real do aparelho
    }

    private fun startBot() {
        if (isRunning) return
        isRunning = true
        toast("MK Bot: jogando")
    }

    private fun stopBot() {
        if (!isRunning) return
        isRunning = false
        toast("MK Bot: parado")
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show() }
    }

    // ---- Toques na tela ----

    fun tap(point: PointF, durationMs: Long = 50L) {
        mainHandler.post {
            val path = Path().apply { moveTo(point.x, point.y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
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

    /** Segura uma direção e, ainda segurando, toca no botão — o padrão "baixo + triângulo" da Ashrah. */
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
