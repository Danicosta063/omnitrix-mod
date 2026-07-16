package com.mkbot.ai

/** Lado da tela que a Ashrah está olhando no momento. */
enum class Facing { LEFT, RIGHT }

/** Direção relativa usada nos comandos dos golpes — independe do lado. */
enum class RelDir { UP, DOWN, FORWARD, BACKWARD }

/** Direção real na tela, já resolvida — é o que vira toque de D-pad. */
enum class ScreenDir { UP, DOWN, LEFT, RIGHT }

/** Botões do PS2 usados nos comandos. */
enum class Button { SQUARE, TRIANGLE, CROSS, CIRCLE, L1, R1 }

/** Um passo do input: direção, botão, ou os dois juntos. */
data class InputStep(val dir: RelDir? = null, val button: Button? = null)

/** Um passo já resolvido pra tela — pronto pra virar toque real. */
data class ScreenStep(val dir: ScreenDir? = null, val button: Button? = null)

data class SpecialMove(
    val name: String,
    val steps: List<InputStep>,
    val description: String
)

/**
 * Golpes especiais da Ashrah (Mortal Kombat: Armageddon).
 * Comando oficial: Heavenly Light, Lightning Blast, Spin Cycle, Nature's Torpedo.
 */
object AshrahMoves {

    val heavenlyLight = SpecialMove(
        name = "Heavenly Light",
        steps = listOf(
            InputStep(dir = RelDir.DOWN),
            InputStep(dir = RelDir.BACKWARD, button = Button.SQUARE)
        ),
        description = "Bola de energia que cai de cima — bom anti-aéreo"
    )

    val lightningBlast = SpecialMove(
        name = "Lightning Blast",
        steps = listOf(
            InputStep(dir = RelDir.DOWN),
            InputStep(dir = RelDir.FORWARD, button = Button.SQUARE)
        ),
        description = "Energia de curto alcance — empurra o oponente"
    )

    val spinCycle = SpecialMove(
        name = "Spin Cycle",
        steps = listOf(
            InputStep(dir = RelDir.DOWN),
            InputStep(dir = RelDir.UP, button = Button.CROSS)
        ),
        description = "Giro que lança pro alto — segurar o botão aumenta a duração"
    )

    val naturesTorpedo = SpecialMove(
        name = "Nature's Torpedo",
        steps = listOf(
            InputStep(dir = RelDir.FORWARD),
            InputStep(dir = RelDir.FORWARD, button = Button.CIRCLE)
        ),
        description = "Avanço voador — fecha distância e pune"
    )

    val seeTheLight = SpecialMove(
        name = "See The Light",
        steps = listOf(InputStep(button = Button.TRIANGLE)), // TODO: confirmar - não achei o botão exato documentado pro pegão dela
        description = "Pegão (throw)"
    )

    val changeStyle = SpecialMove(
        name = "Change Style",
        steps = listOf(InputStep(button = Button.L1)),
        description = "Troca entre Chou Jaio (mão livre) e Kriss (espada)"
    )

    val all: List<SpecialMove> = listOf(
        heavenlyLight, lightningBlast, spinCycle, naturesTorpedo, seeTheLight, changeStyle
    )

    /**
     * Converte um golpe (definido em direção relativa) pra sequência real de
     * toques na tela, considerando o lado que a Ashrah está olhando.
     * Sem isso, FORWARD/BACKWARD saem errados quando ela troca de lado —
     * era esse o bug do R1 sem direção.
     */
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
