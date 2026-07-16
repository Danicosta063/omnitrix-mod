package com.mkbot.ai

enum class BotAction { BLOCK, PUNCH, KICK, SPECIAL, APPROACH, RETREAT, IDLE }

data class CombatState(
    val myHealthPct: Float,
    val opponentHealthPct: Float,
    val opponentAttacking: Boolean,
    val inRange: Boolean
)

object CombatStateMachine {
    private var lastAction: BotAction = BotAction.IDLE

    fun decide(state: CombatState): BotAction {
        val action = when {
            state.opponentAttacking && state.inRange -> BotAction.BLOCK
            lastAction == BotAction.BLOCK && !state.opponentAttacking && state.inRange -> BotAction.PUNCH
            state.myHealthPct < 0.25f && state.inRange -> BotAction.RETREAT
            state.opponentHealthPct < 0.25f && state.inRange -> BotAction.SPECIAL
            !state.inRange -> BotAction.APPROACH
            else -> BotAction.KICK
        }
        lastAction = action
        return action
    }
}
