package com.cocbot.strategy

import android.graphics.Bitmap
import com.cocbot.core.BotConfig
import com.cocbot.input.BotAccessibilityService

/** AI-swappable attack strategy interface */
interface IAttackStrategy {
    /**
     * Deploy troops on the battlefield.
     * @param frame current screen capture
     * @param config bot configuration
     * @param input accessibility service for taps
     */
    suspend fun deploy(frame: Bitmap, config: BotConfig, input: BotAccessibilityService)
}