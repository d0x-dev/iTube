package com.github.itube

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** A small on-device taste profile. It never uploads data: it only chooses the next YouTube Music query. */
object ListeningProfile {
    private lateinit var prefs: SharedPreferences
    private const val KEY = "music_taste_tokens"
    private const val KEY_RECENT = "music_recent_tracks"

    fun init(context: Context) { prefs = context.getSharedPreferences("listening_profile", Context.MODE_PRIVATE) }

    fun record(track: MusicTrack) {
        if (::prefs.isInitialized) {
            val recent = (listOf(track) + recentTracks().filterNot { it.id == track.id }).take(40)
            prefs.edit().putString(KEY_RECENT, JSONArray().apply {
                recent.forEach { song ->
                    put(JSONObject().put("id", song.id).put("title", song.title).put("artists", song.artists)
                        .put("album", song.album).put("thumbnail", song.thumbnailUrl).put("duration", song.durationSec))
                }
            }.toString()).apply()
        }
        recordText("${track.artists} ${track.title}")
    }
    fun record(video: VideoItem) = recordText("${video.channel} ${video.title}")

    private fun recordText(text: String) {
        if (!::prefs.isInitialized) return
        val old = prefs.getString(KEY, "").orEmpty().split('|').filter(String::isNotBlank)
        val tokens = text.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 && it !in stopWords }.take(8)
        prefs.edit().putString(KEY, (tokens + old).distinct().take(36).joinToString("|")).apply()
    }

    fun seed(): String? {
        if (!::prefs.isInitialized) return null
        return prefs.getString(KEY, "").orEmpty().split('|').filter(String::isNotBlank).take(4).joinToString(" ").ifBlank { null }
    }

    fun recentTracks(): List<MusicTrack> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            buildList {
                for (i in 0 until rows.length()) {
                    val song = rows.optJSONObject(i) ?: continue
                    val id = song.optString("id")
                    if (id.isBlank()) continue
                    add(MusicTrack(
                        id = id,
                        title = song.optString("title"),
                        artists = song.optString("artists"),
                        album = song.optString("album").ifBlank { null },
                        thumbnailUrl = song.optString("thumbnail").ifBlank { null },
                        durationSec = song.optLong("duration"),
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    private val stopWords = setOf("official", "video", "audio", "music", "with", "from", "the", "and", "feat", "song")
}
