package com.cocbot.util

import android.content.Context

/**
 * Scales coordinates from base resolution (2400x1080) to actual device resolution.
 */
class CoordScaler(ctx: Context) {
    companion object {
        const val BASE_W = 2400f
        const val BASE_H = 1080f
    }

    private val metrics = ctx.resources.displayMetrics
    val scaleX: Float = metrics.widthPixels / BASE_W
    val scaleY: Float = metrics.heightPixels / BASE_H
    val deviceW: Int = metrics.widthPixels
    val deviceH: Int = metrics.heightPixels

    /** Scale base coords to device coords */
    fun scaleX(x: Float): Float = x * scaleX
    fun scaleY(y: Float): Float = y * scaleY
    fun scale(x: Float, y: Float): Pair<Float, Float> = scaleX(x) to scaleY(y)

    /** Inverse: device coords back to base */
    fun unscaleX(x: Float): Float = x / scaleX
    fun unscaleY(y: Float): Float = y / scaleY
}