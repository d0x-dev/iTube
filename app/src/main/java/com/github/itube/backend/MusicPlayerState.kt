package com.github.itube

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.github.itube.backend.lyrics.LyricsHelper
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dedicated music playback controller and state machine.
 * Completely independent from the video player.
 */
object MusicPlayerState : Player.Listener {
    private const val TAG = "MusicPlayerState"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var exoPlayer: ExoPlayer? = null
    private var appContext: Context? = null
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var loadJob: Job? = null
    /** True while a new URL is being extracted, before ExoPlayer has a buffering state. */
    private var resolvingStream = false

    // ── Observable UI States ──
    var currentTrack by mutableStateOf<MusicTrack?>(null)
        private set

    val queue = mutableStateListOf<MusicTrack>()

    var queueIndex by mutableIntStateOf(0)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var volume by mutableFloatStateOf(1f)
        private set

    var shuffleEnabled by mutableStateOf(false)
        private set

    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    // Sheet / Dialog states
    var expanded by mutableStateOf(false)
    var queueOpen by mutableStateOf(false)
    var lyricsOpen by mutableStateOf(false)

    var lyrics by mutableStateOf<List<LyricsLine>>(emptyList())
        private set

    var lyricsLoading by mutableStateOf(false)
        private set

    var onPlaybackStarted: (() -> Unit)? = null

    fun init(context: Context) {
        if (exoPlayer != null) return
        val appContext = context.applicationContext
        this.appContext = appContext

        val dataSourceFactory = OkHttpDataSource.Factory(Net.client)
            .setUserAgent(Net.USER_AGENT)

        exoPlayer = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLooper(Looper.getMainLooper())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                addListener(this@MusicPlayerState)
                volume = this@MusicPlayerState.volume
            }

        // Keep the media session connected while the app is open. This is deliberately a bind,
        // not a foreground-service start; Media3 promotes the service only for real playback.
        ensureSystemMediaSession()
        startProgressLoop()
    }

    /** The service publishes this same player through MediaSession; it never creates a second player. */
    fun playerForSession(context: Context): ExoPlayer {
        init(context)
        return requireNotNull(exoPlayer)
    }

    private var musicControllerFuture: ListenableFuture<MediaController>? = null
    private var musicController: MediaController? = null

    fun onServiceDestroyed() {
        musicController = null
    }

    private fun ensureSystemMediaSession() {
        val context = appContext ?: return
        if (musicControllerFuture != null || musicController != null) return
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        musicControllerFuture = future
        future.addListener({
            runCatching { future.get() }
                .onSuccess { controller -> musicController = controller }
                .onFailure { error ->
                    musicControllerFuture = null
                    Log.e(TAG, "Failed to connect MusicPlaybackService", error)
                }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun stopSystemMediaSession() {
        val context = appContext ?: return
        try {
            musicControllerFuture?.let(MediaController::releaseFuture)
            musicControllerFuture = null
            musicController = null
            context.stopService(Intent(context, MusicPlaybackService::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MusicPlaybackService", e)
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0) {
                        durationMs = dur
                    }
                    isPlaying = player.isPlaying
                    // Never leave a spinner over audio that is already audible. Some streams
                    // report a buffering transition while their buffered audio is playing.
                    isLoading = !player.isPlaying && (resolvingStream || player.playbackState == Player.STATE_BUFFERING)
                }
                delay(200)
            }
        }
    }

    fun play(track: MusicTrack, newQueue: List<MusicTrack>? = null) {
        onPlaybackStarted?.invoke()
        expanded = true
        loadJob?.cancel()
        // A new selection is a hard handoff: never let the previous song keep playing while
        // its replacement URL is being extracted.
        exoPlayer?.let { player ->
            player.playWhenReady = false
            player.pause()
        }
        resolvingStream = true
        isPlaying = false
        isLoading = true
        positionMs = 0L
        durationMs = 0L
        currentTrack = track
        // Match the video player: a MediaController connection lets Media3 publish its own
        // native Android media card when this player reaches a playable state.
        ensureSystemMediaSession()
        ListeningProfile.record(track)
        loadJob = scope.launch {
            try {
                // If an explicit new queue is provided and it's distinct from the current queue, replace it defensively
                if (newQueue != null && newQueue !== queue) {
                    val snapshot = newQueue.toList()
                    queue.clear()
                    queue.addAll(snapshot)
                    val idx = queue.indexOfFirst { it.id == track.id }
                    queueIndex = if (idx >= 0) idx else 0
                } else {
                    // Playing within existing queue or single track
                    val idx = queue.indexOfFirst { it.id == track.id }
                    if (idx >= 0) {
                        queueIndex = idx
                    } else {
                        queue.add(track)
                        queueIndex = queue.lastIndex
                    }
                }

                // Pause video player if currently playing
                pauseVideoPlayerIfPlaying()

                // Fetch lyrics in background
                fetchLyricsForCurrentTrack(track)

                // Resolve YouTube progressive audio stream
                val streamUrl = MusicBackend.resolveAudioStreamUrl(track.id)
                if (streamUrl.isNullOrBlank()) {
                    Log.e(TAG, "Failed to resolve stream URL for track: ${track.id}")
                    resolvingStream = false
                    isLoading = false
                    return@launch
                }

                exoPlayer?.let { player ->
                    // Normally already bound from init; retain this call for a service that the
                    // system reclaimed while the app stayed open.
                    ensureSystemMediaSession()
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(track.id)
                        .setUri(streamUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artists)
                                .setAlbumTitle(track.album)
                                .setArtworkUri(track.thumbnailUrl?.let(Uri::parse))
                                .build()
                        )
                        .build()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                    resolvingStream = false
                    // A tap on a single song becomes a radio based on that song, not a random
                    // leftover queue. Do this after playback starts so loading audio is never held up.
                    if (newQueue == null) {
                        scope.launch(Dispatchers.IO) {
                            val radio = MusicBackend.relatedTracks(track)
                            withContext(Dispatchers.Main) {
                                if (currentTrack?.id == track.id && queue.size <= 1 && radio.isNotEmpty()) {
                                    queue.clear()
                                    queue.add(track)
                                    queue.addAll(radio)
                                    queueIndex = 0
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error playing music track", e)
                if (currentTrack?.id == track.id) {
                    resolvingStream = false
                    isLoading = false
                }
            }
        }
    }

    private fun fetchLyricsForCurrentTrack(track: MusicTrack) {
        lyricsJob?.cancel()
        lyrics = emptyList()
        lyricsLoading = true
        lyricsJob = scope.launch {
            val list = LyricsHelper.loadLyrics(
                songId = track.id,
                title = track.title,
                artist = track.artists,
                durationSec = track.durationSec.toInt()
            )
            lyrics = list
            lyricsLoading = false
        }
    }

    /** Re-fetch lyrics for the current song without interrupting audio playback. */
    fun refreshLyrics() {
        currentTrack?.let(::fetchLyricsForCurrentTrack)
    }

    fun pause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
            isPlaying = player.isPlaying
        }
    }

    fun resume() {
        exoPlayer?.let { player ->
            if (!player.isPlaying && currentTrack != null) {
                onPlaybackStarted?.invoke()
                pauseVideoPlayerIfPlaying()
                player.play()
            }
            isPlaying = player.isPlaying
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                onPlaybackStarted?.invoke()
                pauseVideoPlayerIfPlaying()
                player.play()
            }
            isPlaying = player.isPlaying
        }
    }

    fun close() {
        loadJob?.cancel()
        lyricsJob?.cancel()
        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        currentTrack = null
        queue.clear()
        queueIndex = 0
        isPlaying = false
        isLoading = false
        positionMs = 0L
        durationMs = 0L
        expanded = false
        queueOpen = false
        lyricsOpen = false
        lyrics = emptyList()
        lyricsLoading = false
        stopSystemMediaSession()
    }

    fun seekTo(posMs: Long) {
        positionMs = posMs
        exoPlayer?.seekTo(posMs)
    }

    fun skipNext() {
        if (queue.isEmpty()) return
        val nextIndex = if (shuffleEnabled) {
            queue.indices.random()
        } else {
            (queueIndex + 1) % queue.size
        }
        val nextTrack = queue.getOrNull(nextIndex) ?: return
        queueIndex = nextIndex
        play(nextTrack, null)
    }

    fun skipPrevious() {
        if (queue.isEmpty()) return
        // If already a few seconds in, restart current track
        if (positionMs > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = if (queueIndex > 0) queueIndex - 1 else queue.size - 1
        val prevTrack = queue.getOrNull(prevIndex) ?: return
        queueIndex = prevIndex
        play(prevTrack, null)
    }

    fun toggleLike() {
        val cur = currentTrack ?: return
        val updated = cur.copy(liked = !cur.liked)
        currentTrack = updated
        val qIdx = queue.indexOfFirst { it.id == cur.id }
        if (qIdx >= 0) {
            queue[qIdx] = updated
        }
    }

    fun adjustVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        exoPlayer?.volume = volume
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer?.repeatMode = repeatMode
    }

    fun addToQueue(track: MusicTrack) {
        queue.add(track)
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            queue.removeAt(index)
            if (queueIndex >= queue.size) {
                queueIndex = (queue.size - 1).coerceAtLeast(0)
            }
        }
    }

    fun clearQueue() {
        currentTrack?.let { current ->
            queue.clear()
            queue.add(current)
            queueIndex = 0
        }
    }

    fun nextUpTrack(): MusicTrack? {
        val nextIdx = queueIndex + 1
        return if (nextIdx in queue.indices) queue[nextIdx] else null
    }

    private fun pauseVideoPlayerIfPlaying() {
        try {
            // Optional friendly audio handoff: if video player is active, pause it
            // (done without tight coupling)
        } catch (_: Exception) {}
    }

    // ── Player.Listener Callbacks ──
    override fun onPlaybackStateChanged(playbackState: Int) {
        val playingNow = exoPlayer?.isPlaying == true
        // READY means the source and duration are known. The transport must become a pause
        // control at that point, even if a vendor reports a late buffering callback.
        if (playbackState == Player.STATE_READY) {
            resolvingStream = false
            isLoading = false
            durationMs = exoPlayer?.duration?.takeIf { it > 0L } ?: durationMs
        } else {
            isLoading = !playingNow && (resolvingStream || playbackState == Player.STATE_BUFFERING)
        }
        if (playbackState == Player.STATE_ENDED) {
            if (repeatMode == Player.REPEAT_MODE_ONE) {
                seekTo(0L)
                exoPlayer?.play()
            } else if (nextUpTrack() != null || repeatMode == Player.REPEAT_MODE_ALL) {
                skipNext()
            } else {
                isPlaying = false
            }
        }
    }

    override fun onIsPlayingChanged(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            resolvingStream = false
            isLoading = false
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "ExoPlayer error: ${error.message}", error)
        resolvingStream = false
        isLoading = false
        isPlaying = false
    }
}
