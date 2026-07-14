package com.danicosta.mkbot

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.ConcurrentHashMap

object BotBrain {

    private const val TICK_MS = 450L

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var stepIndex = 0

    // Sem visão por enquanto — só gira nas suas coordenadas, na ordem abaixo, sem parar.
    private val rotation = listOf(
        Action.RIGHT to "AND",
        Action.FRONT_PUNCH to "SOC",
        Action.FRONT_KICK to "CHT",
        Action.BACK_PUNCH to "SC2",
        Action.BLOCK to "DEF"
    )

    val moveScores: MutableMap<String, ActionScore> = ConcurrentHashMap() // mantido pro arquivo de memória não quebrar

    fun start() {
        if (handlerThread != null) return
        handlerThread = HandlerThread("MKBotBrain").also { it.start() }
        handler = Handler(handlerThread!!.looper)
        scheduleTick()
    }

    fun stop() {
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    private fun scheduleTick() {
        handler?.postDelayed({
            tick()
            scheduleTick()
        }, TICK_MS)
    }

    private fun tick() {
        val service = BotAccessibilityService.instance ?: return
        MemoryManager.addElapsedMillis(MainActivity.CURRENT_CHARACTER, TICK_MS)

        val (action, label) = rotation[stepIndex % rotation.size]
        stepIndex++
        service.lastAction = label
        service.tap(action)
    }
}

class ActionScore {
    var attempts: Int = 0
    var successes: Int = 0
    fun rate(): Double = if (attempts == 0) 0.5 else successes.toDouble() / attempts
}
