package com.mkbot.input

import com.mkbot.ai.Button
import com.mkbot.ai.ScreenDir

/**
 * Posições reais de toque na tela — específicas do seu aparelho e do
 * layout do controle virtual do emulador. PRECISA CALIBRAR: tira um
 * print do emulador aberto, mede onde cada botão/direção cai em pixel,
 * e substitui os valores abaixo.
 */
object Coordinates {

    private val dpadUp = 100f to 800f       // TODO: calibrar
    private val dpadDown = 100f to 950f     // TODO: calibrar
    private val dpadLeft = 30f to 875f      // TODO: calibrar
    private val dpadRight = 170f to 875f    // TODO: calibrar

    private val square = 900f to 900f       // TODO: calibrar
    private val triangle = 950f to 820f     // TODO: calibrar
    private val cross = 1000f to 900f       // TODO: calibrar
    private val circle = 950f to 980f       // TODO: calibrar
    private val l1 = 60f to 100f            // TODO: calibrar
    private val r1 = 1020f to 100f          // TODO: calibrar

    fun forDir(dir: ScreenDir): Pair<Float, Float> = when (dir) {
        ScreenDir.UP -> dpadUp
        ScreenDir.DOWN -> dpadDown
        ScreenDir.LEFT -> dpadLeft
        ScreenDir.RIGHT -> dpadRight
    }

    fun forButton(button: Button): Pair<Float, Float> = when (button) {
        Button.SQUARE -> square
        Button.TRIANGLE -> triangle
        Button.CROSS -> cross
        Button.CIRCLE -> circle
        Button.L1 -> l1
        Button.R1 -> r1
    }
}
