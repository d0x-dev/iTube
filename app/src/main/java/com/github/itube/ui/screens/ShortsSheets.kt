package com.github.itube

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.github.itube.ui.components.TrackBlurPopup
import com.github.itube.ui.components.popupContainerColor
import com.github.itube.ui.components.popupScrimColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

enum class ShortsSheetType {
    None,
    Menu,
    Description,
    Quality,
    Download,
    AudioTrack,
    Speed
}

data class ShortsAudioTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val bitrate: Int,
    val sampleRate: Int,
    val channelCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsOptionsModal(
    sheetType: ShortsSheetType,
    shorts: ShortsState,
    onDismiss: () -> Unit,
    onNavigate: (ShortsSheetType) -> Unit,
) {
    if (sheetType == ShortsSheetType.None) return

    val currentItem = shorts.current ?: return

    if (sheetType == ShortsSheetType.Download) {
        DownloadSheet(video = currentItem, onDismiss = onDismiss)
        return
    }

    TrackBlurPopup()
    val c = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = popupContainerColor(c.surfaceContainerLow),
        scrimColor = popupScrimColor(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        when (sheetType) {
            ShortsSheetType.Menu -> {
                ShortsMainMenu(
                    shorts = shorts,
                    item = currentItem,
                    onNavigate = onNavigate,
                )
            }
            ShortsSheetType.Description -> {
                ShortsDescriptionView(
                    shorts = shorts,
                    item = currentItem,
                    onBack = { onNavigate(ShortsSheetType.Menu) },
                    onClose = onDismiss,
                )
            }
            ShortsSheetType.Quality -> {
                ShortsQualityView(
                    shorts = shorts,
                    onBack = { onNavigate(ShortsSheetType.Menu) },
                    onClose = onDismiss,
                )
            }
            ShortsSheetType.AudioTrack -> {
                ShortsAudioTrackView(
                    shorts = shorts,
                    onBack = { onNavigate(ShortsSheetType.Menu) },
                    onClose = onDismiss,
                )
            }
            ShortsSheetType.Speed -> {
                ShortsSpeedView(
                    shorts = shorts,
                    onBack = { onNavigate(ShortsSheetType.Menu) },
                    onClose = onDismiss,
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun ShortsMainMenu(
    shorts: ShortsState,
    item: VideoItem,
    onNavigate: (ShortsSheetType) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val currentQuality = QualityPreference.height?.let { "${it}p" } ?: "Auto"
    val currentSpeedLabel = if (shorts.speed == 1f) "1.0x (Normal)" else "${shorts.speed}x"
    val audioTracks = remember(shorts.index) { shorts.getAudioTracks() }
    val currentAudio = audioTracks.firstOrNull { it.isSelected }?.name ?: "Original Audio"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp)
    ) {
        // Video Preview Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = c.outlineVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(10.dp))

        // Options List
        MenuOptionRow(
            icon = Icons.AutoMirrored.Rounded.Notes,
            title = "Description",
            subtitle = "Video details, stats & hashtags",
            onClick = { onNavigate(ShortsSheetType.Description) }
        )
        MenuOptionRow(
            icon = Icons.Rounded.HighQuality,
            title = "Quality",
            subtitle = currentQuality,
            onClick = { onNavigate(ShortsSheetType.Quality) }
        )
        MenuOptionRow(
            icon = Icons.Rounded.Download,
            title = "Download",
            subtitle = "Save video or audio offline",
            onClick = { onNavigate(ShortsSheetType.Download) }
        )
        MenuOptionRow(
            icon = Icons.Rounded.Headphones,
            title = "Audio Track",
            subtitle = currentAudio,
            onClick = { onNavigate(ShortsSheetType.AudioTrack) }
        )
        MenuOptionRow(
            icon = Icons.Rounded.Speed,
            title = "Playback speed",
            subtitle = currentSpeedLabel,
            onClick = { onNavigate(ShortsSheetType.Speed) }
        )
    }
}

@Composable
private fun MenuOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = c.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortsDescriptionView(
    shorts: ShortsState,
    item: VideoItem,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    var details by remember(item.id) { mutableStateOf(shorts.details[item.id]) }
    var loadingDetails by remember(item.id) { mutableStateOf(details == null) }

    LaunchedEffect(item.id) {
        if (details == null) {
            loadingDetails = true
            val d = withContext(Dispatchers.IO) {
                runCatching { YouTube.details(item.id, wait = false) }.getOrNull()
            }
            if (d != null) {
                shorts.details[item.id] = d
                details = d
            }
            loadingDetails = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 36.dp)
    ) {
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
            }
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Full Title
        SelectionContainer {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Channel Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c.surfaceContainerHigh)
            ) {
                if (!item.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = item.avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.channel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.onSurface
                )
                details?.subscribers?.takeIf { it > 0 }?.let { subs ->
                    Text(
                        text = "${formatCount(subs)} subscribers",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Stats Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(
                icon = Icons.Rounded.Visibility,
                text = if (item.views >= 0) "${formatCount(item.views)} views" else "Short",
                modifier = Modifier.weight(1f)
            )
            StatPill(
                icon = Icons.Rounded.ThumbUp,
                text = (details?.likes?.takeIf { it > 0 }?.let { formatCount(it) }) ?: "Likes",
                modifier = Modifier.weight(1f)
            )
            StatPill(
                icon = Icons.Rounded.CalendarToday,
                text = item.uploaded ?: "Recent",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = c.outlineVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(14.dp))

        // Description Body
        val fullDescription = details?.description.orEmpty()
        if (fullDescription.isBlank() && !loadingDetails) {
            Text(
                text = "No description provided for this Short.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onSurfaceVariant
            )
        } else {
            val annotated = remember(fullDescription) { formatDescription(fullDescription, c.primary) }
            SelectionContainer {
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurface,
                    lineHeight = 22.sp
                )
            }
        }

        // Hashtags
        val tags = remember(fullDescription, item.title) {
            extractHashtags(fullDescription + " " + item.title)
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.surfaceContainerHigh)
                            .clickable {
                                runCatching {
                                    uriHandler.openUri("https://www.youtube.com/hashtag/${tag.removePrefix("#")}")
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = c.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShortsQualityView(
    shorts: ShortsState,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val chosenHeight = QualityPreference.height
    val qualities = listOf(
        null to ("Auto" to "Best resolution available"),
        1080 to ("1080p" to "Full HD"),
        720 to ("720p" to "High Definition"),
        480 to ("480p" to "Standard Definition"),
        360 to ("360p" to "Data Saver"),
        240 to ("240p" to "Low bandwidth"),
        144 to ("144p" to "Ultra low data")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
            }
            Text(
                text = "Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Select video quality for Shorts",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.height(12.dp))

        qualities.forEach { (h, labels) ->
            val isSelected = chosenHeight == h
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        shorts.setQuality(h)
                        onClose()
                    }
                    .background(if (isSelected) c.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = labels.first,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) c.onPrimaryContainer else c.onSurface
                    )
                    Text(
                        text = labels.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) c.onPrimaryContainer.copy(alpha = 0.8f) else c.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = c.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortsAudioTrackView(
    shorts: ShortsState,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val tracks = remember(shorts.index) { shorts.getAudioTracks() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
            }
            Text(
                text = "Audio Track",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Select active audio track or language",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.height(14.dp))

        if (tracks.isEmpty()) {
            // Default single track fallback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Original Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.onPrimaryContainer
                    )
                    Text(
                        text = "Default audio stream · Stereo · AAC",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Active",
                    tint = c.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            tracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            shorts.selectAudioTrack(track)
                            onClose()
                        }
                        .background(if (track.isSelected) c.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (track.isSelected) c.onPrimaryContainer else c.onSurface
                        )
                        val extra = buildList {
                            if (track.bitrate > 0) add("${track.bitrate / 1000} kbps")
                            if (track.channelCount > 0) add(if (track.channelCount == 2) "Stereo" else "${track.channelCount} ch")
                            if (track.sampleRate > 0) add("${track.sampleRate / 1000} kHz")
                        }.joinToString(" · ")
                        if (extra.isNotBlank()) {
                            Text(
                                text = extra,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (track.isSelected) c.onPrimaryContainer.copy(alpha = 0.8f) else c.onSurfaceVariant
                            )
                        }
                    }
                    if (track.isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = c.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortsSpeedView(
    shorts: ShortsState,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.onSurface)
            }
            Text(
                text = "Playback speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Adjust playback rate for Shorts",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.height(14.dp))

        speeds.forEach { speedVal ->
            val isSelected = shorts.speed == speedVal
            val label = if (speedVal == 1.0f) "1.0x (Normal)" else "${speedVal}x"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        shorts.applySpeed(speedVal)
                        onClose()
                    }
                    .background(if (isSelected) c.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) c.onPrimaryContainer else c.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = c.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatDescription(text: String, accentColor: Color) = buildAnnotatedString {
    append(text)
    val urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
    val matcher = urlPattern.matcher(text)
    while (matcher.find()) {
        addStyle(
            SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
            matcher.start(),
            matcher.end()
        )
    }
}

private fun extractHashtags(text: String): List<String> {
    val regex = Regex("#[\\p{L}\\p{N}_]+")
    return regex.findAll(text).map { it.value }.distinct().take(12).toList()
}
