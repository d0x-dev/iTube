package com.github.itube

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata record for a cached offline audio track.
 * Stores complete audio file reference, artist details, and track metadata.
 */
data class CachedMusicTrack(
    val id: String,
    val title: String,
    val artists: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long = 0L,
    val audioFilePath: String,
    val fileSizeBytes: Long = 0L,
    val cachedAtMs: Long = System.currentTimeMillis(),
    var lastPlayedMs: Long = System.currentTimeMillis(),
    val liked: Boolean = false,
) {
    fun toMusicTrack(): MusicTrack = MusicTrack(
        id = id,
        title = title,
        artists = artists,
        artistId = artistId,
        album = album,
        albumId = albumId,
        thumbnailUrl = thumbnailUrl,
        durationSec = durationSec,
        liked = liked,
        isVideo = false,
    )
}

/**
 * Dedicated offline audio caching engine for iMusic.
 * - Live-caches audio while streaming so songs play 100% offline with zero buffering.
 * - Stores complete metadata (title, artists, artistId, album, albumId, thumbnail, duration).
 * - Manages storage limits (LRU eviction) and user storage settings.
 * - STRICTLY for music audio: never caches video streams to conserve device memory.
 */
object MusicCacheManager {
    private const val TAG = "MusicCacheManager"
    private const val PREFS_NAME = "music_cache_settings"
    private const val KEY_ENABLED = "caching_enabled"
    private const val KEY_LIMIT_MB = "max_cache_limit_mb"
    private const val INDEX_FILE = "cached_songs_index.json"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var cacheDir: File
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active download jobs for live caching
    private val activeDownloads = ConcurrentHashMap<String, Job>()

    // Observable Compose states for UI
    val cachedTracks = mutableStateListOf<CachedMusicTrack>()
    val totalStorageBytes = mutableLongStateOf(0L)
    val cachingEnabled = mutableStateOf(true)
    val maxCacheLimitMB = mutableLongStateOf(2048L) // 2 GB default

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cacheDir = File(app.filesDir, "music_cache").apply { if (!exists()) mkdirs() }

        cachingEnabled.value = prefs.getBoolean(KEY_ENABLED, true)
        maxCacheLimitMB.value = prefs.getLong(KEY_LIMIT_MB, 2048L)

        loadIndex()
    }

    private fun loadIndex() {
        scope.launch {
            try {
                val indexFile = File(cacheDir, INDEX_FILE)
                val loadedList = mutableListOf<CachedMusicTrack>()
                var totalBytes = 0L

                if (indexFile.exists()) {
                    val raw = indexFile.readText()
                    if (raw.isNotBlank()) {
                        val arr = JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val id = o.optString("id")
                            val audioPath = o.optString("audioFilePath")
                            if (id.isBlank() || audioPath.isBlank()) continue

                            val audioFile = File(audioPath)
                            if (audioFile.exists() && audioFile.length() > 0) {
                                val size = audioFile.length()
                                totalBytes += size
                                loadedList.add(
                                    CachedMusicTrack(
                                        id = id,
                                        title = o.optString("title"),
                                        artists = o.optString("artists"),
                                        artistId = o.optString("artistId").ifBlank { null },
                                        album = o.optString("album").ifBlank { null },
                                        albumId = o.optString("albumId").ifBlank { null },
                                        thumbnailUrl = o.optString("thumbnailUrl").ifBlank { null },
                                        durationSec = o.optLong("durationSec", 0L),
                                        audioFilePath = audioPath,
                                        fileSizeBytes = size,
                                        cachedAtMs = o.optLong("cachedAtMs", System.currentTimeMillis()),
                                        lastPlayedMs = o.optLong("lastPlayedMs", System.currentTimeMillis()),
                                        liked = o.optBoolean("liked", false)
                                    )
                                )
                            }
                        }
                    }
                }

                // Sort by last played descending
                loadedList.sortByDescending { it.lastPlayedMs }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    cachedTracks.clear()
                    cachedTracks.addAll(loadedList)
                    totalStorageBytes.longValue = totalBytes
                }

                enforceCacheLimit()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached music index", e)
            }
        }
    }

    private fun persistIndex() {
        scope.launch {
            try {
                val arr = JSONArray()
                val snapshot = cachedTracks.toList()
                var total = 0L
                snapshot.forEach { track ->
                    total += track.fileSizeBytes
                    arr.put(
                        JSONObject()
                            .put("id", track.id)
                            .put("title", track.title)
                            .put("artists", track.artists)
                            .put("artistId", track.artistId)
                            .put("album", track.album)
                            .put("albumId", track.albumId)
                            .put("thumbnailUrl", track.thumbnailUrl)
                            .put("durationSec", track.durationSec)
                            .put("audioFilePath", track.audioFilePath)
                            .put("fileSizeBytes", track.fileSizeBytes)
                            .put("cachedAtMs", track.cachedAtMs)
                            .put("lastPlayedMs", track.lastPlayedMs)
                            .put("liked", track.liked)
                    )
                }
                val indexFile = File(cacheDir, INDEX_FILE)
                indexFile.writeText(arr.toString())

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    totalStorageBytes.longValue = total
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting cached music index", e)
            }
        }
    }

    /**
     * Checks if the given track is already cached offline and returns its file.
     */
    fun getCachedAudioFile(trackId: String): File? {
        val entry = cachedTracks.firstOrNull { it.id == trackId } ?: return null
        val file = File(entry.audioFilePath)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Returns true if track is cached and ready for offline playback.
     */
    fun isTrackCached(trackId: String): Boolean = getCachedAudioFile(trackId) != null

    /**
     * Marks track as recently played for LRU (Least Recently Used) cache ranking.
     */
    fun markTrackPlayed(trackId: String) {
        val idx = cachedTracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            val item = cachedTracks[idx]
            item.lastPlayedMs = System.currentTimeMillis()
            persistIndex()
        }
    }

    /**
     * Starts live background caching while a song is playing.
     * Streams the audio into a temporary file, and commits upon completion with all metadata.
     */
    fun startLiveCaching(track: MusicTrack, streamUrl: String) {
        if (!cachingEnabled.value) return
        if (isTrackCached(track.id)) return
        if (activeDownloads.containsKey(track.id)) return

        val job = scope.launch {
            val tmpFile = File(cacheDir, "${track.id}.tmp")
            val finalFile = File(cacheDir, "${track.id}.m4a")

            try {
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", Net.USER_AGENT)
                    .build()

                Net.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Live caching failed for ${track.id}: HTTP ${response.code}")
                        return@launch
                    }

                    val body = response.body ?: return@launch
                    val stream = body.byteStream()
                    FileOutputStream(tmpFile).use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (stream.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                        }
                        out.flush()
                    }

                    if (tmpFile.exists() && tmpFile.length() > 0) {
                        if (finalFile.exists()) finalFile.delete()
                        if (tmpFile.renameTo(finalFile)) {
                            val cachedTrack = CachedMusicTrack(
                                id = track.id,
                                title = track.title,
                                artists = track.artists,
                                artistId = track.artistId,
                                album = track.album,
                                albumId = track.albumId,
                                thumbnailUrl = track.thumbnailUrl,
                                durationSec = track.durationSec,
                                audioFilePath = finalFile.absolutePath,
                                fileSizeBytes = finalFile.length(),
                                cachedAtMs = System.currentTimeMillis(),
                                lastPlayedMs = System.currentTimeMillis(),
                                liked = ListeningProfile.isLiked(track.id)
                            )

                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                val existingIdx = cachedTracks.indexOfFirst { it.id == track.id }
                                if (existingIdx >= 0) {
                                    cachedTracks[existingIdx] = cachedTrack
                                } else {
                                    cachedTracks.add(0, cachedTrack)
                                }
                            }

                            persistIndex()
                            enforceCacheLimit()
                            Log.d(TAG, "Live cache completed for '${track.title}' (${formatStorageSize(finalFile.length())})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during live caching of ${track.id}: ${e.message}")
            } finally {
                activeDownloads.remove(track.id)
                if (tmpFile.exists()) tmpFile.delete()
            }
        }

        activeDownloads[track.id] = job
    }

    /**
     * Removes a cached song from disk and database.
     */
    fun removeCachedTrack(trackId: String): Boolean {
        activeDownloads[trackId]?.cancel()
        activeDownloads.remove(trackId)

        val idx = cachedTracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            val item = cachedTracks.removeAt(idx)
            val file = File(item.audioFilePath)
            if (file.exists()) file.delete()
            persistIndex()
            return true
        }
        return false
    }

    /**
     * Clears all cached offline songs and frees up storage.
     */
    fun clearAllCache() {
        scope.launch {
            activeDownloads.values.forEach { it.cancel() }
            activeDownloads.clear()

            cacheDir.listFiles()?.forEach { file ->
                if (file.name != INDEX_FILE) {
                    file.delete()
                }
            }

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                cachedTracks.clear()
                totalStorageBytes.longValue = 0L
            }

            persistIndex()
        }
    }

    /**
     * Enforces the user's storage limit using LRU eviction.
     */
    fun enforceCacheLimit() {
        val limitMB = maxCacheLimitMB.longValue
        if (limitMB <= 0) return // Unlimited
        val maxBytes = limitMB * 1024L * 1024L

        scope.launch {
            while (totalStorageBytes.longValue > maxBytes && cachedTracks.isNotEmpty()) {
                val oldest = cachedTracks.minByOrNull { it.lastPlayedMs } ?: break
                Log.d(TAG, "Cache limit exceeded (${formatStorageSize(totalStorageBytes.longValue)} > ${limitMB}MB). Evicting LRU: ${oldest.title}")
                removeCachedTrack(oldest.id)
            }
        }
    }

    fun setCachingEnabled(enabled: Boolean) {
        cachingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setMaxCacheLimitMB(limitMB: Long) {
        maxCacheLimitMB.longValue = limitMB
        prefs.edit().putLong(KEY_LIMIT_MB, limitMB).apply()
        enforceCacheLimit()
    }

    fun getAllCachedAsMusicTracks(): List<MusicTrack> {
        return cachedTracks.map { it.toMusicTrack() }
    }

    fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
