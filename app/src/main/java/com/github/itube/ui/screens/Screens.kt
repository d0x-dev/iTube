package com.github.itube

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import com.github.itube.ui.components.MaterialLoader
import com.github.itube.ui.components.AnimatedEqualizer
import com.github.itube.ui.components.TrackBlurPopup
import com.github.itube.ui.components.popupContainerColor
import com.github.itube.ui.components.popupScrimColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeoutOrNull

// ── Home / Subscriptions / Library ─────────────────────────────────────────────
// Signed in, these show the account's real feeds. Signed out, they explain what signing
// in brings rather than showing sample content.

@Composable
fun HomeScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onSearch: () -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) { if (signedIn) lib.home.ensureLoaded() }
    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.home.generation,
        header = { ScreenHeader(title = "iTube", brandMark = true) { UpdateAction(); AccountAction(onAccount) } },
    ) { top ->
        if (signedIn) {
            FeedList(lib.home, contentPadding.withTop(top), onOpen, scrollToTop = scrollToTop, indicatorTop = top)
        } else {
            LazyColumn(contentPadding = contentPadding.withTop(top)) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Subscriptions,
                        title = "Your recommendations, minus the ads",
                        body = "Sign in with Google to see your own YouTube home feed, or just search.",
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PillButton("Sign in with Google", onSignIn)
                                Spacer(Modifier.height(12.dp))
                                TextLink("Search instead", onSearch)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) {
        if (signedIn) {
            lib.subscriptions.ensureLoaded()
            if (lib.channels.isEmpty()) lib.loadChannels()
        }
    }
    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.subscriptions.generation,
        header = { ScreenHeader(title = "Subscriptions") { AccountAction(onAccount) } },
    ) { top ->
        if (signedIn) {
            FeedList(lib.subscriptions, contentPadding.withTop(top), onOpen, header = {
                // Always present (empty until loaded) so its arrival never shifts the list.
                item(key = "channels") { if (lib.channels.isNotEmpty()) ChannelRail(lib.channels) }
            }, scrollToTop = scrollToTop, indicatorTop = top)
        } else {
            LazyColumn(contentPadding = contentPadding.withTop(top)) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Subscriptions,
                        title = "Your subscriptions",
                        body = "Sign in with Google to see new videos from the channels you follow.",
                        action = { PillButton("Sign in with Google", onSignIn) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRail(channels: List<ChannelRef>) {
    val c = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
    ) {
        items(channels, key = { it.id }) { ch ->
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ChannelAvatar(ch.avatar, ch.name, size = 56)
                Spacer(Modifier.height(6.dp))
                Text(ch.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun LibraryScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenPlaylist: (PlaylistRef) -> Unit,
    onSignIn: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val signedIn = Account.signedIn
    LaunchedEffect(signedIn) {
        if (signedIn) {
            lib.history.ensureLoaded()
            if (lib.playlists.isEmpty()) lib.loadPlaylists()
        }
    }
    CollapsingHeaderLayout(
        resetKey = scrollToTop to lib.history.generation,
        header = { ScreenHeader(title = "Library") { AccountAction(onAccount) } },
    ) { top ->
        PullToRefresh(
            enabled = signedIn,
            refreshing = lib.history.refreshing,
            onRefresh = { lib.history.refresh(); lib.loadPlaylists() },
            topPadding = top,
        ) {
        LazyColumn(state = rememberScrollToTop(scrollToTop), contentPadding = contentPadding.withTop(top)) {
            if (!signedIn) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.VideoLibrary,
                        title = "Your library",
                        body = "Sign in with Google for your history, Watch later, liked videos and playlists.",
                        action = { PillButton("Sign in with Google", onSignIn) },
                    )
                }
                return@LazyColumn
            }
            item(key = "historyTitle") { SectionTitle("History") }
            item(key = "history") {
                val history = lib.history.videos
                when {
                    history.isNotEmpty() -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(history.take(20), key = { "h-" + it.id }) { video -> HistoryCard(video) { onOpen(video) } }
                    }
                    lib.history.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        MaterialLoader(color = MaterialTheme.colorScheme.primary, size = 28.dp)
                    }
                    else -> Text(
                        lib.history.error ?: "Nothing watched yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item(key = "playlistsTitle") { SectionTitle("Playlists") }
            val pinned = listOf(
                PlaylistRef("WL", "Watch later", null, null),
                PlaylistRef("LL", "Liked videos", null, null),
            )
            items((pinned + lib.playlists).chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
                Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { pl -> PlaylistTile(pl, Modifier.weight(1f)) { onOpenPlaylist(pl) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        }
    }
}

@Composable
private fun HistoryCard(video: VideoItem, onClick: () -> Unit) {
    val menu = LocalVideoMenu.current
    Column(Modifier.width(168.dp).prefetchOnPress(video.id).pressable(pressedScale = 0.97f, onLongClick = { menu(video) }, onClick = onClick)) {
        Box {
            Thumbnail(video.thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f), radius = 20)
            DurationPill(video.durationSec, Modifier.align(Alignment.BottomEnd).padding(6.dp), small = true)
        }
        Spacer(Modifier.height(7.dp))
        Text(video.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistTile(playlist: PlaylistRef, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    val (icon, tone) = when (playlist.id) {
        "WL" -> Icons.Rounded.WatchLater to c.primaryContainer
        "LL" -> Icons.Rounded.ThumbUp to c.tertiaryContainer
        else -> Icons.AutoMirrored.Rounded.PlaylistPlay to c.secondaryContainer
    }
    Column(
        modifier = modifier
            .height(132.dp)
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .clip(shape)
            .background(c.surfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (playlist.thumbnail != null) {
            Thumbnail(playlist.thumbnail, Modifier.size(width = 72.dp, height = 44.dp), radius = 12)
        } else {
            Box(Modifier.size(52.dp).clip(CookieShape).background(tone), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = contentColorFor(tone))
            }
        }
        Column {
            Text(playlist.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            playlist.subtitle?.let { Text(it, color = c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
        }
    }
}

/** A playlist's videos (Watch later, Liked, or the user's own). */
@Composable
fun PlaylistScreen(playlist: PlaylistRef, feed: FeedState, contentPadding: PaddingValues, onBack: () -> Unit, onOpen: (VideoItem) -> Unit) {
    LaunchedEffect(playlist.id) { feed.ensureLoaded() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    CollapsingHeaderLayout(
        resetKey = feed.generation,
        header = {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                playlist.title,
                fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.8).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        },
    ) { top ->
        if (feed.videos.isEmpty() && !feed.loading && feed.error == null && !feed.hasMore) {
            Box(Modifier.padding(top = top)) { EmptyState(Icons.Rounded.VideoLibrary, "Nothing here yet", "Videos you add to this playlist will show up here.") }
        } else {
            FeedList(feed, contentPadding.withTop(top), onOpen, indicatorTop = top)
        }
    }
    }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        modifier = Modifier.pressable(onClick = onClick).padding(8.dp),
    )
}

// ── Search ─────────────────────────────────────────────────────────────────────

enum class SearchCategory {
    Videos,
    Music,
}

/** Lives above the tab switcher so results survive switching tabs. */
@Stable
class SearchState(private val scope: CoroutineScope) {
    var query by mutableStateOf("")
    var category by mutableStateOf(SearchCategory.Videos)

    // Video search state
    var submitted by mutableStateOf("")
        private set
    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    /** All is the default and uses YouTube's real ALL result filter. */
    var filter by mutableStateOf(SearchFilter.All)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var generation by mutableStateOf(0)
        private set
    private var job: Job? = null

    // Music search state
    var musicSubmitted by mutableStateOf("")
        private set
    var musicResults by mutableStateOf<List<MusicTrack>>(emptyList())
        private set
    var musicLoading by mutableStateOf(false)
        private set
    var musicError by mutableStateOf<String?>(null)
        private set
    var musicGeneration by mutableStateOf(0)
        private set
    private var musicJob: Job? = null
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    private var suggestionsJob: Job? = null

    fun updateQuery(value: String) {
        query = value
        suggestionsJob?.cancel()
        if (value.trim().length < 2) {
            suggestions = emptyList()
            return
        }
        suggestionsJob = scope.launch {
            kotlinx.coroutines.delay(180)
            suggestions = runCatching { YouTube.suggestions(value.trim()) }.getOrDefault(emptyList())
        }
    }

    fun selectCategory(newCategory: SearchCategory) {
        if (category == newCategory) return
        category = newCategory
        val activeQuery = query.trim().ifEmpty {
            if (newCategory == SearchCategory.Videos) musicSubmitted else submitted
        }
        if (activeQuery.isNotEmpty()) {
            query = activeQuery
            submit()
        }
    }

    /** Selecting a row in the search filter sheet immediately refreshes the active query. */
    fun selectFilter(newFilter: SearchFilter) {
        if (filter == newFilter) return
        filter = newFilter
        if (category == SearchCategory.Videos && query.trim().isNotEmpty()) submit()
    }

    fun submit() {
        val q = query.trim()
        if (q.isEmpty()) return
        SearchHistory.add(q)
        if (category == SearchCategory.Videos) {
            submitted = q
            loading = true
            error = null
            job?.cancel()
            job = scope.launch {
                runCatching { YouTube.search(q, filter) }
                    .onSuccess { results = it; generation++ }
                    .onFailure { error = YouTube.friendlyError(it) }
                loading = false
            }
        } else {
            musicSubmitted = q
            musicLoading = true
            musicError = null
            musicJob?.cancel()
            musicJob = scope.launch {
                runCatching { MusicBackend.searchMusic(q) }
                    .onSuccess { musicResults = it; musicGeneration++ }
                    .onFailure { musicError = it.localizedMessage ?: "Failed to search music" }
                musicLoading = false
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: SearchState,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    onSettings: () -> Unit,
    scrollToTop: Int = 0,
) {
    val c = MaterialTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val view = LocalView.current
    // Search and Enter both dismiss the keyboard — every field, every time. A hide issued
    // while the key event is still being dispatched can be undone by the IME, so the
    // window-level hide is posted to run after it.
    fun submit() {
        focus.clearFocus(force = true)
        keyboard?.hide()
        view.postDelayed({
            view.context.getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }, 120)
        state.submit()
    }
    var focused by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    // The keyboard closed by Back or its own button leaves the field focused; drop the focus
    // with it so the history gives way to the results again.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) { if (!imeVisible && focused) focus.clearFocus() }

    val currentSubmitted = if (state.category == SearchCategory.Videos) state.submitted else state.musicSubmitted
    // Past searches, filtered by what's typed: while typing, and on the empty page.
    val history = SearchHistory.matching(state.query)
    val showHistory = history.isNotEmpty() && (focused || currentSubmitted.isEmpty())
    val showSuggestions = focused && state.query.trim().length >= 2 && state.suggestions.isNotEmpty()

    // The search bar rides with the header: both slide away together while scrolling results.
    CollapsingHeaderLayout(
        resetKey = listOf(scrollToTop, state.generation, state.musicGeneration, state.category, showHistory),
        header = { Column {
        // This remains a Search page; filters are separate from application settings.
        ScreenHeader(
            title = "Search",
        ) {
            if (state.category == SearchCategory.Videos) {
                CircleAction(Icons.Rounded.Tune, "Search filters") { filtersOpen = true }
            }
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(60.dp)
                .clip(CircleShape)
                .background(c.surfaceContainerHigh)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = c.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        if (state.category == SearchCategory.Videos) "Search iTube" else "Search iMusic",
                        color = c.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = state::updateQuery,
                    singleLine = true,
                    textStyle = OwnDirectionText.merge(TextStyle(color = c.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }, onDone = { submit() }),
                    // Hardware Enter doesn't go through the IME action; treat it the same.
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.onPreviewKeyEvent { e ->
                        if (e.key == Key.Enter || e.key == Key.NumPadEnter) {
                            if (e.type == KeyEventType.KeyDown) submit()
                            true
                        } else false
                    },
                )
            }
            if (state.query.isNotEmpty()) {
                Box(
                    modifier = Modifier.size(44.dp).pressable { state.updateQuery("") },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Clear", tint = c.onSurfaceVariant) }
            }
        }

        // Switch the source without losing the typed query.
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isVideos = state.category == SearchCategory.Videos
            val videosBg by animateColorAsState(
                targetValue = if (isVideos) c.primary else c.surfaceContainerHigh,
                animationSpec = tween(200),
                label = "videosBg"
            )
            val videosFg by animateColorAsState(
                targetValue = if (isVideos) c.onPrimary else c.onSurfaceVariant,
                animationSpec = tween(200),
                label = "videosFg"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(videosBg)
                    .clickable { state.selectCategory(SearchCategory.Videos) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = videosFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "iTube",
                        color = videosFg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val isMusic = state.category == SearchCategory.Music
            val musicBg by animateColorAsState(
                targetValue = if (isMusic) c.primary else c.surfaceContainerHigh,
                animationSpec = tween(200),
                label = "musicBg"
            )
            val musicFg by animateColorAsState(
                targetValue = if (isMusic) c.onPrimary else c.onSurfaceVariant,
                animationSpec = tween(200),
                label = "musicFg"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(musicBg)
                    .clickable { state.selectCategory(SearchCategory.Music) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = musicFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "iMusic",
                        color = musicFg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        } },
    ) { top ->
        Box(Modifier.fillMaxSize()) {
            if (state.category == SearchCategory.Videos) {
                val listShown = showHistory || (state.results.isNotEmpty() && state.error == null)
                Box(Modifier.padding(top = if (listShown) 0.dp else top)) {
                    when {
                        showSuggestions -> SearchSuggestionsList(
                            suggestions = state.suggestions,
                            contentPadding = contentPadding.withTop(top),
                            onSearch = { state.updateQuery(it); submit() },
                        )
                        showHistory -> SearchHistoryList(
                            history = history,
                            contentPadding = contentPadding.withTop(top),
                            onSearch = { state.query = it; submit() },
                        )
                        state.loading && state.results.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                            MaterialLoader(color = c.primary, size = 36.dp)
                        }
                        state.error != null -> EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "Search didn't load",
                            body = state.error.orEmpty(),
                            action = { PillButton("Try again", { state.submit() }) },
                        )
                        state.submitted.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.TravelExplore,
                            title = "Search without the ads",
                            body = "Results play straight away, with no ads and no sign-in.",
                        )
                        state.results.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.History,
                            title = "Nothing found",
                            body = "No videos matched “${state.submitted}”.",
                        )
                        else -> {
                            val list = rememberScrollToTop(scrollToTop)
                            LaunchedEffect(state.generation) { list.scrollToItem(0) }
                            LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                                items(state.results, key = { it.id }) { result ->
                                    SearchResultCard(result, onOpen, onOpenResult)
                                }
                            }
                        }
                    }
                }
            } else {
                // Music Search Results (AirBeats style)
                val listShown = showHistory || (state.musicResults.isNotEmpty() && state.musicError == null)
                Box(Modifier.padding(top = if (listShown) 0.dp else top)) {
                    when {
                        showHistory -> SearchHistoryList(
                            history = history,
                            contentPadding = contentPadding.withTop(top),
                            onSearch = { state.query = it; submit() },
                        )
                        state.musicLoading && state.musicResults.isEmpty() -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                            MaterialLoader(color = c.primary, size = 36.dp)
                        }
                        state.musicError != null -> EmptyState(
                            icon = Icons.Rounded.CloudOff,
                            title = "Music search didn't load",
                            body = state.musicError.orEmpty(),
                            action = { PillButton("Try again", { state.submit() }) },
                        )
                        state.musicSubmitted.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.MusicNote,
                            title = "Search YouTube Music",
                            body = "Stream songs, albums, and artists with synchronized lyrics and lossless artwork.",
                        )
                        state.musicResults.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = "No songs found",
                            body = "No music tracks matched “${state.musicSubmitted}”.",
                        )
                        else -> {
                            val list = rememberScrollToTop(scrollToTop)
                            LaunchedEffect(state.musicGeneration) { list.scrollToItem(0) }
                            LazyColumn(state = list, contentPadding = contentPadding.withTop(top)) {
                                items(state.musicResults, key = { it.id }) { track ->
                                    MusicSearchTrackRow(
                                        track = track,
                                        onClick = {
                                            MusicPlayerState.play(track, state.musicResults)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (filtersOpen) SearchFiltersSheet(
        selected = state.filter,
        onSelected = { filter ->
            state.selectFilter(filter)
            filtersOpen = false
        },
        onDismiss = { filtersOpen = false },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SearchFiltersSheet(
    selected: SearchFilter,
    onSelected: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackBlurPopup()
    val c = MaterialTheme.colorScheme
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = popupContainerColor(c.surfaceContainerLow),
        scrimColor = popupScrimColor(),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Search filters", fontFamily = RoundedHeavy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("Choose what to search for", color = c.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            SearchFilter.entries.forEach { filter ->
                val active = selected == filter
                Row(
                    Modifier.fillMaxWidth().pressable { onSelected(filter) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(filter.label, modifier = Modifier.weight(1f), fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 16.sp)
                    if (active) Icon(Icons.Rounded.Check, "Selected", tint = c.primary)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, onOpen: (VideoItem) -> Unit, onOpenResult: (SearchResult) -> Unit) {
    result.video?.let { video ->
        VideoCard(video, onClick = { onOpen(video) })
        return
    }
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(20.dp)).background(c.surfaceContainerLow).pressable { onOpenResult(result) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(result.thumbnail, Modifier.size(68.dp), radius = 16)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(result.subtitle, color = c.onSurfaceVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchSuggestionsList(suggestions: List<String>, contentPadding: PaddingValues, onSearch: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    LazyColumn(contentPadding = contentPadding) {
        items(suggestions, key = { it }) { suggestion ->
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).pressable { onSearch(suggestion) }.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.onSurfaceVariant, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(16.dp))
                Text(suggestion, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** AirBeats-styled music search track item row with cover art, titles, badges, and quick play. */
@Composable
fun MusicSearchTrackRow(
    track: MusicTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = MusicPlayerState.currentTrack?.id == track.id
    val isPlaying = isCurrent && MusicPlayerState.isPlaying
    val c = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with rounded corners and playing indicator
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = track.thumbnailUrl?.highQualityThumbnail(),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizer(
                        isPlaying = isPlaying,
                        color = Color.White,
                        size = 24.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = if (isCurrent) c.primary else c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AirBeats style "Music" badge
                Box(
                    modifier = Modifier
                        .background(
                            color = c.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.sp
                        ),
                        color = c.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = track.artists + if (track.durationSec > 0) " • ${formatMusicDuration(track.durationSec)}" else "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play/Pause Action Icon
        Surface(
            shape = CircleShape,
            color = if (isCurrent) c.primary.copy(alpha = 0.15f) else c.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isCurrent) c.primary else c.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Music discovery feed: real YouTube Music shelves with a bounded catalog-search fallback. */
@Stable
class MusicHomeState(private val scope: CoroutineScope) {
    data class Mix(val title: String, val query: String, val tracks: List<VideoItem> = emptyList())
    private val fallbackMixes = listOf(
        Mix("Quick picks", "popular songs"),
        Mix("New releases", "new songs 2026"),
        Mix("Trending", "trending music"),
        Mix("Made for you", "top hits playlist"),
        Mix("Indian hits", "latest Indian hit songs"),
        Mix("Chill", "chill relaxing music"),
        Mix("Workout", "workout music hits"),
        Mix("Focus", "instrumental focus music"),
    )
    var mixes by mutableStateOf(fallbackMixes)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var started = false
    private var loadJob: Job? = null

    fun load(force: Boolean = false) {
        if (started && !force) return
        started = true
        loadJob?.cancel()
        loadJob = scope.launch {
            loading = true
            error = null

            // If YouTube changes or blocks the home endpoint, keep the page usable with
            // independent catalog searches; one slow request cannot keep the whole screen spinning.
            val requested = fallbackMixes
            mixes = requested
            // Sending every query at the same time makes YouTube reject the extractor as bot
            // traffic. Load each shelf in order and reveal it immediately.
            requested.forEachIndexed { index, mix ->
                val tracks = withTimeoutOrNull(15_000) {
                    runCatching { YouTube.search(mix.query) }.getOrNull()
                }.orEmpty()
                if (tracks.isNotEmpty()) {
                    mixes = mixes.toMutableList().also { it[index] = mix.copy(tracks = tracks) }
                } else if (error == null) {
                    error = "Some music shelves could not be loaded."
                }
            }
            loading = false
            if (mixes.none { it.tracks.isNotEmpty() } && error == null) error = "No music loaded. Check your connection and try again."
        }
    }
}
@Composable
fun MusicHomeScreen(
    state: MusicHomeState,
    contentPadding: PaddingValues,
    onSearch: () -> Unit,
    onPlay: (List<VideoItem>, Int) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    LaunchedEffect(state) { state.load() }
    val featured = state.mixes.firstNotNullOfOrNull { it.tracks.firstOrNull() }
    LazyColumn(contentPadding = contentPadding) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("iTube Music", color = c.onSurface, fontSize = 29.sp, fontWeight = FontWeight.Black)
                    Text("Your music, ready to play", color = c.onSurfaceVariant, fontSize = 14.sp)
                }
                Icon(Icons.Rounded.Equalizer, "Music", tint = c.primary, modifier = Modifier.size(30.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(CircleShape)
                    .background(c.surfaceContainerHigh).pressable(onClick = onSearch)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Search songs, artists, albums", color = c.onSurfaceVariant, fontSize = 16.sp)
            }
            Spacer(Modifier.height(18.dp))
            if (featured != null) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(270.dp)
                        .clip(RoundedCornerShape(26.dp)).pressable {
                            val mix = state.mixes.firstOrNull { it.tracks.any { song -> song.id == featured.id } }
                            val list = mix?.tracks.orEmpty()
                            onPlay(list, list.indexOfFirst { it.id == featured.id }.coerceAtLeast(0))
                        },
                ) {
                    Thumbnail(featured.thumbnail, Modifier.fillMaxSize(), radius = 0)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
                        Text("FEATURED SONG", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Text(featured.title, Modifier.padding(top = 5.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(featured.channel, Modifier.weight(1f), color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.clip(CircleShape).background(c.primary).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
                                Text("Play", Modifier.padding(start = 5.dp), color = c.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (state.loading && state.mixes.none { it.tracks.isNotEmpty() }) {
            item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { MaterialLoader(color = c.primary, size = 32.dp) } }
        }
        state.error?.let { message ->
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), color = c.onSurfaceVariant, fontSize = 13.sp)
                    TextLink("Retry") { state.load(force = true) }
                }
            }
        }
        state.mixes.filter { it.tracks.isNotEmpty() }.forEach { mix ->
            item(key = "music-section-${mix.query}") {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mix.title, color = c.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("From YouTube Music", color = c.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Icon(Icons.Rounded.PlayArrow, "Play ${mix.title}", tint = c.primary, modifier = Modifier.size(28.dp).pressable { onPlay(mix.tracks, 0) })
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(mix.tracks.take(16), key = { "${mix.query}-${it.id}" }) { track ->
                        Column(Modifier.width(158.dp).pressable {
                            onPlay(mix.tracks, mix.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
                        }) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))) {
                                Thumbnail(track.thumbnail, Modifier.fillMaxSize(), radius = 0)
                                Box(Modifier.align(Alignment.BottomEnd).padding(9.dp).size(38.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PlayArrow, "Play ${track.title}", tint = c.onPrimary)
                                }
                            }
                            Text(track.title, Modifier.padding(top = 9.dp), color = c.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(track.channel, color = c.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().pressable(onClick = onSearch).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Album, null, tint = c.primary)
                Spacer(Modifier.width(12.dp))
                Text("Explore more songs and artists", color = c.onSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.QueueMusic, null, tint = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchHistoryList(history: List<String>, contentPadding: PaddingValues, onSearch: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    LazyColumn(contentPadding = contentPadding) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            ) {
                SectionTitle("Recent searches", Modifier.weight(1f).alignByBaseline())
                Box(Modifier.alignByBaseline()) { TextLink("Clear all") { SearchHistory.clear() } }
            }
        }
        items(history, key = { it }) { query ->
            Row(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .pressable { onSearch(query) }
                    .height(52.dp)
                    .padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.History, null, tint = c.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    query,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier.size(44.dp).pressable { SearchHistory.remove(query) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Remove", tint = c.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
