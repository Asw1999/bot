package com.cocbot.util

import kotlinx.coroutines.delay
import kotlin.random.Random

object RandomDelay {
    /** Delay with ±20% jitter */
    suspend fun delay(baseMs: Long) {
        val jitter = (baseMs * 0.2).toLong()
        val actual = baseMs + Random.nextLong(-jitter, jitter + 1)
        delay(actual.coerceAtLeast(50))
    }

    /** Delay within range */
    suspend fun delayRange(minMs: Long, maxMs: Long) {
        delay(Random.nextLong(minMs, maxMs + 1).coerceAtLeast(50))
    }

    /** Random offset for tap coordinates (anti-detection) */
    fun offset(range: Int): Int = if (range > 0) Random.nextInt(-range, range + 1) else 0
}