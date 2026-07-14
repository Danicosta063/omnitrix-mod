package com.danicosta.mkbot

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

data class FightState(
    val inFight: Boolean,
    val p1HealthPercent: Int,
    val p2HealthPercent: Int,
    val distance: Distance
)

enum class Distance { CLOSE, MID, FAR, UNKNOWN }

object GameStateReader {

    // Calibrado a partir do print que você mandou (tela 2400x1080, barras a 100%).
    // Se no seu aparelho a barra não bater, esses 5 números são os primeiros a ajustar.
    private const val BAR_Y = 110
    private const val P1_ANCHOR_X = 662   // ponta esquerda fixa da barra do jogador 1
    private const val P1_FULL_X = 1164    // ponta direita da barra com 100% de vida
    private const val P2_ANCHOR_X = 1883  // ponta direita fixa da barra do jogador 2
    private const val P2_FULL_X = 1321    // ponta esquerda da barra com 100% de vida

    private var lastP1Health = 100
    private var lastP2Health = 100

    fun read(): FightState {
        val frame = ScreenCaptureService.latestFrame
            ?: return FightState(false, lastP1Health, lastP2Health, Distance.UNKNOWN)

        val p1Edge = findBarEdge(frame, P1_ANCHOR_X, P1_FULL_X)
        val p2Edge = findBarEdge(frame, P2_ANCHOR_X, P2_FULL_X)

        if (p1Edge == null || p2Edge == null) {
            // Sem cor de barra nos dois lados = provavelmente menu, vitória ou transição, não luta ativa
            return FightState(false, lastP1Health, lastP2Health, Distance.UNKNOWN)
        }

        val p1Percent = (((p1Edge - P1_ANCHOR_X).toFloat() / (P1_FULL_X - P1_ANCHOR_X)) * 100)
            .coerceIn(0f, 100f).toInt()
        val p2Percent = (((P2_ANCHOR_X - p2Edge).toFloat() / (P2_ANCHOR_X - P2_FULL_X)) * 100)
            .coerceIn(0f, 100f).toInt()

        lastP1Health = p1Percent
        lastP2Health = p2Percent

        return FightState(true, p1Percent, p2Percent, estimateDistance(frame))
    }

    /** Anda do anchor até o full contando presença de cor de barra; devolve onde ela termina. */
    private fun findBarEdge(bitmap: Bitmap, anchorX: Int, fullX: Int): Int? {
        val step = if (fullX > anchorX) 1 else -1
        var x = anchorX
        var lastGoodX: Int? = null
        var misses = 0
        while (if (step > 0) x <= fullX else x >= fullX) {
            if (isBarColorAtColumn(bitmap, x)) {
                lastGoodX = x
                misses = 0
            } else if (lastGoodX != null) {
                misses++
                if (misses > 3) break // tolera 3px de ruído antes de assumir que a barra acabou
            }
            x += step
        }
        return lastGoodX
    }

    private fun isBarColorAtColumn(bitmap: Bitmap, x: Int): Boolean {
        if (x < 0 || x >= bitmap.width) return false
        var hits = 0
        for (dy in -1..1) {
            val y = BAR_Y + dy
            if (y < 0 || y >= bitmap.height) continue
            if (isBarColor(bitmap.getPixel(x, y))) hits++
        }
        return hits > 1
    }

    private fun isBarColor(pixel: Int): Boolean {
        val hsv = FloatArray(3)
        Color.colorToHSV(pixel, hsv)
        // verde -> amarelo -> vermelho conforme a vida cai, sempre saturado e claro
        return hsv[0] <= 140f && hsv[1] > 0.45f && hsv[2] > 0.35f
    }

    /**
     * Aproximação simples de distância: conta variações bruscas de brilho numa faixa horizontal
     * no meio da tela. É o ponto mais fraco dessa primeira versão — cenário e câmera mudam o
     * resultado. O cérebro da Ashrah (próximo arquivo) vai se apoiar mais no dano recebido/causado
     * do que nisso aqui, e a gente ajusta ou troca por outra técnica se não funcionar bem no teste real.
     */
    private fun estimateDistance(bitmap: Bitmap): Distance {
        val sampleY = (bitmap.height * 0.55f).toInt()
        val leftX = (bitmap.width * 0.30f).toInt()
        val rightX = (bitmap.width * 0.70f).toInt()

        var edges = 0
        var lastValue = -1f
        var x = leftX
        val hsv = FloatArray(3)
        while (x < rightX) {
            Color.colorToHSV(bitmap.getPixel(x, sampleY), hsv)
            if (lastValue >= 0 && abs(hsv[2] - lastValue) > 0.25f) edges++
            lastValue = hsv[2]
            x += 4
        }

        return when {
            edges >= 10 -> Distance.CLOSE
            edges >= 5 -> Distance.MID
            else -> Distance.FAR
        }
    }
}
