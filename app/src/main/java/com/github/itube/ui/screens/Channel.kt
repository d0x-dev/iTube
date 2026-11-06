package com.github.itube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.NotificationsActive
import com.github.itube.ui.components.MaterialLoader
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

/** Opens a channel page from anywhere (player channel row, card avatars). Provided by the shell. */
val LocalChannelOpener = staticCompositionLocalOf<(String) -> Unit> { {} }

@Stable
class ChannelState(val url: String, private val scope: CoroutineScope) {
    var details by mutableStateOf<ChannelDetails?>(null)
        private set
    var videos by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var shorts by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var releases by mutableStateOf<List<PublicCollection>>(emptyList())
        private set
    var playlists by mutableStateOf<List<PublicCollection>>(emptyList())
        private set
    var sectionsLoading by mutableStateOf(false)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var subscribed by mutableStateOf<Boolean?>(null)
        private set
    private var next: String? = null
    val hasMore get() = next != null

    init { load() }

    fun load() {
        loading = true
        error = null
        scope.launch {
            runCatching { YouTube.channel(url) }
                .onSuccess { d ->
                    details = d
                    if (Account.signedIn) launch { subscribed = runCatching { InnerTube.channelSubscribed(d.id, d.name, d.avatar) }.getOrNull() }
                    runCatching { YouTube.channelVideos(d, null) }
                        .onSuccess { p -> videos = p.videos; next = p.next }
                        .onFailure { error = YouTube.friendlyError(it) }
                    sectionsLoading = true
                    launch {
                        runCatching { YouTube.channelShorts(d) }.onSuccess { shorts = it }
                        runCatching { YouTube.channelCollections(d, albums = true) }.onSuccess { releases = it }
                        runCatching { YouTube.channelCollections(d, albums = false) }.onSuccess { playlists = it }
                        sectionsLoading = false
                    }
                }
                .onFailure { error = YouTube.friendlyError(it) }
            loading = false
        }
    }

    fun loadMore() {
        val d = details ?: return
        val page = next ?: return
        if (loading) return
        loading = true
        scope.launch {
            runCatching { YouTube.channelVideos(d, page) }
                .onSuccess { p ->
                    val seen = videos.mapTo(HashSet()) { it.id }
                    videos = videos + p.videos.filter { seen.add(it.id) }
                    next = p.next
                }
            loading = false
        }
    }

    fun toggleSubscribe(onError: (String) -> Unit) {
        val id = details?.id ?: return
        val before = subscribed ?: return
        subscribed = !before
        scope.launch {
            runCatching { InnerTube.setSubscribed(id, !before) }
                .onSuccess { if (before) Bells.forget(id) }
                .onFailure { subscribed = before; onError("Couldn't update your subscription.") }
        }
    }
}

@Composable
fun ChannelScreen(
    state: ChannelState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenCollection: (PublicCollection) -> Unit,
    onMessage: (String) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    var section by remember(state.url) { mutableStateOf(ChannelSection.Videos) }
    val nearEnd by remember {
        derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 4 }
    }
    LaunchedEffect(nearEnd, state.videos.size) { if (nearEnd && state.videos.isNotEmpty()) state.loadMore() }
    Box(Modifier.fillMaxSize().background(c.surface)) {
    CollapsingHeaderLayout(
        header = {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                state.details?.name.orEmpty(),
                fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.6).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        },
    ) { top ->
        val d = state.details
        when {
            d == null && state.error != null -> Box(Modifier.padding(top = top)) { EmptyState(
                Icons.Rounded.CloudOff, "Couldn't load this channel", state.error.orEmpty(),
                action = { PillButton("Try again", state::load) },
            ) }
            d == null -> Box(Modifier.fillMaxWidth().padding(top = top + 60.dp), Alignment.Center) {
                MaterialLoader(color = c.primary, size = 36.dp)
            }
            else -> LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                item(key = "header") {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (d.banner != null) {
                            Thumbnail(d.banner, Modifier.fillMaxWidth().aspectRatio(5.6f), radius = 22)
                            Spacer(Modifier.height(16.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelAvatar(d.avatar, d.name, size = 76)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name, fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (d.subscribers > 0) Text("${formatCount(d.subscribers)} subscribers", color = c.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        state.subscribed?.let { subscribed ->
                            Spacer(Modifier.height(14.dp))
                            SubscribeWithBell(
                                subscribed,
                                channel = BellChannel(d.id, d.name, d.avatar),
                                onSubscribe = { state.toggleSubscribe(onMessage) },
                                onMessage = onMessage,
                                modifier = Modifier.fillMaxWidth(),
                                wide = true,
                            )
                        }
                        if (d.description.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(d.description, color = c.onSurfaceVariant, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                item(key = "tabs") {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        ChannelSection.entries.forEach { tab ->
                            val active = section == tab
                            Text(
                                tab.label,
                                color = if (active) c.onPrimary else c.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(if (active) c.primary else c.surfaceContainerHigh)
                                    .pressable { section = tab }.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
                when (section) {
                    ChannelSection.Videos -> {
                        item(key = "videosTitle") { SectionTitle("Videos") }
                        items(state.videos, key = { it.id }) { video -> VideoCard(video, onClick = { onOpen(video) }) }
                    }
                    ChannelSection.Shorts -> {
                        item(key = "shortsTitle") { SectionTitle("Shorts") }
                        items(state.shorts, key = { it.id }) { video -> VideoCard(video, onClick = { onOpen(video) }) }
                    }
                    ChannelSection.Releases -> {
                        item(key = "releasesTitle") { SectionTitle("Releases") }
                        items(state.releases, key = { it.url }) { collection -> PublicCollectionRow(collection, onOpenCollection) }
                    }
                    ChannelSection.Playlists -> {
                        item(key = "playlistsTitle") { SectionTitle("Playlists") }
                        items(state.playlists, key = { it.url }) { collection -> PublicCollectionRow(collection, onOpenCollection) }
                    }
                }
                if (section == ChannelSection.Videos && (state.loading || state.hasMore)) item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) {
                        MaterialLoader(color = c.primary, size = 32.dp)
                    }
                }
                if (section != ChannelSection.Videos && state.sectionsLoading) item {
                    Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) { MaterialLoader(color = c.primary, size = 28.dp) }
                }
            }
        }
    }
    }
}

private enum class ChannelSection(val label: String) { Videos("Videos"), Shorts("Shorts"), Releases("Releases"), Playlists("Playlists") }

@Composable
fun PublicCollectionRow(collection: PublicCollection, onOpen: (PublicCollection) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).clip(CircleShape)
            .background(c.surfaceContainerLow).pressable { onOpen(collection) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(collection.thumbnail, Modifier.size(64.dp), radius = 14)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(collection.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(collection.subtitle, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Stable
class PublicCollectionState(val collection: PublicCollection, private val scope: CoroutineScope) {
    var details by mutableStateOf<PublicCollectionDetails?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { load() }
    fun load() {
        loading = true; error = null
        scope.launch {
            runCatching { YouTube.publicCollection(collection.url, collection.isAlbum) }
                .onSuccess { details = it }
                .onFailure { error = YouTube.friendlyError(it) }
            loading = false
        }
    }
}

@Composable
fun PublicCollectionScreen(state: PublicCollectionState, contentPadding: PaddingValues, onBack: () -> Unit, onOpen: (VideoItem) -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(c.surface)) {
        CollapsingHeaderLayout(header = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(12.dp))
                Text(state.collection.title, fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }) { top ->
            when {
                state.loading && state.details == null -> Box(Modifier.fillMaxWidth().padding(top = top + 64.dp), Alignment.Center) { MaterialLoader(c.primary, 36.dp) }
                state.error != null -> Box(Modifier.padding(top = top)) { EmptyState(Icons.Rounded.CloudOff, "Couldn't load this ${if (state.collection.isAlbum) "album" else "playlist"}", state.error.orEmpty(), action = { PillButton("Try again", state::load) }) }
                else -> {
                    val details = state.details ?: return@CollapsingHeaderLayout
                    LazyColumn(contentPadding = contentPadding.withTop(top)) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Thumbnail(details.collection.thumbnail, Modifier.fillMaxWidth().aspectRatio(1f), radius = 24)
                                Spacer(Modifier.height(14.dp))
                                Text(details.collection.title, fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                                Text(details.collection.subtitle, color = c.onSurfaceVariant, fontSize = 14.sp)
                                details.description.takeIf { it.isNotBlank() }?.let { Text(it, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)) }
                            }
                        }
                        items(details.videos, key = { it.id }) { VideoCard(it, onClick = { onOpen(it) }) }
                    }
                }
            }
        }
    }
}
