package com.mkbot.input

import com.mkbot.ai.Button
import com.mkbot.ai.ScreenDir

/** Lido direto do print do controle no NetherSX2, resolução 2400x1080. */
object Coordinates {

    private val dpadUp = 360f to 918f
    private val dpadDown = 360f to 616f
    private val dpadLeft = 504f to 767f
    private val dpadRight = 216f to 767f

    private val square = 1992f to 767f
    private val triangle = 2136f to 616f
    private val cross = 2136f to 918f
    private val circle = 2280f to 767f
    private val l1 = 360f to 367f
    private val l2 = 360f to 216f
    private val r1 = 2040f to 367f
    private val r2 = 2040f to 216f

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
        Button.L2 -> l2
        Button.R1 -> r1
        Button.R2 -> r2
    }
}
