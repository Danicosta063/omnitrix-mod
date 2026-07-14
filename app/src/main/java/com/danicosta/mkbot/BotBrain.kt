package com.danicosta.mkbot

import android.os.Handler
import android.os.HandlerThread
import kotlin.random.Random

object BotBrain {

    private const val TICK_MS = 180L // ritmo de decisão — menor que isso não ajuda, o toque já leva 150-300ms

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastP1Health = 100
    private var lastP2Health = 100
    private var lastZone: String? = null
    private var lastMove: String? = null

    // pontuação "zona:golpe" -> favorece o que funciona. Fica só na memória RAM por enquanto;
    // gravar num arquivo persistente é o próximo passo.
    val moveScores: MutableMap<String, ActionScore> = mutableMapOf()

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
        val state = GameStateReader.read()
        if (!state.inFight) return

        val iGotHit = state.p1HealthPercent < lastP1Health - 1
        val iDealtDamage = state.p2HealthPercent < lastP2Health - 1

        if (iDealtDamage && lastZone != null && lastMove != null) {
            reward(lastZone!!, lastMove!!)
        }

        lastP1Health = state.p1HealthPercent
        lastP2Health = state.p2HealthPercent

        val (zone, move) = decideMove(state, iGotHit)
        lastZone = zone
        lastMove = move
        execute(service, move)
    }

    private fun decideMove(state: FightState, iGotHit: Boolean): Pair<String, String> {
        // acabou de apanhar -> bloqueia primeiro, decide depois
        if (iGotHit) return "DEFENSE" to "BLOCK"

        return when (state.distance) {
            Distance.FAR, Distance.UNKNOWN -> "FAR" to "APPROACH"
            Distance.MID -> "MID" to pickMove("MID", listOf("POKE_KICK" to 0.5, "APPROACH" to 0.5))
            Distance.CLOSE -> "CLOSE" to pickMove(
                "CLOSE",
                listOf("QUICK_PUNCH" to 0.35, "BLOCK" to 0.2, "UPPERCUT" to 0.25, "STRING_3HIT" to 0.2)
            )
        }
    }

    private fun pickMove(zone: String, options: List<Pair<String, Double>>): String {
        val useHistory = Random.nextDouble() < 0.8
        val best = if (useHistory) {
            options.maxByOrNull { (name, base) -> moveScores["$zone:$name"]?.rate() ?: base }?.first
        } else null
        val chosen = best ?: weightedRandom(options)
        moveScores.getOrPut("$zone:$chosen") { ActionScore() }.attempts++
        return chosen
    }

    private fun weightedRandom(options: List<Pair<String, Double>>): String {
        val total = options.sumOf { it.second }
        var r = Random.nextDouble() * total
        for ((name, weight) in options) {
            if (r < weight) return name
            r -= weight
        }
        return options.last().first
    }

    private fun reward(zone: String, move: String) {
        moveScores["$zone:$move"]?.let { it.successes++ }
    }

    private fun execute(service: BotAccessibilityService, move: String) {
        when (move) {
            "APPROACH" -> service.tap(Action.RIGHT)
            "BLOCK" -> service.tap(Action.BLOCK)
            "QUICK_PUNCH" -> service.tap(Action.FRONT_PUNCH)
            "POKE_KICK" -> service.tap(Action.FRONT_KICK)
            "UPPERCUT" -> service.directionThenButton(Coordinates.DOWN, Coordinates.TRIANGLE)
            "STRING_3HIT" -> service.sequence(
                listOf(Action.BACK_PUNCH, Action.BACK_PUNCH, Action.FRONT_PUNCH),
                stepDelayMs = 110L
            )
            "SPECIAL" -> service.tap(Action.SPECIAL)
        }
    }
}

class ActionScore {
    var attempts: Int = 0
    var successes: Int = 0
    fun rate(): Double = if (attempts == 0) 0.5 else successes.toDouble() / attempts
}
