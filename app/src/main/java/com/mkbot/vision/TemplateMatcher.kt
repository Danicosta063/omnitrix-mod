package com.mkbot.vision

import android.graphics.Bitmap
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

object TemplateMatcher {

    data class MatchResult(val confidence: Double, val location: Point)

    fun find(screen: Bitmap, template: Bitmap, threshold: Double = 0.82): MatchResult? {
        val screenMat = Mat()
        val templateMat = Mat()
        Utils.bitmapToMat(screen, screenMat)
        Utils.bitmapToMat(template, templateMat)
        Imgproc.cvtColor(screenMat, screenMat, Imgproc.COLOR_BGRA2BGR)
        Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGRA2BGR)

        val cols = screenMat.cols() - templateMat.cols() + 1
        val rows = screenMat.rows() - templateMat.rows() + 1
        if (cols <= 0 || rows <= 0) {
            screenMat.release(); templateMat.release()
            return null
        }

        val result = Mat(rows, cols, CvType.CV_32FC1)
        Imgproc.matchTemplate(screenMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)
        val mmr = Core.minMaxLoc(result)

        screenMat.release(); templateMat.release(); result.release()
        return if (mmr.maxVal >= threshold) MatchResult(mmr.maxVal, mmr.maxLoc) else null
    }

    fun readHealthPercent(screen: Bitmap, region: Rect, fullColor: Int, tolerance: Int = 30): Float {
        var filled = 0
        val y = (region.top + region.bottom) / 2
        for (x in region.left until region.right) {
            if (colorsClose(screen.getPixel(x, y), fullColor, tolerance)) filled++
        }
        return filled.toFloat() / region.width()
    }

    private fun colorsClose(a: Int, b: Int, tol: Int): Boolean {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return kotlin.math.abs(dr) <= tol && kotlin.math.abs(dg) <= tol && kotlin.math.abs(db) <= tol
    }
}
