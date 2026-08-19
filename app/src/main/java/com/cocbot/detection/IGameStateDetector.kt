package com.cocbot.detection

import android.graphics.Bitmap
import com.cocbot.core.GameState

/** AI-swappable interface for game state detection */
interface IGameStateDetector {
    fun detect(frame: Bitmap): GameState
}

/** Future: structure detection for AI attack planning */
interface IStructureDetector {
    data class Structure(val name: String, val x: Int, val y: Int, val w: Int, val h: Int, val confidence: Float)
    fun detect(frame: Bitmap): List<Structure>
}

/** Future: base analysis for AI */
interface IBaseAnalyzer {
    data class AnalysisResult(
        val coreX: Int, val coreY: Int,
        val weakSide: String,
        val entryPoints: List<Pair<Int, Int>>
    )
    fun analyze(frame: Bitmap, structures: List<IStructureDetector.Structure>): AnalysisResult
}