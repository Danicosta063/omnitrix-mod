package com.mkbot.ai

enum class Facing { LEFT, RIGHT }
enum class RelDir { UP, DOWN, FORWARD, BACKWARD }
enum class ScreenDir { UP, DOWN, LEFT, RIGHT }
enum class Button { SQUARE, TRIANGLE, CROSS, CIRCLE, L1, L2, R1, R2 }

data class InputStep(val dir: RelDir? = null, val button: Button? = null)
data class ScreenStep(val dir: ScreenDir? = null, val button: Button? = null)

data class SpecialMove(
    val name: String,
    val steps: List<InputStep>,
    val description: String
)

object AshrahMoves {

    val heavenlyLight = SpecialMove(
        name = "Heavenly Light",
        steps = listOf(InputStep(dir = RelDir.DOWN), InputStep(dir = RelDir.BACKWARD, button = Button.SQUARE)),
        description = "Bola de energia que cai de cima — bom anti-aéreo"
    )

    val lightningBlast = SpecialMove(
        name = "Lightning Blast",
        steps = listOf(InputStep(dir = RelDir.DOWN), InputStep(dir = RelDir.FORWARD, button = Button.SQUARE)),
        description = "Energia de curto alcance — empurra o oponente"
    )

    val spinCycle = SpecialMove(
        name = "Spin Cycle",
        steps = listOf(InputStep(dir = RelDir.DOWN), InputStep(dir = RelDir.UP, button = Button.CROSS)),
        description = "Giro que lança pro alto"
    )

    val naturesTorpedo = SpecialMove(
        name = "Nature's Torpedo",
        steps = listOf(InputStep(dir = RelDir.FORWARD), InputStep(dir = RelDir.FORWARD, button = Button.CIRCLE)),
        description = "Avanço voador — fecha distância e pune"
    )

    val seeTheLight = SpecialMove(
        name = "See The Light",
        steps = listOf(InputStep(button = Button.TRIANGLE)), // TODO: confirmar botão do pegão
        description = "Pegão (throw)"
    )

    val changeStyle = SpecialMove(
        name = "Change Style",
        steps = listOf(InputStep(button = Button.L1)),
        description = "Troca entre Chou Jaio e Kriss"
    )

    val all: List<SpecialMove> = listOf(heavenlyLight, lightningBlast, spinCycle, naturesTorpedo, seeTheLight, changeStyle)

    fun resolve(move: SpecialMove, facing: Facing): List<ScreenStep> {
        return move.steps.map { step ->
            val screenDir = when (step.dir) {
                RelDir.UP -> ScreenDir.UP
                RelDir.DOWN -> ScreenDir.DOWN
                RelDir.FORWARD -> if (facing == Facing.RIGHT) ScreenDir.RIGHT else ScreenDir.LEFT
                RelDir.BACKWARD -> if (facing == Facing.RIGHT) ScreenDir.LEFT else ScreenDir.RIGHT
                null -> null
            }
            ScreenStep(dir = screenDir, button = step.button)
        }
    }
}
