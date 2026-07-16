package com.mkbot.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class BotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: BotAccessibilityService? = null

        val isReady: Boolean get() = instance != null

        /** Toque simples num ponto da tela — usado pra "apertar" um botão/D-pad virtual. */
        fun tap(x: Float, y: Float, durationMs: Long = 50) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            instance?.dispatchGesture(gesture, null, null)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Não precisa ler eventos — o bot enxerga a tela via TemplateMatcher/ScreenCaptureService.
        // Esse serviço só existe pra poder tocar (dispatchGesture).
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
