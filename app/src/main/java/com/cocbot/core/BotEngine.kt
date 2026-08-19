package com.cocbot.core

import com.cocbot.capture.ScreenCaptureService
import com.cocbot.detection.IGameStateDetector
import com.cocbot.detection.LootInfo
import com.cocbot.input.BotAccessibilityService
import com.cocbot.navigation.Navigator
import com.cocbot.overlay.OverlayManager
import com.cocbot.strategy.IAttackStrategy
import com.cocbot.util.BotLog
import com.cocbot.util.RandomDelay
import kotlinx.coroutines.*

/**
 * Core bot state machine. Runs the auto-farm loop.
 */
class BotEngine(
    private val config: BotConfig,
    private val capture: ScreenCaptureService,
    private val input: BotAccessibilityService,
    private val detector: IGameStateDetector,
    private val navigator: Navigator,
    private val strategy: IAttackStrategy,
    private val overlay: OverlayManager
) {
    private var job: Job? = null
    private var state = BotState.IDLE
    private var attackCount = 0
    private var startTime = 0L

    val isRunning: Boolean get() = job?.isActive == true

    fun start() {
        if (isRunning) return
        attackCount = 0
        startTime = System.currentTimeMillis()
        state = BotState.NAVIGATE_TO_ATTACK
        BotLog.i("Bot started")
        overlay.setRunning(true)

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                loop()
            } catch (e: CancellationException) {
                BotLog.i("Bot cancelled")
            } catch (e: Exception) {
                BotLog.e("Bot error: ${e.message}")
            } finally {
                state = BotState.IDLE
                overlay.setRunning(false)
                overlay.updateStatus("Idle")
                BotLog.i("Bot stopped (attacks: $attackCount)")
            }
        }
    }

    fun stop() {
        BotLog.i("Stopping bot...")
        job?.cancel()
        job = null
    }

    private suspend fun loop() {
        while (true) {
            // Check limits
            if (config.maxAttacks > 0 && attackCount >= config.maxAttacks) {
                BotLog.i("Max attacks reached ($attackCount)")
                break
            }
            val elapsed = (System.currentTimeMillis() - startTime) / 60000
            if (config.sessionTimeoutMin > 0 && elapsed >= config.sessionTimeoutMin) {
                BotLog.i("Session timeout ($elapsed min)")
                break
            }
            if (config.loopCount > 0 && attackCount >= config.loopCount) {
                BotLog.i("Loop count reached")
                break
            }

            overlay.updateStatus("$state | Attacks: $attackCount")

            when (state) {
                BotState.NAVIGATE_TO_ATTACK -> {
                    navigator.goToAttack()
                    RandomDelay.delayRange(500, 1000)
                    navigator.findMatch()
                    state = BotState.SEARCHING
                }

                BotState.SEARCHING -> {
                    // Wait for opponent base to load
                    RandomDelay.delayRange(2000, 3000)
                    state = BotState.CHECK_LOOT
                }

                BotState.CHECK_LOOT -> {
                    val loot = navigator.readLoot()
                    if (loot != null && meetsThreshold(loot)) {
                        BotLog.i("Loot OK: g=${loot.gold} e=${loot.elixir} d=${loot.darkElixir}")
                        state = BotState.DEPLOY_TROOPS
                    } else {
                        BotLog.i("Loot too low, skipping")
                        navigator.skipOpponent()
                        state = BotState.SEARCHING
                    }
                }

                BotState.DEPLOY_TROOPS -> {
                    val frame = capture.captureFrame()
                    if (frame != null) {
                        strategy.deploy(frame, config, input)
                        frame.recycle()
                    }
                    state = BotState.BATTLING
                }

                BotState.BATTLING -> {
                    // Wait for battle to end (poll game state)
                    var waitMs = 0L
                    val maxWait = 180_000L // 3 min max battle
                    while (waitMs < maxWait) {
                        RandomDelay.delayRange(3000, 5000)
                        waitMs += 4000
                        val frame = capture.captureFrame() ?: continue
                        val gs = detector.detect(frame)
                        frame.recycle()
                        if (gs == GameState.BATTLE_END) break
                    }
                    state = BotState.BATTLE_END
                }

                BotState.BATTLE_END -> {
                    attackCount++
                    BotLog.i("Battle #$attackCount complete")
                    RandomDelay.delayRange(2000, 4000)
                    navigator.returnHome()
                    state = BotState.RETURN_HOME
                }

                BotState.RETURN_HOME -> {
                    RandomDelay.delayRange(3000, 5000)
                    state = BotState.NAVIGATE_TO_ATTACK
                }

                BotState.IDLE, BotState.ERROR -> break
            }

            // Breathing room between states
            RandomDelay.delayRange(config.actionDelayMin, config.actionDelayMax)
        }
    }

    private fun meetsThreshold(loot: LootInfo): Boolean {
        return loot.gold >= config.targetGold || loot.elixir >= config.targetElixir ||
            (config.targetDarkElixir > 0 && loot.darkElixir >= config.targetDarkElixir)
    }
}