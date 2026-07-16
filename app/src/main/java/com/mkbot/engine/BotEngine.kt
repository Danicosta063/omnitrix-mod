package com.mkbot.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import com.mkbot.ai.AshrahMoves
import com.mkbot.ai.BotAction
import com.mkbot.ai.Button
import com.mkbot.ai.CombatState
import com.mkbot.ai.CombatStateMachine
import com.mkbot.ai.Facing
import com.mkbot.ai.ScreenDir
import com.mkbot.ai.SpecialMove
import com.mkbot.capture.ScreenCaptureService
import com.mkbot.input.BotAccessibilityService
import com.mkbot.input.Coordinates
import com.mkbot.vision.TemplateMatcher

object BotEngine {

    private const val TICK_MS = 100L
    private var facing = Facing.RIGHT   // TODO: detectar de verdade
    private var running = false

    private val thread = HandlerThread("BotEngineThread").apply { start() }
    private val handler = Handler(thread.looper)

    // Lido do pixel do print — Scorpion vs Mileena, ambos a 100%.
    // Esquerda = seu personagem, direita = oponente.
    private val myHealthRegion = Rect(700, 100, 1140, 125)
    private val opponentHealthRegion = Rect(1340, 100, 1840, 125)
    private const val HEALTH_FULL_COLOR = 0xFF15F822.toInt()

    fun start() {
        if (running) return
        running = true
        handler.post(::tick)
    }

    fun stop() { running = false }

    private fun tick() {
        if (!running) return
        val frame = ScreenCaptureService.latestFrame
        if (frame != null && BotAccessibilityService.isReady) {
            execute(CombatStateMachine.decide(detectState(frame)))
        }
        handler.postDelayed(::tick, TICK_MS)
    }

    private fun detectState(frame: Bitmap): CombatState {
        val myHp = TemplateMatcher.readHealthPercent(frame, myHealthRegion, HEALTH_FULL_COLOR)
        val oppHp = TemplateMatcher.readHealthPercent(frame, opponentHealthRegion, HEALTH_FULL_COLOR)
        // TODO: template match real quando tiver print do golpe do oponente
        return CombatState(myHp, oppHp, opponentAttacking = false, inRange = true)
    }

    private fun execute(action: BotAction) {
        when (action) {
            BotAction.BLOCK -> tapButton(Button.R2)
            BotAction.PUNCH -> tapButton(Button.SQUARE)
            BotAction.KICK -> tapButton(Button.CROSS)
            BotAction.SPECIAL -> executeMove(AshrahMoves.naturesTorpedo)
            BotAction.APPROACH -> tapDir(if (facing == Facing.RIGHT) ScreenDir.RIGHT else ScreenDir.LEFT)
            BotAction.RETREAT -> tapDir(if (facing == Facing.RIGHT) ScreenDir.LEFT else ScreenDir.RIGHT)
            BotAction.IDLE -> {}
        }
    }

    private fun executeMove(move: SpecialMove) {
        var delay = 0L
        for (step in AshrahMoves.resolve(move, facing)) {
            handler.postDelayed({
                step.dir?.let { tapDir(it) }
                step.button?.let { tapButton(it) }
            }, delay)
            delay += 90L
        }
    }

    private fun tapDir(dir: ScreenDir) = BotAccessibilityService.tap(Coordinates.forDir(dir).first, Coordinates.forDir(dir).second)
    private fun tapButton(button: Button) = BotAccessibilityService.tap(Coordinates.forButton(button).first, Coordinates.forButton(button).second)
}
