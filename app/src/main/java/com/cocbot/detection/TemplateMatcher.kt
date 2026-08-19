package com.cocbot.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * OpenCV template matching wrapper.
 * Templates loaded from assets/templates/ on first use, cached.
 */
class TemplateMatcher(private val ctx: Context) {

    data class MatchResult(
        val templateName: String,
        val x: Int, val y: Int,
        val w: Int, val h: Int,
        val confidence: Double
    ) {
        val centerX: Int get() = x + w / 2
        val centerY: Int get() = y + h / 2
    }

    private val templateCache = mutableMapOf<String, Mat>()

    private fun loadTemplate(name: String): Mat? {
        templateCache[name]?.let { return it }
        return try {
            val stream = ctx.assets.open("templates/$name")
            val bmp = BitmapFactory.decodeStream(stream)
            stream.close()
            val mat = Mat()
            Utils.bitmapToMat(bmp, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
            bmp.recycle()
            templateCache[name] = mat
            mat
        } catch (e: Exception) {
            null
        }
    }

    fun findMatch(frame: Bitmap, templateName: String, threshold: Double = 0.85): MatchResult? {
        if (frame.isRecycled) return null
        val tmpl = loadTemplate(templateName) ?: return null
        val src = Mat()
        try {
            Utils.bitmapToMat(frame, src)
            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR)

            val result = Mat()
            Imgproc.matchTemplate(src, tmpl, result, Imgproc.TM_CCOEFF_NORMED)

            val mmr = Core.minMaxLoc(result)
            src.release()
            result.release()

            return if (mmr.maxVal >= threshold) {
                MatchResult(templateName, mmr.maxLoc.x.toInt(), mmr.maxLoc.y.toInt(),
                    tmpl.cols(), tmpl.rows(), mmr.maxVal)
            } else null
        } catch (e: Exception) {
            src.release()
            return null
        }
    }

    fun findAllMatches(frame: Bitmap, templateName: String, threshold: Double = 0.85): List<MatchResult> {
        val tmpl = loadTemplate(templateName) ?: return emptyList()
        val src = Mat()
        Utils.bitmapToMat(frame, src)
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR)

        val result = Mat()
        Imgproc.matchTemplate(src, tmpl, result, Imgproc.TM_CCOEFF_NORMED)

        val matches = mutableListOf<MatchResult>()
        for (y in 0 until result.rows()) {
            for (x in 0 until result.cols()) {
                val v = result.get(y, x)[0]
                if (v >= threshold) {
                    matches.add(MatchResult(templateName, x, y, tmpl.cols(), tmpl.rows(), v))
                }
            }
        }
        src.release()
        result.release()
        return nonMaxSuppression(matches)
    }

    private fun nonMaxSuppression(matches: List<MatchResult>): List<MatchResult> {
        val sorted = matches.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<MatchResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeFirst()
            kept.add(best)
            sorted.removeAll { iou(best, it) > 0.5 }
        }
        return kept
    }

    private fun iou(a: MatchResult, b: MatchResult): Double {
        val x1 = maxOf(a.x, b.x); val y1 = maxOf(a.y, b.y)
        val x2 = minOf(a.x + a.w, b.x + b.w); val y2 = minOf(a.y + a.h, b.y + b.h)
        if (x2 <= x1 || y2 <= y1) return 0.0
        val inter = (x2 - x1).toDouble() * (y2 - y1)
        return inter / (a.w * a.h + b.w * b.h - inter)
    }

    fun clearCache() {
        templateCache.values.forEach { it.release() }
        templateCache.clear()
    }
}