package com.cocbot.detection

import android.graphics.Bitmap
import com.cocbot.util.BotLog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class LootInfo(val gold: Int, val elixir: Int, val darkElixir: Int)

/**
 * Reads loot values from opponent base screen using ML Kit OCR.
 * Loot display region is ratio-based crop from full frame.
 *
 * ponytail: hardcoded crop ratios for 2400x1080. Adjust if base resolution changes.
 */
class LootReader {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Crop ratios for loot region (top-left area of opponent base screen)
    // These are approximate — tune per device by inspecting screenshots
    private val lootRegion = CropRegion(
        leftRatio = 0.02f, topRatio = 0.15f,
        widthRatio = 0.25f, heightRatio = 0.12f
    )

    data class CropRegion(val leftRatio: Float, val topRatio: Float,
                          val widthRatio: Float, val heightRatio: Float)

    suspend fun readLoot(frame: Bitmap): LootInfo? {
        val x = (frame.width * lootRegion.leftRatio).toInt()
        val y = (frame.height * lootRegion.topRatio).toInt()
        val w = (frame.width * lootRegion.widthRatio).toInt()
        val h = (frame.height * lootRegion.heightRatio).toInt()

        val cropped = Bitmap.createBitmap(frame, x, y,
            w.coerceAtMost(frame.width - x),
            h.coerceAtMost(frame.height - y))

        val image = InputImage.fromBitmap(cropped, 0)
        val text = suspendCoroutine<String?> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { cont.resume(null) }
        }
        cropped.recycle()

        if (text == null) return null
        return parseLootText(text)
    }

    private fun parseLootText(text: String): LootInfo {
        // Extract numbers from OCR text. Typical format: "409,345\n214,223\n697"
        val numbers = Regex("""\d[\d,. ]*\d|\d""")
            .findAll(text.replace(" ", ""))
            .map { it.value.replace(",", "").replace(".", "").trim().toIntOrNull() ?: 0 }
            .toList()

        val gold = numbers.getOrNull(0) ?: 0
        val elixir = numbers.getOrNull(1) ?: 0
        val dark = numbers.getOrNull(2) ?: 0

        BotLog.i("Loot OCR: gold=$gold, elixir=$elixir, dark=$dark")
        return LootInfo(gold, elixir, dark)
    }
}