package com.cocbot.core

import android.content.Context
import android.content.SharedPreferences

data class BotConfig(
    var targetGold: Int = 200000,
    var targetElixir: Int = 200000,
    var targetDarkElixir: Int = 0,
    var deploySide: DeploySide = DeploySide.BOTTOM,
    var deployPattern: DeployPattern = DeployPattern.LINE,
    var troopSlots: List<TroopSlot> = listOf(TroopSlot(0, 15), TroopSlot(1, 15)),
    var captureIntervalMs: Long = 500,
    var actionDelayMin: Long = 200,
    var actionDelayMax: Long = 600,
    var maxAttacks: Int = 50,
    var sessionTimeoutMin: Int = 120,
    var deadBaseOnly: Boolean = false,
    var loopCount: Int = 0, // 0 = infinite
    var tapOffsetRange: Int = 5
) {
    fun save(ctx: Context) {
        val p = prefs(ctx)
        p.edit().apply {
            putInt("targetGold", targetGold)
            putInt("targetElixir", targetElixir)
            putInt("targetDarkElixir", targetDarkElixir)
            putString("deploySide", deploySide.name)
            putString("deployPattern", deployPattern.name)
            putLong("captureInterval", captureIntervalMs)
            putLong("delayMin", actionDelayMin)
            putLong("delayMax", actionDelayMax)
            putInt("maxAttacks", maxAttacks)
            putInt("sessionTimeout", sessionTimeoutMin)
            putBoolean("deadBaseOnly", deadBaseOnly)
            putInt("loopCount", loopCount)
            putInt("tapOffset", tapOffsetRange)
            apply()
        }
    }

    companion object {
        fun load(ctx: Context): BotConfig {
            val p = prefs(ctx)
            return BotConfig(
                targetGold = p.getInt("targetGold", 200000),
                targetElixir = p.getInt("targetElixir", 200000),
                targetDarkElixir = p.getInt("targetDarkElixir", 0),
                deploySide = DeploySide.valueOf(p.getString("deploySide", "BOTTOM")!!),
                deployPattern = DeployPattern.valueOf(p.getString("deployPattern", "LINE")!!),
                captureIntervalMs = p.getLong("captureInterval", 500),
                actionDelayMin = p.getLong("delayMin", 200),
                actionDelayMax = p.getLong("delayMax", 600),
                maxAttacks = p.getInt("maxAttacks", 50),
                sessionTimeoutMin = p.getInt("sessionTimeout", 120),
                deadBaseOnly = p.getBoolean("deadBaseOnly", false),
                loopCount = p.getInt("loopCount", 0),
                tapOffsetRange = p.getInt("tapOffset", 5)
            )
        }

        private fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences("bot_config", Context.MODE_PRIVATE)
    }
}

data class TroopSlot(val slotIndex: Int, val dropCount: Int)

enum class DeploySide { TOP, BOTTOM, LEFT, RIGHT }
enum class DeployPattern { LINE, POINT, MULTI_LINE }