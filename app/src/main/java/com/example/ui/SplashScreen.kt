package com.example.ui

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production-Ready Video Splash Screen
 * Features:
 * - Hardware Accelerated TextureView with Center-Crop Matrix
 * - Zero Memory Leak Lifecycle-Aware MediaPlayer
 * - Atomic single-trigger onTimeout guard
 * - Safe fallback timeout
 * - Skip pill button with visual feedback & tap-anywhere dismissal
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    
    // Guard to ensure onTimeout only triggers exactly once
    val isCompleted = remember { AtomicBoolean(false) }
    val finishOnce = remember {
        {
            if (isCompleted.compareAndSet(false, true)) {
                currentOnTimeout()
            }
        }
    }

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isVideoReady by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Fallback safety timeout slightly beyond the current 8-second asset duration.
    // It prevents a decode stall from blocking startup while allowing the video to finish normally.
    LaunchedEffect(Unit) {
        delay(9000)
        finishOnce()
    }

    // Manage Android Lifecycle for MediaPlayer (pause on background, resume on foreground, cleanup on destroy)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        if (mediaPlayerRef?.isPlaying == true) {
                            mediaPlayerRef?.pause()
                        }
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (isVideoReady && mediaPlayerRef != null && mediaPlayerRef?.isPlaying == false) {
                            mediaPlayerRef?.start()
                        }
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        mediaPlayerRef?.release()
                    } catch (_: Exception) {}
                    mediaPlayerRef = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayerRef?.release()
            } catch (_: Exception) {}
            mediaPlayerRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Dark sleek backdrop
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Tap anywhere to skip
                finishOnce()
            }
            .testTag("dapp_video_splash"),
        contentAlignment = Alignment.Center
    ) {
        // TextureView Video Player
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    val textureView = this
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(surfaceTexture)
                            try {
                                val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash_video}")
                                val player = MediaPlayer().apply {
                                    setDataSource(ctx, videoUri)
                                    setSurface(surface)
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                            .build()
                                    )
                                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                    setOnPreparedListener { mp ->
                                        adjustAspectRatio(textureView, mp.videoWidth, mp.videoHeight, width, height)
                                        mp.isLooping = false
                                        mp.start()
                                        isVideoReady = true
                                    }
                                    setOnCompletionListener {
                                        finishOnce()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        finishOnce()
                                        true
                                    }
                                    prepareAsync()
                                }
                                mediaPlayerRef = player
                            } catch (e: Exception) {
                                finishOnce()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            mediaPlayerRef?.let { mp ->
                                if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                                    adjustAspectRatio(textureView, mp.videoWidth, mp.videoHeight, width, height)
                                }
                            }
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            try {
                                mediaPlayerRef?.release()
                            } catch (_: Exception) {}
                            mediaPlayerRef = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top-Right Elegant Skip Pill Button
        AnimatedVisibility(
            visible = isVideoReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { finishOnce() }
                    .testTag("skip_splash_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Lewati",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Skip",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Adjust the TextureView matrix for a proportional center-crop presentation.
 * The same uniform scale is applied on both axes, so the video fills the view
 * without stretching; only the unavoidable edge crop varies by device ratio.
 */
private fun adjustAspectRatio(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int
) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

    val scaleX = viewWidth.toFloat() / videoWidth.toFloat()
    val scaleY = viewHeight.toFloat() / videoHeight.toFloat()
    val scale = maxOf(scaleX, scaleY)
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    val translateX = (viewWidth - scaledWidth) / 2f
    val translateY = (viewHeight - scaledHeight) / 2f

    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(translateX, translateY)
    }
    textureView.setTransform(matrix)
}
