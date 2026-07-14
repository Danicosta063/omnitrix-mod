package com.danicosta.mkbot

import android.graphics.PointF

object Coordinates {
    // D-pad
    val UP = PointF(333f, 640f)
    val DOWN = PointF(360f, 950f)
    val LEFT = PointF(200f, 790f)
    val RIGHT = PointF(516f, 800f)

    // Zonas de pulo e agachar
    val JUMP_LEFT = PointF(196f, 690f)
    val JUMP_RIGHT = PointF(480f, 686f)
    val CROUCH = PointF(220f, 920f)

    // Botões de ataque
    val SQUARE = PointF(1937f, 785f)   // Front Punch
    val TRIANGLE = PointF(2100f, 630f) // Back Punch
    val X = PointF(2060f, 930f)        // Front Kick
    val CIRCLE = PointF(2220f, 800f)   // Back Kick

    // Gatilhos
    val R1_SPECIAL = PointF(2060f, 260f)
    val R2_BLOCK = PointF(2060f, 235f)

    // Pegam arma do cenário — não usados no combate
    val L1_WEAPON = PointF(350f, 266f)
    val L2_WEAPON = PointF(331f, 235f)
}

enum class Action {
    FRONT_PUNCH, BACK_PUNCH, FRONT_KICK, BACK_KICK,
    BLOCK, SPECIAL,
    UP, DOWN, LEFT, RIGHT,
    JUMP_LEFT, JUMP_RIGHT, CROUCH
}

fun Action.toPoint(): PointF = when (this) {
    Action.FRONT_PUNCH -> Coordinates.SQUARE
    Action.BACK_PUNCH -> Coordinates.TRIANGLE
    Action.FRONT_KICK -> Coordinates.X
    Action.BACK_KICK -> Coordinates.CIRCLE
    Action.BLOCK -> Coordinates.R2_BLOCK
    Action.SPECIAL -> Coordinates.R1_SPECIAL
    Action.UP -> Coordinates.UP
    Action.DOWN -> Coordinates.DOWN
    Action.LEFT -> Coordinates.LEFT
    Action.RIGHT -> Coordinates.RIGHT
    Action.JUMP_LEFT -> Coordinates.JUMP_LEFT
    Action.JUMP_RIGHT -> Coordinates.JUMP_RIGHT
    Action.CROUCH -> Coordinates.CROUCH
}
