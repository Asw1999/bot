package com.cocbot.navigation

import com.cocbot.capture.ScreenCaptureService
import com.cocbot.detection.LootInfo
import com.cocbot.detection.LootReader
import com.cocbot.detection.TemplateDetector
import com.cocbot.input.BotAccessibilityService
import com.cocbot.util.BotLog
import com.cocbot.util.CoordScaler
import com.cocbot.util.RandomDelay

/**
 * Navigates CoC UI: attack screen, search, skip, return home.
 * Uses template matching to find button positions before tapping.
 *
 * ponytail: button positions fallback to hardcoded coords if template not found.
 */
class Navigator(
    private val detector: TemplateDetector,
    private val lootReader: LootReader,
    private val capture: ScreenCaptureService,
    private val input: BotAccessibilityService,
    private val scaler: CoordScaler
) {
    // Fallback coords in base resolution (2400x1080 landscape)
    // These are approximate — template matching preferred
    private object Fallback {
        val attackBtn = 80f to 950f     // Main village attack button
        val findMatch = 1200f to 800f   // "Find a Match" button
        val nextBtn = 2200f to 600f     // "Next" button on opponent base
        val endBattle = 120f to 80f     // "End Battle" button (surrender)
        val returnHome = 1200f to 750f  // "Return Home" after battle
    }

    suspend fun goToAttack(): Boolean {
        BotLog.i("Navigate: going to attack")
        return tapButton("btn_attack.png", Fallback.attackBtn).also {
            if (it) RandomDelay.delayRange(800, 1200)
        }
    }

    suspend fun findMatch(): Boolean {
        BotLog.i("Navigate: find match")
        return tapButton("btn_find_match.png", Fallback.findMatch).also {
            if (it) RandomDelay.delayRange(2000, 3000) // search loading
        }
    }

    suspend fun skipOpponent(): Boolean {
        BotLog.i("Navigate: skip opponent")
        return tapButton("btn_next.png", Fallback.nextBtn).also {
            if (it) RandomDelay.delayRange(1500, 2500)
        }
    }

    suspend fun returnHome(): Boolean {
        BotLog.i("Navigate: return home")
        return tapButton("btn_return_home.png", Fallback.returnHome).also {
            if (it) RandomDelay.delayRange(2000, 3000)
        }
    }

    suspend fun readLoot(): LootInfo? {
        val frame = capture.captureFrame() ?: return null
        return try { lootReader.readLoot(frame) } finally { frame.recycle() }
    }

    suspend fun dismissPopup(templateName: String): Boolean {
        return tapButton(templateName, null)
    }

    private suspend fun tapButton(templateName: String, fallback: Pair<Float, Float>?): Boolean {
        val frame = capture.captureFrame()
        if (frame != null) {
            val match = detector.findButton(frame, templateName)
            frame.recycle()
            if (match != null) {
                val x = match.centerX.toFloat()
                val y = match.centerY.toFloat()
                BotLog.i("Tap $templateName at ($x, $y)")
                return input.tap(x, y, 3)
            }
        }
        // Fallback to hardcoded coords
        if (fallback != null) {
            val (fx, fy) = scaler.scale(fallback.first, fallback.second)
            BotLog.w("Template $templateName not found, fallback ($fx, $fy)")
            return input.tap(fx, fy, 5)
        }
        BotLog.e("Cannot find $templateName, no fallback")
        return false
    }
}