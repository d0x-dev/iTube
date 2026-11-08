package com.github.itube.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.Image
import com.github.itube.FastHoldPill
import com.github.itube.aspectFit
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.github.itube.SleepTimerState
import com.github.itube.ui.components.TrackBlurPopup
import com.github.itube.ui.components.popupContainerColor
import com.github.itube.ui.components.popupScrimColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.github.itube.CommentsPanel
import com.github.itube.CommentsState
import com.github.itube.DownloadSheet
import com.github.itube.MusicPlayerState
import com.github.itube.MusicTrack
import com.github.itube.R
import com.github.itube.StreamCache
import com.github.itube.VideoItem
import com.github.itube.highQualityThumbnail
import com.github.itube.ui.components.WavySlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    start + (stop - start) * fraction

private fun lerpSp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit =
    (start.value + (stop.value - start.value) * fraction).sp

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/**
 * AirBeats Immersive Refresh Music Player Screen.
 * Full screen high-quality thumbnail with lower ambient blur effect,
 * circular like/unlike/comment/share action buttons, expressive wavy seek bar,
 * quality indicator chip, playback controls, and bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosStyledPlayer(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit = { MusicPlayerState.expanded = false },
    onSwitchToVideo: ((VideoItem, Long) -> Unit)? = null,
    onAddToPlaylist: ((VideoItem) -> Unit)? = null,
) {
    val track = MusicPlayerState.currentTrack ?: return
    val isPlaying = MusicPlayerState.isPlaying
    val isLoading = MusicPlayerState.isLoading
    val position = MusicPlayerState.positionMs
    val duration = MusicPlayerState.durationMs
    val isLiked = track.liked
    var isDisliked by remember(track.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // Video Mode States (Persisted across collapse in MusicPlayerState)
    val isVideoMode = MusicPlayerState.isVideoMode
    val isVideoFullscreen = MusicPlayerState.isVideoFullscreen
    var isVideoLoading by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQualityOption>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQualityOption?>(null) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var currentVideoTrackId by remember { mutableStateOf<String?>(null) }
    var loadedQualityHeight by remember { mutableIntStateOf(0) }

    // Companion Video Player (Owned by MusicPlayerState, retained when collapsed)
    val videoPlayer = remember(context) { MusicPlayerState.getOrCreateVideoPlayer(context) }

    // Video aspect ratio tracking for pixel-perfect zoom-to-fill (no stretching!)
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    DisposableEffect(videoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspect = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }
        }
        videoPlayer.addListener(listener)
        val sz = videoPlayer.videoSize
        if (sz.width > 0 && sz.height > 0) {
            videoAspect = sz.width.toFloat() / sz.height.toFloat()
        }
        onDispose {
            videoPlayer.removeListener(listener)
        }
    }

    // Load video stream when video mode is active, or track changes, or quality changes
    LaunchedEffect(track.id, isVideoMode, MusicPlayerState.selectedVideoQuality) {
        if (!isVideoMode) {
            videoPlayer.pause()
            return@LaunchedEffect
        }
        val targetHeight = MusicPlayerState.selectedVideoQuality ?: 480
        if (currentVideoTrackId == track.id && loadedQualityHeight == targetHeight && videoPlayer.playbackState != androidx.media3.common.Player.STATE_IDLE) {
            if (MusicPlayerState.isPlaying) videoPlayer.play()
            return@LaunchedEffect
        }
        isVideoLoading = true
        try {
            val info = withContext(Dispatchers.IO) {
                StreamCache.get(track.id) ?: StreamCache.getOrLoad(track.id)
            }
            // Prefer progressive MP4s for ultra-fast, smooth playback without decoder lag
            val progressive = info.videoStreams
                .filter { it.isUrl && it.height > 0 }
                .distinctBy { it.height }
                .sortedByDescending { it.height }

            val allVideoStreams = (progressive + info.videoOnlyStreams)
                .filter { it.isUrl && it.height > 0 }
                .distinctBy { it.height }
                .sortedByDescending { it.height }

            val qualityOptions = allVideoStreams.map { s ->
                VideoQualityOption(height = s.height, label = "${s.height}p", url = s.content)
            }
            availableQualities = qualityOptions

            // Default to 480p or 360p as requested: smooth playback in square box with zero lag
            val chosenStream = qualityOptions.firstOrNull { it.height == targetHeight }
                ?: qualityOptions.firstOrNull { it.height <= targetHeight }
                ?: qualityOptions.firstOrNull { it.height == 480 }
                ?: qualityOptions.firstOrNull { it.height == 360 }
                ?: qualityOptions.firstOrNull()

            if (chosenStream != null) {
                currentVideoTrackId = track.id
                loadedQualityHeight = chosenStream.height
                selectedQuality = chosenStream
                val mediaItem = MediaItem.fromUri(chosenStream.url)
                videoPlayer.setMediaItem(mediaItem)
                videoPlayer.prepare()
                videoPlayer.seekTo(MusicPlayerState.positionMs)
                videoPlayer.setPlaybackSpeed(if (MusicPlayerState.holdingFast) 2f else MusicPlayerState.playbackSpeed)
                if (MusicPlayerState.isPlaying) videoPlayer.play() else videoPlayer.pause()
            }
        } catch (e: Exception) {
            android.util.Log.w("IosStyledPlayer", "Failed to load video stream for ${track.id}: ${e.message}")
        } finally {
            isVideoLoading = false
        }
    }

    // Play / Pause sync
    LaunchedEffect(isPlaying) {
        if (isVideoMode) {
            if (isPlaying) videoPlayer.play() else videoPlayer.pause()
        }
    }

    // Playback Speed sync (including 2x Fast Hold)
    LaunchedEffect(isVideoMode, MusicPlayerState.holdingFast, MusicPlayerState.playbackSpeed) {
        if (isVideoMode) {
            val targetSpeed = if (MusicPlayerState.holdingFast) 2f else MusicPlayerState.playbackSpeed
            videoPlayer.setPlaybackSpeed(targetSpeed)
        }
    }

    // Periodic time drift sync without micro-seeking stutter
    LaunchedEffect(isVideoMode, isPlaying) {
        while (isVideoMode && isPlaying) {
            delay(1500)
            val audioPos = MusicPlayerState.positionMs
            val videoPos = videoPlayer.currentPosition
            if (kotlin.math.abs(audioPos - videoPos) > 1500L) {
                videoPlayer.seekTo(audioPos)
            }
        }
    }

    var zoomFeedbackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(zoomFeedbackText) {
        if (zoomFeedbackText != null) {
            delay(1400)
            zoomFeedbackText = null
        }
    }

    val isLyricsOpen = MusicPlayerState.lyricsOpen
    val lyricsProgress by animateFloatAsState(
        targetValue = if (isLyricsOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "lyricsProgress"
    )

    var areLyricsControlsVisible by remember { mutableStateOf(true) }
    var lastLyricsInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLyricsOpen) {
        if (isLyricsOpen) {
            areLyricsControlsVisible = true
            lastLyricsInteractionTime = System.currentTimeMillis()
            if (MusicPlayerState.lyrics.isEmpty() && !MusicPlayerState.lyricsLoading) {
                MusicPlayerState.refreshLyrics()
            }
        } else {
            areLyricsControlsVisible = true
        }
    }

    LaunchedEffect(track.id, isLyricsOpen) {
        if (isLyricsOpen && MusicPlayerState.lyrics.isEmpty() && !MusicPlayerState.lyricsLoading) {
            MusicPlayerState.refreshLyrics()
        }
    }

    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLyricsOpen, lastLyricsInteractionTime, areLyricsControlsVisible, isSeeking) {
        if (isLyricsOpen && areLyricsControlsVisible && !isSeeking) {
            delay(2000L)
            areLyricsControlsVisible = false
        }
    }

    val onLyricsInteraction: () -> Unit = {
        lastLyricsInteractionTime = System.currentTimeMillis()
        if (!areLyricsControlsVisible) {
            areLyricsControlsVisible = true
        }
    }

    val density = LocalDensity.current

    val controlsTranslationY by animateFloatAsState(
        targetValue = if (!isLyricsOpen || areLyricsControlsVisible) 0f else with(density) { 340.dp.toPx() },
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "lyricsControlsTranslationY"
    )

    val controlsAlpha by animateFloatAsState(
        targetValue = if (!isLyricsOpen || areLyricsControlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "lyricsControlsAlpha"
    )

    val lyricsBottomPadding by animateDpAsState(
        targetValue = if (!areLyricsControlsVisible && isLyricsOpen) 24.dp else 295.dp,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "lyricsBottomPadding"
    )

    BackHandler(enabled = MusicPlayerState.expanded) {
        if (isLyricsOpen) {
            MusicPlayerState.lyricsOpen = false
        } else if (MusicPlayerState.isVideoFullscreen) {
            MusicPlayerState.isVideoFullscreen = false
            MusicPlayerState.videoFullscreenZoomMode = 0
            zoomFeedbackText = "Square Mode"
        } else {
            onCollapse()
        }
    }

    val safeDuration = duration.coerceAtLeast(1L)
    val displayPos = if (isSeeking) seekPosition else position
    val remaining = (duration - displayPos).coerceAtLeast(0L)

    var commentsOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation()
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (MusicPlayerState.startFastHold()) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        try {
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            MusicPlayerState.endFastHold()
                        }
                    }
                }
            }
    ) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight

        // Continuous Avatar Morphing Calculations
        val fullArtSize = minOf(maxWidthDp - 48.dp, 330.dp, maxHeightDp * 0.38f)
        val normalArtX = (maxWidthDp - fullArtSize) / 2
        val normalArtY = statusBarTop + 48.dp

        val collapsedArtSize = 48.dp
        val collapsedArtX = 20.dp
        val collapsedArtY = statusBarTop + 14.dp

        val currentArtSize = lerpDp(fullArtSize, collapsedArtSize, lyricsProgress)
        val currentArtX = lerpDp(normalArtX, collapsedArtX, lyricsProgress)
        val currentArtY = lerpDp(normalArtY, collapsedArtY, lyricsProgress)
        val currentArtCorner = lerpDp(16.dp, 10.dp, lyricsProgress)
        val currentArtElevation = lerpDp(14.dp, 0.dp, lyricsProgress)

        // Title and More Button Row Morphing
        val normalTitleX = 24.dp
        val normalTitleY = normalArtY + fullArtSize + 16.dp
        val normalTitleWidth = maxWidthDp - 48.dp

        val collapsedTitleX = collapsedArtX + collapsedArtSize + 12.dp
        val collapsedTitleY = statusBarTop + 14.dp
        val collapsedTitleWidth = (maxWidthDp - collapsedTitleX - 54.dp).coerceAtLeast(100.dp)

        val currentTitleX = lerpDp(normalTitleX, collapsedTitleX, lyricsProgress)
        val currentTitleY = lerpDp(normalTitleY, collapsedTitleY, lyricsProgress)
        val currentTitleWidth = lerpDp(normalTitleWidth, collapsedTitleWidth, lyricsProgress)
        val titleScale = lerpFloat(1.0f, 0.74f, lyricsProgress)

        val normalMoreX = maxWidthDp - 24.dp - 40.dp
        val normalMoreY = normalTitleY + 2.dp
        val collapsedMoreX = maxWidthDp - 20.dp - 40.dp
        val collapsedMoreY = statusBarTop + 18.dp
        val currentMoreX = lerpDp(normalMoreX, collapsedMoreX, lyricsProgress)
        val currentMoreY = lerpDp(normalMoreY, collapsedMoreY, lyricsProgress)

        // 1. Vibrant blurred artwork layer (expands upward into full screen when lyrics open)
        val blurSeamY by animateDpAsState(
            targetValue = if (isLyricsOpen) 0.dp else (normalArtY + fullArtSize - 30.dp),
            animationSpec = tween(420, easing = FastOutSlowInEasing),
            label = "blurSeam"
        )

        AsyncImage(
            model = track.thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(65.dp)
                .graphicsLayer {
                    scaleX = 1.15f
                    scaleY = 1.15f
                }
                .drawWithCache {
                    val seamPx = blurSeamY.toPx()
                    val totalH = size.height
                    val fadeH = with(density) { 50.dp.toPx() }
                    val startY = (seamPx - fadeH).coerceAtLeast(0f)
                    val endY = seamPx.coerceAtLeast(startY + 1f)

                    val maskBrush = if (blurSeamY <= 1.dp) {
                        Brush.verticalGradient(
                            0f to Color.Black,
                            1f to Color.Black
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            (startY / totalH).coerceIn(0f, 1f) to Color.Transparent,
                            (endY / totalH).coerceIn(0f, 1f) to Color.Black,
                            1f to Color.Black
                        )
                    }
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = maskBrush, blendMode = BlendMode.DstIn)
                    }
                }
        )

        // Luminous Scrim Overlay (vibrant gradient without muddy dark overlays)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = lerpFloat(0.40f, 0.20f, lyricsProgress)),
                            0.50f to Color.Black.copy(alpha = lerpFloat(0.55f, 0.35f, lyricsProgress)),
                            1f to Color.Black.copy(alpha = lerpFloat(0.85f, 0.58f, lyricsProgress)),
                        )
                    )
                )
        )

        // Fullscreen Video (When in Video Mode & Fullscreen)
        if (isVideoMode && isVideoFullscreen) {
            val zoomMode = MusicPlayerState.videoFullscreenZoomMode
            val scaleVal by animateFloatAsState(
                targetValue = when (zoomMode) {
                    0 -> 1.0f
                    1 -> 1.0f
                    else -> 1.28f
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
                label = "fullscreenZoomScale"
            )
            val bottomPaddingDp by animateDpAsState(
                targetValue = if (zoomMode == 0) 60.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "fullscreenBottomPadding"
            )
            val topPaddingDp by animateDpAsState(
                targetValue = if (zoomMode == 0) 58.dp else 0.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "fullscreenTopPadding"
            )
            val verticalBias by animateFloatAsState(
                targetValue = if (zoomMode == 0) 1.0f else 0.0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "fullscreenVerticalBias"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topPaddingDp.coerceAtLeast(0.dp),
                        bottom = bottomPaddingDp.coerceAtLeast(0.dp)
                    )
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            videoPlayer.setVideoTextureView(this)
                        }
                    },
                    update = {
                        videoPlayer.setVideoTextureView(it)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .videoAspectFit(
                            aspect = videoAspect,
                            cover = zoomMode >= 1,
                            verticalBias = verticalBias
                        )
                        .graphicsLayer {
                            scaleX = scaleVal
                            scaleY = scaleVal
                        }
                )
            }

            if (zoomMode >= 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.35f),
                                    0.25f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.40f),
                                    0.80f to Color.Black.copy(alpha = 0.80f),
                                    1f to Color.Black.copy(alpha = 0.95f),
                                )
                            )
                        )
                )
            }
        }

        // Top Drag Handle Pill '—'
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 10.dp)
                .size(width = 38.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.55f))
        )

        // Playing from caption
        Text(
            text = "Playing from ${track.artists.ifBlank { track.title }}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.70f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 20.dp, start = 48.dp, end = 48.dp)
                .graphicsLayer {
                    alpha = (1f - lyricsProgress * 2.5f).coerceIn(0f, 1f)
                }
        )

        // Normal Collapse Chevron (fades out in lyrics mode)
        IconButton(
            onClick = onCollapse,
            modifier = Modifier
                .offset(x = 10.dp, y = statusBarTop + 4.dp)
                .size(44.dp)
                .graphicsLayer {
                    alpha = (1f - lyricsProgress * 3f).coerceIn(0f, 1f)
                },
            enabled = lyricsProgress < 0.2f
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(34.dp)
            )
        }

        // Top Song / Video Switch Pill (Always Centered, fades out in lyrics mode)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 38.dp)
                .graphicsLayer {
                    alpha = (1f - lyricsProgress * 2.5f).coerceIn(0f, 1f)
                }
        ) {
            MusicVideoSwitchPill(
                isVideoMode = isVideoMode,
                onToggle = { toVideo ->
                    if (lyricsProgress < 0.2f) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        MusicPlayerState.isVideoMode = toVideo
                        if (!toVideo) {
                            MusicPlayerState.isVideoFullscreen = false
                            MusicPlayerState.videoFullscreenZoomMode = 0
                        }
                    }
                }
            )
        }

        // Unified Animated Avatar Card (Continuous morphing between normal card and corner thumbnail)
        Box(
            modifier = Modifier
                .offset(x = currentArtX, y = currentArtY)
                .size(currentArtSize)
                .shadow(currentArtElevation, RoundedCornerShape(currentArtCorner))
                .clip(RoundedCornerShape(currentArtCorner))
                .background(Color.Black.copy(alpha = 0.25f))
                .then(
                    if (lyricsProgress > 0.8f) {
                        Modifier.clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            MusicPlayerState.lyricsOpen = false
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (isVideoMode && !isVideoFullscreen && lyricsProgress < 0.5f) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                videoPlayer.setVideoTextureView(this)
                            }
                        },
                        update = {
                            videoPlayer.setVideoTextureView(it)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectFit(videoAspect, cover = true)
                    )

                    if (isVideoLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp).align(Alignment.Center)
                        )
                    }

                    // Top-right Quality Badge Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        onClick = { showQualitySheet = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HighQuality,
                                contentDescription = "Quality",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = selectedQuality?.label ?: if (loadedQualityHeight > 0) "${loadedQualityHeight}p" else "480p",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Bottom-right Fullscreen Toggle Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            MusicPlayerState.videoFullscreenZoomMode = 0
                            MusicPlayerState.isVideoFullscreen = true
                            zoomFeedbackText = "Original Fit"
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                AsyncImage(
                    model = track.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Unified Title and Artist Column (morphs smoothly between normal position and header beside avatar)
        Column(
            modifier = Modifier
                .offset(x = currentTitleX, y = currentTitleY)
                .width(currentTitleWidth)
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    scaleX = titleScale
                    scaleY = titleScale
                }
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artists,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        // 3-Dots More Options Button (morphs between normal position below avatar and header position)
        Box(
            modifier = Modifier
                .offset(x = currentMoreX, y = currentMoreY)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable {
                    onLyricsInteraction()
                    optionsOpen = true
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = "More Options",
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                modifier = Modifier.size(24.dp)
            )
        }

        // Lyric One-Liner Preview
        if (lyricsProgress < 0.99f) {
            val teaserLyric = remember(MusicPlayerState.lyrics, position) {
                val currentLine = MusicPlayerState.lyrics.lastOrNull { it.timeMs <= position }
                currentLine?.text?.ifBlank { null }
                    ?: MusicPlayerState.lyrics.firstOrNull { it.text.isNotBlank() }?.text
                    ?: "Lyrics"
            }
            Row(
                modifier = Modifier
                    .offset(x = 24.dp, y = normalTitleY + 54.dp)
                    .width(maxWidthDp - 48.dp)
                    .graphicsLayer {
                        alpha = (1f - lyricsProgress * 3f).coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        MusicPlayerState.lyricsOpen = true
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teaserLyric,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "›",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }
        }

        // In-Player Synced Lyrics Screen
        if (lyricsProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop + 68.dp)
                    .padding(bottom = lyricsBottomPadding)
                    .graphicsLayer {
                        alpha = lyricsProgress
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onLyricsInteraction()
                    }
            ) {
                AirBeatsLyricsV2Content(
                    lyrics = MusicPlayerState.lyrics,
                    sliderPositionProvider = { if (isSeeking) seekPosition else null },
                    onSeek = { seekPos ->
                        onLyricsInteraction()
                        MusicPlayerState.seekTo(seekPos)
                        if (isVideoMode) {
                            videoPlayer.seekTo(seekPos)
                        }
                    },
                    onUserInteraction = onLyricsInteraction,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Zoom Mode Feedback Pill
        AnimatedVisibility(
            visible = zoomFeedbackText != null,
            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 2 },
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = zoomFeedbackText.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // Fast Forward 2× Pill Indicator
        FastHoldPill(
            shown = MusicPlayerState.holdingFast,
            fullscreen = isVideoMode && isVideoFullscreen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
        )

        // Bottom Controls Anchored Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 18.dp)
                .graphicsLayer {
                    translationY = controlsTranslationY
                    alpha = controlsAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action buttons row (Like, Dislike, Comment, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Like
                ImmersiveCircleActionButton(
                    icon = if (isLiked) R.drawable.yt_fill_experimental_thumb_up_vd_theme_24 else R.drawable.yt_outline_experimental_thumb_up_vd_theme_24,
                    isActive = isLiked,
                    contentDescription = "Like",
                    onClick = {
                        onLyricsInteraction()
                        MusicPlayerState.toggleLike()
                    }
                )

                // 2. Dislike
                ImmersiveCircleActionButton(
                    icon = if (isDisliked) R.drawable.yt_fill_experimental_thumb_down_vd_theme_24 else R.drawable.yt_outline_experimental_thumb_down_vd_theme_24,
                    isActive = isDisliked,
                    contentDescription = "Dislike",
                    onClick = {
                        onLyricsInteraction()
                        isDisliked = !isDisliked
                    }
                )

                // 3. Comment
                ImmersiveCircleActionButton(
                    icon = R.drawable.ic_comment,
                    isActive = commentsOpen,
                    contentDescription = "Comments",
                    onClick = {
                        onLyricsInteraction()
                        commentsOpen = true
                    }
                )

                // 4. Share
                ImmersiveCircleActionButton(
                    icon = R.drawable.ic_share,
                    isActive = false,
                    contentDescription = "Share",
                    onClick = {
                        onLyricsInteraction()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${track.id}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Music"))
                    }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Wavy Seek Bar (AirBeats Expressive Squiggly Slider) ──
            WavySlider(
                value = displayPos.toFloat(),
                onValueChange = {
                    onLyricsInteraction()
                    isSeeking = true
                    seekPosition = it.toLong().coerceIn(0L, safeDuration)
                    if (isVideoMode) {
                        videoPlayer.seekTo(seekPosition)
                    }
                },
                onValueChangeFinished = {
                    onLyricsInteraction()
                    MusicPlayerState.seekTo(seekPosition)
                    if (isVideoMode) {
                        videoPlayer.seekTo(seekPosition)
                    }
                    isSeeking = false
                },
                valueRange = 0f..safeDuration.toFloat(),
                isPlaying = isPlaying,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                thumbColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // ── Time & Audio Quality Tag Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(displayPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )

                // Center Audio Quality Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            onLyricsInteraction()
                            optionsOpen = true
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.85f)),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "YouTube • 128k",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }

                Text(
                    text = "-${formatTime(remaining)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Main Playback Controls (Previous, Play/Pause, Next) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = {
                        onLyricsInteraction()
                        MusicPlayerState.skipPrevious()
                    },
                    modifier = Modifier.size(54.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable {
                            onLyricsInteraction()
                            MusicPlayerState.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading && !isPlaying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = {
                        onLyricsInteraction()
                        MusicPlayerState.skipNext()
                    },
                    modifier = Modifier.size(54.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Bottom Action Controls (Queue, Lyrics, YouTube Pill) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Queue Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                onLyricsInteraction()
                                MusicPlayerState.queueOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue",
                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.90f)),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Lyrics Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLyricsOpen) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                            )
                            .clickable {
                                onLyricsInteraction()
                                MusicPlayerState.lyricsOpen = !MusicPlayerState.lyricsOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = "Lyrics",
                            colorFilter = ColorFilter.tint(
                                if (isLyricsOpen) Color.White else Color.White.copy(alpha = 0.90f)
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // YouTube Pill Button -> Directly opens YouTube
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable {
                            onLyricsInteraction()
                            val videoId = track.id
                            try {
                                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                                    setPackage("com.google.android.youtube")
                                }
                                context.startActivity(ytIntent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                                context.startActivity(webIntent)
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.youtube),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "YouTube",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ── 4. Comments Bottom Panel Overlay ──
        if (commentsOpen) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val commentsState = remember(track.id) { CommentsState(scope) }
                LaunchedEffect(track.id) {
                    commentsState.load(track.id)
                }
                CommentsPanel(
                    state = commentsState,
                    partialTop = maxHeight * 0.42f,
                    onClose = { commentsOpen = false }
                )
            }
        }

        // ── 5. Options Modal Sheet ──
        if (optionsOpen) {
            MusicOptionsModal(
                track = track,
                onDismiss = { optionsOpen = false },
                onDownload = {
                    optionsOpen = false
                    showDownloadSheet = true
                },
                onAddToPlaylist = onAddToPlaylist,
                onSwitchToVideo = {
                    optionsOpen = false
                    val pos = MusicPlayerState.positionMs
                    onCollapse()
                    onSwitchToVideo?.invoke(track.asVideoItem(), pos)
                }
            )
        }

        // ── 6. Download Sheet Overlay ──
        if (showDownloadSheet) {
            DownloadSheet(
                video = track.asVideoItem(),
                onDismiss = { showDownloadSheet = false }
            )
        }

        // ── 7. Video Quality Sheet Overlay ──
        if (showQualitySheet) {
            VideoQualitySheet(
                qualities = availableQualities,
                selected = selectedQuality,
                onSelect = {
                    selectedQuality = it
                    MusicPlayerState.selectedVideoQuality = it?.height
                    showQualitySheet = false
                },
                onDismiss = { showQualitySheet = false }
            )
        }
    }
}

/**
 * Circular frosted action button for Like, Unlike, Comments, Share.
 */
@Composable
private fun ImmersiveCircleActionButton(
    icon: Int,
    isActive: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.10f),
        label = "btnBgColor"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Options modal sheet shown on 3-dot menu or YouTube pill tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicOptionsModal(
    track: MusicTrack,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: ((VideoItem) -> Unit)? = null,
    onSwitchToVideo: () -> Unit,
) {
    TrackBlurPopup()
    val c = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val currentSpeed = MusicPlayerState.playbackSpeed

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = popupContainerColor(c.surfaceContainer),
        contentColor = c.onSurface,
        scrimColor = popupScrimColor(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Track preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.artists,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = c.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Speed Selection Sub-View
            if (showSpeedDialog) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSpeedDialog = false }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
                    }
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.onSurface
                    )
                }

                val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                speedOptions.forEach { s ->
                    val isSelected = currentSpeed == s
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                MusicPlayerState.applyPlaybackSpeed(s)
                                showSpeedDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (s == 1.0f) "Normal (1.0x)" else "${s}x",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) c.primary else c.onSurface
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.primary)
                        }
                    }
                }
            } else if (showSleepTimerDialog) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSleepTimerDialog = false }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
                    }
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = c.onSurface
                    )
                }
                SleepTimerContent(
                    isVideo = false,
                    onDismiss = onDismiss,
                    scrollable = false,
                    onSetMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Menu item: Playback Speed
                MusicOptionItem(
                    icon = Icons.Rounded.Speed,
                    title = "Playback Speed",
                    subtitle = if (currentSpeed == 1.0f) "Normal" else "${currentSpeed}x",
                    onClick = { showSpeedDialog = true }
                )

                // Menu item: Sleep Timer
                MusicOptionItem(
                    icon = Icons.Rounded.Bedtime,
                    title = "Sleep Timer",
                    subtitle = SleepTimerState.labelForMusic(),
                    onClick = { showSleepTimerDialog = true }
                )

                // Menu item: Download
                MusicOptionItem(
                    icon = Icons.Rounded.Download,
                    title = "Download",
                    subtitle = "Save offline audio or video",
                    onClick = onDownload
                )

                // Menu item: Add to Playlist
                MusicOptionItem(
                    icon = Icons.Rounded.LibraryAdd,
                    title = "Add to Playlist",
                    subtitle = "Save to your library or custom playlist",
                    onClick = {
                        onDismiss()
                        onAddToPlaylist?.invoke(track.asVideoItem())
                    }
                )

                // Menu item: Play Next
                MusicOptionItem(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Play Next",
                    subtitle = "Insert track next in line",
                    onClick = {
                        MusicPlayerState.addToQueue(track)
                        Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // Menu item: Add to Queue
                MusicOptionItem(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    title = "Add to Queue",
                    subtitle = "Append track to playback queue",
                    onClick = {
                        MusicPlayerState.addToQueue(track)
                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // Menu item: Watch Video
                MusicOptionItem(
                    icon = Icons.Rounded.PlayArrow,
                    title = "Watch Music Video",
                    subtitle = "Switch to video player",
                    onClick = onSwitchToVideo
                )

                // Menu item: Share
                MusicOptionItem(
                    icon = Icons.Rounded.Share,
                    title = "Share",
                    subtitle = "Share song link",
                    onClick = {
                        onDismiss()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${track.id}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Music"))
                    }
                )

                // Menu item: Copy Link
                MusicOptionItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "Copy Link",
                    subtitle = "Copy track URL to clipboard",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Link", "https://youtu.be/${track.id}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MusicOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = c.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%d:%02d", minutes, seconds)
}

/** Converts a music entry for the existing video-only sheets without a backend dependency. */
private fun MusicTrack.asVideoItem(): VideoItem = VideoItem(
    id = id,
    title = title,
    channel = artists,
    channelUrl = artistId,
    thumbnail = thumbnailUrl,
    avatar = null,
    durationSec = durationSec,
    views = 0L,
    uploaded = null,
)

/**
 * Top pill toggle switch between Song (Headphones) and Video mode.
 * Matches YouTube Music's iconic player switch.
 */
@Composable
fun MusicVideoSwitchPill(
    isVideoMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillWidth = 104.dp
    val pillHeight = 40.dp
    val thumbWidth = 48.dp
    val thumbHeight = 32.dp
    val padding = 4.dp
    val maxOffset = (pillWidth - thumbWidth - padding).coerceAtLeast(0.dp)

    val thumbOffset by animateDpAsState(
        targetValue = if (isVideoMode) maxOffset else padding,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "pillThumb"
    )

    Box(
        modifier = modifier
            .width(pillWidth)
            .height(pillHeight)
            .clip(RoundedCornerShape(pillHeight / 2))
            .background(Color.Black.copy(alpha = 0.52f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(pillHeight / 2))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                onToggle(!isVideoMode)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding Thumb
        Box(
            modifier = Modifier
                .padding(start = thumbOffset.coerceAtLeast(0.dp))
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(thumbHeight / 2))
                .background(Color.White.copy(alpha = 0.22f)),
        )

        // Icons Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Audio/Headphones (Song mode)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isVideoMode) onToggle(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Song Mode",
                    tint = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right: Video (Video mode)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isVideoMode) onToggle(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartDisplay,
                    contentDescription = "Video Mode",
                    tint = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class VideoQualityOption(
    val height: Int,
    val label: String,
    val url: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoQualitySheet(
    qualities: List<VideoQualityOption>,
    selected: VideoQualityOption?,
    onSelect: (VideoQualityOption?) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBlurPopup()
    val c = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = c.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Auto Option
            QualityOptionRow(
                label = "Auto (Recommended)",
                isSelected = selected == null,
                onClick = {
                    onSelect(null)
                    onDismiss()
                }
            )

            qualities.forEach { opt ->
                QualityOptionRow(
                    label = "${opt.label}${if (opt.height >= 720) " (HD)" else ""}",
                    isSelected = selected?.height == opt.height,
                    onClick = {
                        onSelect(opt)
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QualityOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) c.primary else c.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Layout modifier for fullscreen video in iMusic.
 * - In original fit (cover = false, verticalBias = 1f): aligns placeable to the bottom of the container,
 *   resting just above the song metadata.
 * - In zoom modes (cover = true): centers and fills the available display bounds.
 */
private fun Modifier.videoAspectFit(
    aspect: Float,
    cover: Boolean = false,
    verticalBias: Float = 0f
) = layout { measurable, constraints ->
    val boxW = constraints.maxWidth.toFloat()
    val boxH = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else boxW / aspect
    val byHeight = (boxW / boxH > aspect) != cover
    val w = (if (byHeight) boxH * aspect else boxW).roundToInt()
    val h = (if (byHeight) boxH else boxW / aspect).roundToInt()
    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
    layout(boxW.roundToInt(), boxH.roundToInt()) {
        val x = ((boxW - w) / 2f).roundToInt()
        val bias = if (cover) 0.5f else ((verticalBias + 1f) / 2f).coerceIn(0f, 1f)
        val y = ((boxH - h) * bias).roundToInt()
        placeable.place(x, y)
    }
}
