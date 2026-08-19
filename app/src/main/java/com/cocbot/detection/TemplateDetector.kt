package com.cocbot.detection

import android.graphics.Bitmap
import com.cocbot.core.GameState
import com.cocbot.util.BotLog

/**
 * Template-matching based game state detector.
 * ponytail: swap to ML classifier when accuracy needed on varied devices
 */
class TemplateDetector(private val matcher: TemplateMatcher) : IGameStateDetector {

    private val stateTemplates = mapOf(
        "btn_attack.png" to GameState.MAIN_VILLAGE,
        "btn_find_match.png" to GameState.SEARCH_OPPONENT,
        "btn_next.png" to GameState.OPPONENT_BASE,
        "btn_end_battle.png" to GameState.IN_BATTLE,
        "star_result.png" to GameState.BATTLE_END
    )

    override fun detect(frame: Bitmap): GameState {
        var bestState = GameState.UNKNOWN
        var bestConf = 0.0

        for ((template, state) in stateTemplates) {
            val result = matcher.findMatch(frame, template, threshold = 0.80)
            if (result != null && result.confidence > bestConf) {
                bestConf = result.confidence
                bestState = state
            }
        }

        if (bestState != GameState.UNKNOWN) {
            BotLog.i("State: $bestState (conf: ${"%.2f".format(bestConf)})")
        }
        return bestState
    }

    fun findButton(frame: Bitmap, templateName: String): TemplateMatcher.MatchResult? {
        return matcher.findMatch(frame, templateName, threshold = 0.80)
    }
}