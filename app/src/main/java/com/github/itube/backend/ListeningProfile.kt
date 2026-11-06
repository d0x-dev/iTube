package com.github.itube

import android.content.Context
import android.content.SharedPreferences

/** A small on-device taste profile. It never uploads data: it only chooses the next YouTube Music query. */
object ListeningProfile {
    private lateinit var prefs: SharedPreferences
    private const val KEY = "music_taste_tokens"

    fun init(context: Context) { prefs = context.getSharedPreferences("listening_profile", Context.MODE_PRIVATE) }

    fun record(track: MusicTrack) = recordText("${track.artists} ${track.title}")
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

    private val stopWords = setOf("official", "video", "audio", "music", "with", "from", "the", "and", "feat", "song")
}
