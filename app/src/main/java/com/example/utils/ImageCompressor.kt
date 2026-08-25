package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ImageCompressor
 * Handles intelligent downscaling and WebP compression to save memory,
 * reduce database storage footprint, and accelerate network uploads.
 */
object ImageCompressor {

    const val MIME_TYPE_WEBP = "image/webp"
    private const val DEFAULT_MAX_DIMENSION = 1280
    private const val DEFAULT_WEBP_QUALITY = 80

    /**
     * Compresses a Bitmap into high-efficiency WebP format after scaling down to maxDimension.
     */
    fun compressBitmapToWebP(
        source: Bitmap,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_WEBP_QUALITY
    ): ByteArray {
        val scaledBitmap = scaleBitmapDown(source, maxDimension)
        val outputStream = ByteArrayOutputStream()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
        } else {
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
        }

        if (scaledBitmap != source) {
            scaledBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    /**
     * Decodes and compresses an image from a Content URI to WebP with optimal inSampleSize downsampling.
     */
    fun compressUriToWebP(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_WEBP_QUALITY
    ): ByteArray? {
        return try {
            val bitmap = decodeSampledBitmapFromUri(context, uri, maxDimension) ?: return null
            val compressedBytes = compressBitmapToWebP(bitmap, maxDimension, quality)
            bitmap.recycle()
            compressedBytes
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely decodes a downsampled Bitmap from Content URI to avoid OutOfMemoryError.
     */
    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? {
        val contentResolver = context.contentResolver

        // Step 1: Decode bounds only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        // Step 2: Calculate inSampleSize
        val largestDim = max(options.outWidth, options.outHeight)
        var inSampleSize = 1
        while (largestDim / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2
        }

        // Step 3: Decode full bitmap with sample size
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        var decodedBitmap: Bitmap? = null
        contentResolver.openInputStream(uri)?.use { inputStream ->
            decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
        }

        // Step 4: Scale down if still slightly larger than maxDimension
        return decodedBitmap?.let { scaleBitmapDown(it, maxDimension) }
    }

    /**
     * Scales down bitmap proportionally if any dimension exceeds maxDimension.
     */
    fun scaleBitmapDown(source: Bitmap, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) {
            return source
        }

        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (width * ratio).roundToInt()
        val targetHeight = (height * ratio).roundToInt()

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
