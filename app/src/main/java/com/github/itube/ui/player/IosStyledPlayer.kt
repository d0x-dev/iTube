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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import kotlin.math.abs
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.text.TextStyle
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
    val isQueueOpen = MusicPlayerState.queueOpen
    val isPanelOpen = isLyricsOpen || isQueueOpen

    val panelProgress by animateFloatAsState(
        targetValue = if (isPanelOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "panelProgress"
    )

    var areLyricsControlsVisible by remember { mutableStateOf(true) }
    var lastLyricsInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPanelOpen) {
        if (isPanelOpen) {
            areLyricsControlsVisible = true
            lastLyricsInteractionTime = System.currentTimeMillis()
            if (isLyricsOpen && MusicPlayerState.lyrics.isEmpty() && !MusicPlayerState.lyricsLoading) {
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

    LaunchedEffect(isPanelOpen, lastLyricsInteractionTime, areLyricsControlsVisible, isSeeking) {
        if (isPanelOpen && areLyricsControlsVisible && !isSeeking) {
            delay(2000L)
            areLyricsControlsVisible = false
        }
    }

    val onLyricsInteraction: () -> Unit = {
        lastLyricsInteractionTime = System.currentTimeMillis()
        areLyricsControlsVisible = !areLyricsControlsVisible
    }

    val density = LocalDensity.current

    val controlsTranslationY by animateFloatAsState(
        targetValue = if (!isPanelOpen || areLyricsControlsVisible) 0f else with(density) { 340.dp.toPx() },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "lyricsControlsTranslationY"
    )

    val controlsAlpha by animateFloatAsState(
        targetValue = if (!isPanelOpen || areLyricsControlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "lyricsControlsAlpha"
    )

    val contentBottomPadding by animateDpAsState(
        targetValue = if (!areLyricsControlsVisible && isPanelOpen) 24.dp else 295.dp,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "contentBottomPadding"
    )

    BackHandler(enabled = MusicPlayerState.expanded) {
        if (isQueueOpen) {
            MusicPlayerState.queueOpen = false
        } else if (isLyricsOpen) {
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

    var rootBoxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var titleBottomPadding by remember { mutableStateOf(350.dp) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { rootBoxCoords = it }
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
        val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val fullBleedHeight = maxHeight * 0.52f

        // ── 1. Ambient Blurred Artwork Layer (Down side in music mode, zooms to fill whole screen in lyrics/queue) ──
        val blurTranslateY by animateDpAsState(
            targetValue = if (isPanelOpen) 0.dp else 190.dp,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "blurTranslateY"
        )
        val blurScale by animateFloatAsState(
            targetValue = if (isPanelOpen) 1.28f else 1.15f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "blurScale"
        )

        AsyncImage(
            model = track.thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = blurTranslateY)
                .graphicsLayer {
                    scaleX = blurScale
                    scaleY = blurScale
                    alpha = 0.95f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .blur(50.dp)
        )

        // ── 2. Luminous Scrim Gradient (Background Scrim Overlay - Rich Colors, never pitch black) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.30f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.25f),
                            1f to Color.Black.copy(alpha = 0.52f),
                        )
                    )
                )
        )

        // ── Fullscreen Video (When in Video Mode & Fullscreen) ──
        if (isVideoMode && isVideoFullscreen) {
            val zoomMode = MusicPlayerState.videoFullscreenZoomMode
            val scaleVal by animateFloatAsState(
                targetValue = when (zoomMode) {
                    0 -> 1.0f // Fit (own resolution)
                    1 -> 1.0f // Fill (zoom to cover)
                    else -> 1.28f // Deep zoom (bounded so key elements remain visible)
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f),
                label = "fullscreenZoomScale"
            )
            val bottomPaddingDp by animateDpAsState(
                targetValue = if (zoomMode == 0) (titleBottomPadding + 8.dp).coerceAtLeast(0.dp) else 0.dp,
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
                    .zIndex(15f)
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

            // In Zoom to Fill / Deep Zoom (zoomMode >= 1), add a subtle legibility scrim over the full-bleed video
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

        // ── Square Video Window (When in Video Mode, not fullscreen) ──
        if (isVideoMode && !isVideoFullscreen && panelProgress < 0.99f) {
            Box(
                modifier = Modifier
                    .zIndex(15f)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 58.dp, bottom = 270.dp, start = 20.dp, end = 20.dp)
                    .graphicsLayer {
                        alpha = (1f - panelProgress).coerceIn(0f, 1f)
                        scaleX = 1f - (panelProgress * 0.18f)
                        scaleY = 1f - (panelProgress * 0.18f)
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                                .aspectFit(videoAspect, cover = true)
                        )
                    }

                    if (isVideoLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Top-right Quality Badge Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        onClick = { showQualitySheet = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HighQuality,
                                contentDescription = "Quality",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
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
                            .padding(10.dp)
                            .size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── In-Player Synced Lyrics Screen ──
        if (isLyricsOpen && panelProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .zIndex(5f)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 66.dp)
                    .padding(bottom = contentBottomPadding)
                    .graphicsLayer {
                        alpha = panelProgress
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

        // ── In-Player Queue Screen ──
        if (isQueueOpen && panelProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .zIndex(5f)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 66.dp)
                    .padding(bottom = contentBottomPadding)
                    .graphicsLayer {
                        alpha = panelProgress
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onLyricsInteraction()
                    }
            ) {
                InPlayerQueueContent(
                    currentTrack = track,
                    queue = MusicPlayerState.queue,
                    currentIndex = MusicPlayerState.queueIndex,
                    isPlaying = isPlaying,
                    onTrackClick = { clickedTrack: MusicTrack ->
                        MusicPlayerState.play(clickedTrack, null)
                    },
                    onRemoveTrack = { idx: Int ->
                        MusicPlayerState.removeFromQueue(idx)
                    },
                    onUserInteraction = onLyricsInteraction,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Continuous Morphing Artwork Avatar (Sharp artwork layer over the blurred layer) ──
        val showArtworkAvatar = (!isVideoMode || isPanelOpen) && (!isVideoFullscreen || isPanelOpen)
        if (showArtworkAvatar) {
            val artTop = lerpDp(0.dp, statusBarTopDp + 6.dp, panelProgress)
            val artStart = lerpDp(0.dp, 16.dp, panelProgress)
            val artWidth = lerpDp(maxWidth, 44.dp, panelProgress)
            val artHeight = lerpDp(fullBleedHeight, 44.dp, panelProgress)
            val artRadius = lerpDp(0.dp, 10.dp, panelProgress)

            Box(
                modifier = Modifier
                    .zIndex(20f)
                    .offset { IntOffset(artStart.roundToPx(), artTop.roundToPx()) }
                    .size(width = artWidth, height = artHeight)
                    .clip(RoundedCornerShape(artRadius))
                    .then(
                        if (panelProgress > 0.85f) {
                            Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.22f * panelProgress), RoundedCornerShape(artRadius))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    MusicPlayerState.lyricsOpen = false
                                    MusicPlayerState.queueOpen = false
                                }
                        } else Modifier
                    )
            ) {
                AsyncImage(
                    model = track.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gentle blend at the bottom of the sharp avatar into the down-side blur layer in music mode
                if (panelProgress < 0.20f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(fullBleedHeight * 0.35f)
                            .graphicsLayer {
                                alpha = (1f - panelProgress * 5f).coerceIn(0f, 1f)
                            }
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.55f)
                                )
                            )
                    )
                }
            }
        }

        // ── Top Navigation Bar (Collapse Chevron, Centered Switch Pill, More Menu, In-Player Header) ──
        Box(
            modifier = Modifier
                .zIndex(25f)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Normal Collapse Chevron (fades out in lyrics or queue mode)
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        alpha = (1f - panelProgress * 2.5f).coerceIn(0f, 1f)
                    },
                enabled = panelProgress < 0.2f
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Top Song / Video Switch Pill (Always Centered in music mode, disappears in lyrics/queue mode)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = (1f - panelProgress * 2.5f).coerceIn(0f, 1f)
                    }
            ) {
                MusicVideoSwitchPill(
                    isVideoMode = isVideoMode,
                    onToggle = { toVideo ->
                        if (panelProgress < 0.2f) {
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

            // Top 3-Dots More Button (Right in its original top position!)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVideoMode && isVideoFullscreen) {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            when (MusicPlayerState.videoFullscreenZoomMode) {
                                0 -> {
                                    MusicPlayerState.videoFullscreenZoomMode = 1
                                    zoomFeedbackText = "Zoom to Fill"
                                }
                                1 -> {
                                    MusicPlayerState.videoFullscreenZoomMode = 2
                                    zoomFeedbackText = "Zoom 1.3×"
                                }
                                else -> {
                                    MusicPlayerState.videoFullscreenZoomMode = 0
                                    MusicPlayerState.isVideoFullscreen = false
                                    zoomFeedbackText = "Square Mode"
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        val icon = when (MusicPlayerState.videoFullscreenZoomMode) {
                            0 -> Icons.Rounded.ZoomOutMap
                            1 -> Icons.Rounded.ZoomIn
                            else -> Icons.Rounded.FullscreenExit
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = when (MusicPlayerState.videoFullscreenZoomMode) {
                                0 -> "Zoom to Fill"
                                1 -> "Deep Zoom"
                                else -> "Return to Square"
                            },
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onLyricsInteraction()
                        optionsOpen = true
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "More Options",
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.92f)),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Lyrics / Queue Mode Header Title & Artist (Placed right next to the morphing avatar thumbnail)
            if (panelProgress > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 70.dp, end = 52.dp)
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            alpha = panelProgress
                            translationX = (1f - panelProgress) * 20.dp.toPx()
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W700,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = track.artists,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W500,
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp
                        ),
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }

        // ── Zoom Mode Feedback Pill ──
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

        // ── Fast Forward 2× Pill Indicator ──
        FastHoldPill(
            shown = MusicPlayerState.holdingFast,
            fullscreen = isVideoMode && isVideoFullscreen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
        )

        // ── 3. Bottom Controls Anchored Column ──
        Column(
            modifier = Modifier
                .zIndex(25f)
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
            // In music mode, show the track title, artist, and action buttons (Like, Dislike, Comment, Share).
            // Smoothly fade out the metadata row on the graphics layer without causing any layout shifts or animation hitching!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .graphicsLayer {
                        alpha = (1f - panelProgress * 2f).coerceIn(0f, 1f)
                    }
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(unbounded = true)
                            .padding(end = 4.dp)
                            .align(Alignment.TopStart)
                            .graphicsLayer {
                                translationY = -50.dp.toPx()
                            }
                            .onGloballyPositioned { coords ->
                                if (!isPanelOpen && panelProgress < 0.01f) {
                                    val root = rootBoxCoords
                                    if (root != null && root.isAttached && coords.isAttached) {
                                        val pos = root.localPositionOf(coords, Offset.Zero)
                                        val titleVisualTop = pos.y - with(density) { 50.dp.toPx() }
                                        val bottomPx = (root.size.height - titleVisualTop).coerceAtLeast(0f)
                                        if (bottomPx > 0f) {
                                            val newDp = with(density) { bottomPx.toDp() }.coerceAtLeast(0.dp)
                                            if (abs(titleBottomPadding.value - newDp.value) > 2f) {
                                                titleBottomPadding = newDp
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = track.artists,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    // The buttons deliberately retain their size and on-screen position (Right aligned!)
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                        // 2. Dislike / Unlike
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
                            .background(
                                if (isQueueOpen) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                            )
                            .clickable {
                                onLyricsInteraction()
                                MusicPlayerState.lyricsOpen = false
                                MusicPlayerState.queueOpen = !MusicPlayerState.queueOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue",
                            colorFilter = ColorFilter.tint(
                                if (isQueueOpen) Color.White else Color.White.copy(alpha = 0.90f)
                            ),
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
                                MusicPlayerState.queueOpen = false
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

/**
 * In-Player Queue Content view.
 * Renders the queue directly inside the player with smooth animated transition,
 * showing the "Queue" title, currently playing card with equalizer, and upcoming tracks with drag handles.
 */
@Composable
private fun InPlayerQueueContent(
    currentTrack: MusicTrack,
    queue: List<MusicTrack>,
    currentIndex: Int,
    isPlaying: Boolean,
    onTrackClick: (MusicTrack) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val upcomingItems = if (currentIndex in 0 until queue.size - 1) {
        queue.subList(currentIndex + 1, queue.size)
    } else {
        emptyList()
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onUserInteraction()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onUserInteraction()
        },
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // "Queue" Title Row with Shuffle & Repeat Capsule at the end
        item(key = "header_queue") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    style = TextStyle(
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.W800,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                // Capsule Pill (Shuffle & Repeat)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Shuffle
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                MusicPlayerState.toggleShuffle()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (MusicPlayerState.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Repeat
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                MusicPlayerState.toggleRepeat()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (MusicPlayerState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                contentDescription = "Repeat",
                                tint = if (MusicPlayerState.repeatMode != Player.REPEAT_MODE_OFF) Color.White else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // "Now playing" Subtitle
        item(key = "header_now_playing") {
            Text(
                text = "Now playing",
                style = TextStyle(
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Currently Playing Track
        item(key = "now_playing_track") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = currentTrack.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = currentTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(Modifier.width(14.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTrack.title,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W700,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentTrack.artists,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W400,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Equalizer Icon
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(12.dp))

                // Close / Remove Icon
                IconButton(
                    onClick = {
                        if (currentIndex in queue.indices) {
                            onRemoveTrack(currentIndex)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // "AutoPlay" Section Header
        item(key = "header_autoplay") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "AutoPlay",
                    style = TextStyle(
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.W700,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Similar music, selected to play next",
                    style = TextStyle(
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.W400,
                        fontSize = 13.sp
                    ),
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        // Upcoming Queued Tracks List
        itemsIndexed(
            items = upcomingItems,
            key = { idx, item -> "upcoming_${item.id}_$idx" }
        ) { relativeIdx, item ->
            val actualIdx = currentIndex + 1 + relativeIdx
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTrackClick(item) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag handle icon on left
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Reorder",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(10.dp))

                // Thumbnail
                AsyncImage(
                    model = item.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(Modifier.width(14.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.title,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W600,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.artists,
                        style = TextStyle(
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.W400,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Close / Remove icon
                IconButton(
                    onClick = { onRemoveTrack(actualIdx) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
