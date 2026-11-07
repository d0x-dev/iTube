package com.github.itube

import androidx.compose.runtime.Immutable

@Immutable
data class MusicTrack(
    val id: String,
    val title: String,
    val artists: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long = 0,
    val liked: Boolean = false,
    val isVideo: Boolean = false,
) {
    val duration: String
        get() = if (durationSec > 0) formatDuration(durationSec * 1000L) else ""
}

@Immutable
data class MusicArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
)

@Immutable
data class MusicPlaylistRef(
    val id: String,
    val title: String,
    val author: String = "",
    val authorId: String? = null,
    val thumbnailUrl: String? = null,
    val isAlbum: Boolean = false,
    val trackCount: Int = 0,
    val year: String? = null,
)

@Immutable
data class ArtistDetails(
    val channelId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val bannerUrl: String? = null,
    val subscriberCountText: String? = null,
    val monthlyListenersText: String? = null,
    val description: String = "",
    val topTracks: List<MusicTrack> = emptyList(),
    val albums: List<MusicPlaylistRef> = emptyList(),
    val singles: List<MusicPlaylistRef> = emptyList(),
    val videos: List<MusicTrack> = emptyList(),
    val relatedArtists: List<MusicArtist> = emptyList(),
    val featuredOn: List<MusicPlaylistRef> = emptyList(),
)

@Immutable
data class PlaylistDetails(
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val author: String = "",
    val authorId: String? = null,
    val trackCount: Int = 0,
    val durationText: String? = null,
    val year: String? = null,
    val description: String? = null,
    val isAlbum: Boolean = false,
    val tracks: List<MusicTrack> = emptyList(),
)

@Immutable
data class MoodCategory(
    val title: String,
    val stripeColor: Long = 0xFF5C6BC0,
    val params: String? = null,
    val backgroundColor: Long = 0xFF2A2A2A,
)

@Immutable
data class MusicSection(
    val title: String,
    val label: String? = null,
    val thumbnail: String? = null,
    val items: List<MusicTrack> = emptyList(),
)

@Immutable
data class MusicHomeFeed(
    val chips: List<String> = emptyList(),
    val speedDial: List<MusicTrack> = emptyList(),
    val quickPicks: List<MusicTrack> = emptyList(),
    val dailyDiscover: List<MusicTrack> = emptyList(),
    val communityPlaylists: List<MusicPlaylistRef> = emptyList(),
    val moodsAndGenres: List<MoodCategory> = emptyList(),
    val artistsForYou: List<MusicArtist> = emptyList(),
    val fromArtistsYouLove: List<MusicPlaylistRef> = emptyList(),
    val newReleases: List<MusicPlaylistRef> = emptyList(),
    val mixedForYou: List<MusicPlaylistRef> = emptyList(),
    val popularArtists: List<MusicArtist> = emptyList(),
    val topAlbums: List<MusicPlaylistRef> = emptyList(),
    val favoriteArtistMix: List<MusicTrack> = emptyList(),
    val moreFrom: List<MusicTrack> = emptyList(),
    val energizeVibe: List<MusicTrack> = emptyList(),
    val trendingSongs: List<MusicTrack> = emptyList(),
    val chartTopArtists: List<MusicArtist> = emptyList(),
    val sections: List<MusicSection> = emptyList(),
)

@Immutable
data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val words: List<WordTiming> = emptyList(),
)

@Immutable
data class WordTiming(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

