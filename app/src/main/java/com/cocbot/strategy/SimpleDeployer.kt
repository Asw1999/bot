package com.cocbot.strategy

import android.graphics.Bitmap
import com.cocbot.core.*
import com.cocbot.input.BotAccessibilityService
import com.cocbot.util.BotLog
import com.cocbot.util.CoordScaler
import com.cocbot.util.RandomDelay

/**
 * Simple troop deployer: select troop slot, drop along line/point.
 *
 * Troop bar is at bottom of battle screen.
 * Slot positions approximated for base res 2400x1080 landscape.
 *
 * ponytail: slot positions hardcoded. Use template matching for robust slot detection.
 */
class SimpleDeployer(private val scaler: CoordScaler) : IAttackStrategy {

    // Approximate troop slot X positions in base resolution (2400x1080 landscape)
    // Bottom bar, Y ~1020. Slots roughly at these X positions:
    private val slotBaseX = listOf(80f, 170f, 260f, 350f, 440f, 530f, 620f, 710f, 800f, 890f)
    private val slotBaseY = 1020f

    // Deploy edge coordinates per side (base resolution)
    private val deployEdges = mapOf(
        DeploySide.TOP to DeployEdge(y = 80f, xStart = 300f, xEnd = 2100f),
        DeploySide.BOTTOM to DeployEdge(y = 900f, xStart = 300f, xEnd = 2100f),
        DeploySide.LEFT to DeployEdge(x = 100f, yStart = 200f, yEnd = 800f),
        DeploySide.RIGHT to DeployEdge(x = 2300f, yStart = 200f, yEnd = 800f),
    )

    data class DeployEdge(
        val x: Float? = null, val y: Float? = null,
        val xStart: Float = 0f, val xEnd: Float = 0f,
        val yStart: Float = 0f, val yEnd: Float = 0f
    )

    override suspend fun deploy(frame: Bitmap, config: BotConfig, input: BotAccessibilityService) {
        BotLog.i("Deploying troops: side=${config.deploySide}, pattern=${config.deployPattern}")

        for (slot in config.troopSlots) {
            // 1. Tap troop slot to select
            val sx = scaler.scaleX(slotBaseX.getOrElse(slot.slotIndex) { slotBaseX[0] })
            val sy = scaler.scaleY(slotBaseY)
            input.tap(sx, sy, config.tapOffsetRange)
            RandomDelay.delayRange(200, 400)

            // 2. Deploy troops
            val edge = deployEdges[config.deploySide] ?: deployEdges[DeploySide.BOTTOM]!!
            when (config.deployPattern) {
                DeployPattern.LINE -> deployLine(input, edge, slot.dropCount, config.tapOffsetRange)
                DeployPattern.POINT -> deployPoint(input, edge, slot.dropCount, config.tapOffsetRange)
                DeployPattern.MULTI_LINE -> deployLine(input, edge, slot.dropCount, config.tapOffsetRange)
            }

            RandomDelay.delayRange(config.actionDelayMin, config.actionDelayMax)
        }

        BotLog.i("Troop deployment complete")
    }

    private suspend fun deployLine(
        input: BotAccessibilityService, edge: DeployEdge, count: Int, offset: Int
    ) {
        if (edge.y != null) {
            // Horizontal line
            val y = scaler.scaleY(edge.y)
            val xS = scaler.scaleX(edge.xStart)
            val xE = scaler.scaleX(edge.xEnd)
            val step = if (count > 1) (xE - xS) / (count - 1) else 0f
            for (i in 0 until count) {
                val x = xS + step * i
                input.tap(x, y, offset)
                RandomDelay.delayRange(30, 80)
            }
        } else if (edge.x != null) {
            // Vertical line
            val x = scaler.scaleX(edge.x)
            val yS = scaler.scaleY(edge.yStart)
            val yE = scaler.scaleY(edge.yEnd)
            val step = if (count > 1) (yE - yS) / (count - 1) else 0f
            for (i in 0 until count) {
                val y = yS + step * i
                input.tap(x, y, offset)
                RandomDelay.delayRange(30, 80)
            }
        }
    }

    private suspend fun deployPoint(
        input: BotAccessibilityService, edge: DeployEdge, count: Int, offset: Int
    ) {
        val x = scaler.scaleX(edge.x ?: ((edge.xStart + edge.xEnd) / 2))
        val y = scaler.scaleY(edge.y ?: ((edge.yStart + edge.yEnd) / 2))
        for (i in 0 until count) {
            input.tap(x, y, offset)
            RandomDelay.delayRange(30, 80)
        }
    }
}