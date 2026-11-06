package com.github.itube.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import com.github.itube.ui.components.LyricsSkeleton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.github.itube.LyricsLine
import com.github.itube.MusicPlayerState
import com.github.itube.WordTiming
import com.github.itube.highQualityThumbnail
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fullscreen Lyrics v2 Screen with AirBeats BetterLyrics animations.
 * Provides Apple Music style synchronized line-by-line scrolling, word liquid fill,
 * singing float bounce, and interactive line tap-to-seek.
 */
@Composable
fun LyricsV2Screen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { MusicPlayerState.lyricsOpen = false },
) {
    val track = MusicPlayerState.currentTrack ?: return
    val lyrics = MusicPlayerState.lyrics
    val isLoading = MusicPlayerState.lyricsLoading
    val positionMs = MusicPlayerState.positionMs

    BackHandler(enabled = MusicPlayerState.lyricsOpen) {
        onDismiss()
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Determine current active lyric line index
    val activeIndex = remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll so active line stays in prime viewing zone
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Atmospheric artwork background blur
        AsyncImage(
            model = track.thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 65.dp)
                .graphicsLayer { alpha = 0.5f }
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.65f),
                        0.5f to Color.Black.copy(alpha = 0.8f),
                        1f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF0000)
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Lyrics content
            if (isLoading) {
                LyricsSkeleton()
            } else if (lyrics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No synced lyrics available for this song",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = index < activeIndex

                        LyricsLineRow(
                            line = line,
                            isActive = isActive,
                            isPast = isPast,
                            positionMs = positionMs,
                            onClick = {
                                MusicPlayerState.seekTo(line.timeMs)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineRow(
    line: LyricsLine,
    isActive: Boolean,
    isPast: Boolean,
    positionMs: Long,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "lineScale"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            isPast -> 0.42f
            else -> 0.32f
        },
        animationSpec = tween(300),
        label = "lineAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        if (line.words.isNotEmpty() && isActive) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center
            ) {
                line.words.forEach { word ->
                    AnimatedBetterWord(
                        word = word,
                        positionMs = positionMs
                    )
                }
            }
        } else {
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = if (isActive) 27.sp else 23.sp,
                    lineHeight = 34.sp
                ),
                color = Color.White
            )
        }
    }
}

/** Word-level liquid fill sweep and singing bounce matching BetterLyrics engine. */
@Composable
private fun AnimatedBetterWord(
    word: WordTiming,
    positionMs: Long,
) {
    val wordDuration = (word.endMs - word.startMs).coerceAtLeast(1L)
    val isComplete = positionMs >= word.endMs
    val isActive = positionMs in word.startMs until word.endMs

    val progress = when {
        isComplete -> 1f
        positionMs <= word.startMs -> 0f
        else -> ((positionMs - word.startMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    val sinProgress = sin(progress * PI).toFloat()
    val wordBounce = if (isActive) 1f + (0.04f * sinProgress) else 1f

    val floatOffset by animateFloatAsState(
        targetValue = if (isActive) -3f * sinProgress else 0f,
        animationSpec = tween(durationMillis = if (isActive) 60 else 300, easing = FastOutSlowInEasing),
        label = "wordFloat"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = floatOffset
                scaleX = wordBounce
                scaleY = wordBounce
            }
            .padding(end = 6.dp)
    ) {
        // Base text (semi-transparent)
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 27.sp,
                lineHeight = 34.sp
            ),
            color = Color.White.copy(alpha = 0.35f)
        )

        // Overlay with liquid fill gradient progress
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = if (isComplete) 1f else 0.95f
                    }
            ) {
                Text(
                    text = word.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 27.sp,
                        lineHeight = 34.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
