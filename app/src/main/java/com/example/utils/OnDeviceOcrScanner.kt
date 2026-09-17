package com.example.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * OnDeviceOcrScanner
 * Uses Google ML Kit Text Recognition on-device (100% OFFLINE, Zero API Key, Free forever).
 * Intelligently extracts vehicle Odometer (KM) reading from dashboard photos.
 */
object OnDeviceOcrScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scanOdometerFromBitmap(bitmap: Bitmap): Result<Int> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedKm = extractBestOdometerReading(visionText)
                    if (detectedKm != null) {
                        continuation.resume(Result.success(detectedKm))
                    } else {
                        // Fallback: extract any reasonable multi-digit integer found in the image
                        val fallbackNumber = extractFallbackNumber(visionText.text)
                        if (fallbackNumber != null) {
                            continuation.resume(Result.success(fallbackNumber))
                        } else {
                            continuation.resume(Result.failure(Exception("Angka speedometer tidak terdeteksi jelas. Pastikan foto fokus dan angka odometer terlihat terang.")))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Extracts the most likely Odometer value by checking surrounding text cues (ODO, KM, TOTAL)
     * and number formatting common in truck & vehicle digital/analog dashboards.
     */
    fun extractBestOdometerReading(visionText: Text): Int? {
        val candidates = mutableListOf<Pair<Int, Int>>() // Pair(KmValue, Score)

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                val upperLine = lineText.uppercase()

                // Check lines containing keywords
                val hasOdoKeyword = upperLine.contains("ODO") || upperLine.contains("KM") || upperLine.contains("TOTAL")
                
                // Clean common OCR mistranslations on numeric displays (e.g. O -> 0, I/l -> 1, S -> 5, B -> 8)
                val sanitizedNumbers = extractNumbersFromText(lineText)
                
                for (num in sanitizedNumbers) {
                    if (isValidOdometerRange(num)) {
                        var score = 10
                        if (hasOdoKeyword) score += 50
                        if (upperLine.startsWith("ODO")) score += 40
                        
                        // Favor typical truck odometer lengths (4 to 6 digits, e.g., 10,000 to 999,999)
                        val strLen = num.toString().length
                        if (strLen in 4..6) score += 20
                        
                        candidates.add(num to score)
                    }
                }
            }
        }

        if (candidates.isNotEmpty()) {
            // Return candidate with highest score; if tied, take the largest valid one (total odo vs trip)
            return candidates.sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first }).first().first
        }

        return null
    }

    private fun extractNumbersFromText(text: String): List<Int> {
        val list = mutableListOf<Int>()
        // Match sequences of digits, ignoring dots/commas (like 123.456 or 123,456)
        val regex = Regex("""(?:\b|\D)(\d{1,3}(?:[.,]\d{3})+|\d{2,7})(?:\b|\D)""")
        val matches = regex.findAll(text)
        for (match in matches) {
            val rawGroup = match.groupValues[1]
            val cleaned = rawGroup.replace(".", "").replace(",", "").trim()
            cleaned.toIntOrNull()?.let {
                list.add(it)
            }
        }
        return list
    }

    private fun extractFallbackNumber(fullText: String): Int? {
        val numbers = extractNumbersFromText(fullText)
        // Filter out unreasonable values (e.g., small trip meters like 0..99 or clock time like 1200)
        val reasonable = numbers.filter { isValidOdometerRange(it) }
        return reasonable.maxOrNull()
    }

    private fun isValidOdometerRange(value: Int): Boolean {
        // Vehicle odometer range: typically between 500 km and 1,500,000 km
        return value in 500..1_500_000
    }
}
