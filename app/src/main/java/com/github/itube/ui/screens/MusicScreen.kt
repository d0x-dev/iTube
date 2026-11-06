package com.github.itube.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.github.itube.ui.components.ModernPullToRefreshBox
import com.github.itube.ui.components.MusicScreenSkeleton
import com.github.itube.AccountAction
import com.github.itube.CircleAction
import com.github.itube.MusicBackend
import com.github.itube.MusicHomeFeed
import com.github.itube.MusicPlayerState
import com.github.itube.MusicSection
import com.github.itube.MusicTrack
import com.github.itube.ScreenHeader
import com.github.itube.UpdateAction
import com.github.itube.highQualityThumbnail
import kotlinx.coroutines.launch

/**
 * iMusic Home Screen.
 * Displays matching UI header with tag "iMusic" ("i" in red),
 * YouTube Music recommendation shelves, quick picks, and mood/genre chips.
 */
@Composable
fun MusicScreen(
    contentPadding: PaddingValues,
    onOpenMusic: (MusicTrack, List<MusicTrack>?) -> Unit,
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var feed by remember { mutableStateOf<MusicHomeFeed?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedChip by remember { mutableStateOf<String?>(null) }

    fun loadData(chip: String? = null) {
        scope.launch {
            isLoading = true
            feed = MusicBackend.fetchMusicHome(chip)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Header with tag "iMusic" ("i" in red) ──
        ScreenHeader(title = "iMusic", brandMark = true) {
            UpdateAction()
            CircleAction(Icons.Rounded.Search, "Search", onSearch)
            AccountAction(onAccount)
        }

        ModernPullToRefreshBox(
            isRefreshing = isLoading && feed != null,
            onRefresh = { loadData(selectedChip) },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && feed == null) {
                MusicScreenSkeleton(modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding() + 24.dp))
            } else {
            val currentFeed = feed ?: MusicHomeFeed()

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── 1. Mood & Activity Chips ──
                if (currentFeed.chips.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentFeed.chips) { chip ->
                                val selected = chip == selectedChip
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedChip = if (selected) null else chip
                                        loadData(selectedChip)
                                    },
                                    label = {
                                        Text(
                                            text = chip,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF0000),
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = null
                                )
                            }
                        }
                    }
                }

                // ── 2. Quick Picks Section ──
                if (currentFeed.quickPicks.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "START RADIO FROM A SONG",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Quick Picks",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Grid of Quick Picks (2 rows horizontally scrolling)
                            val chunked = currentFeed.quickPicks.chunked(4)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(chunked) { columnTracks ->
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.width(310.dp)
                                    ) {
                                        columnTracks.forEach { track ->
                                            QuickPickRow(
                                                track = track,
                                                onClick = {
                                                    onOpenMusic(track, null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 3. Recommendation Shelves & Carousels ──
                itemsIndexed(currentFeed.sections) { _, section ->
                    MusicSectionCarousel(
                        section = section,
                        onTrackClick = { track ->
                            onOpenMusic(track, null)
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun QuickPickRow(
    track: MusicTrack,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp)
            ) {
                AsyncImage(
                    model = track.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artists,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicSectionCarousel(
    section: MusicSection,
    onTrackClick: (MusicTrack) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (!section.label.isNullOrBlank()) {
                    Text(
                        text = section.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(section.items) { track ->
                MusicTrackCard(
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun MusicTrackCard(
    track: MusicTrack,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(148.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = track.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Small floating play pill in lower right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artists,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
