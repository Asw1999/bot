package com.cocbot.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple log buffer for overlay display + logcat.
 */
object BotLog {
    private const val TAG = "CocBot"
    private const val MAX_LINES = 200
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val lines = CopyOnWriteArrayList<String>()
    var onNewLine: ((String) -> Unit)? = null

    fun i(msg: String) {
        val line = "[${sdf.format(Date())}] $msg"
        Log.i(TAG, msg)
        append(line)
    }

    fun e(msg: String) {
        val line = "[${sdf.format(Date())}] ❌ $msg"
        Log.e(TAG, msg)
        append(line)
    }

    fun w(msg: String) {
        val line = "[${sdf.format(Date())}] ⚠ $msg"
        Log.w(TAG, msg)
        append(line)
    }

    private fun append(line: String) {
        lines.add(line)
        if (lines.size > MAX_LINES) lines.removeAt(0)
        onNewLine?.invoke(line)
    }

    fun getLines(): List<String> = lines.toList()
    fun clear() = lines.clear()
}