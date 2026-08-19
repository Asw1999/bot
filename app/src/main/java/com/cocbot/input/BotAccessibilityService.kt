package com.cocbot.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.cocbot.util.BotLog
import com.cocbot.util.RandomDelay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BotAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        BotLog.i("Accessibility Service connected")
    }

    override fun onDestroy() {
        instance = null
        BotLog.i("Accessibility Service disconnected")
        super.onDestroy()
    }

    /** Dispatch a gesture and suspend until complete */
    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean =
        suspendCoroutine { cont ->
            val ok = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(false)
                }
            }, null)
            if (!ok) cont.resume(false)
        }

    /** Tap at (x,y) with optional random offset */
    suspend fun tap(x: Float, y: Float, offsetRange: Int = 0): Boolean {
        val ox = x + RandomDelay.offset(offsetRange)
        val oy = y + RandomDelay.offset(offsetRange)
        val path = Path().apply { moveTo(ox, oy) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture)
    }

    /** Swipe from (x1,y1) to (x2,y2) */
    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture)
    }

    /** Long press at (x,y) */
    suspend fun longPress(x: Float, y: Float, durationMs: Long = 1000): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture)
    }

    companion object {
        @Volatile
        var instance: BotAccessibilityService? = null
            private set

        val isConnected: Boolean get() = instance != null
    }
}