package com.github.itube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object MusicBackend {
    private const val TAG = "MusicBackend"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val SONG_PARAMS = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchMusicHome(chipFilter: String? = null): MusicHomeFeed = withContext(Dispatchers.IO) {
        val chips = mutableListOf<String>()
        val quickPicks = mutableListOf<MusicTrack>()
        val sections = mutableListOf<MusicSection>()

        // 1. Fetch Explore (Trending, New releases)
        try {
            val exploreJson = postInnerTube("FEmusic_explore")
            parseShelves(exploreJson, sections, quickPicks)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching FEmusic_explore: ${e.message}")
        }

        // 2. Fetch Home (Personalized / Curated shelves & chips)
        try {
            val homeJson = postInnerTube("FEmusic_home")
            parseChips(homeJson, chips)
            parseShelves(homeJson, sections, quickPicks)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching FEmusic_home: ${e.message}")
        }

        // Default chips if none parsed
        if (chips.isEmpty()) {
            chips.addAll(listOf("Relax", "Workout", "Energize", "Focus", "Party", "Romance", "Chill", "Feel Good"))
        }

        // If quick picks are empty, take the first available tracks from trending/sections
        val finalQuickPicks = if (quickPicks.isNotEmpty()) {
            quickPicks.distinctBy { it.id }.take(12)
        } else {
            sections.flatMap { it.items }.distinctBy { it.id }.take(12)
        }

        // Filter sections to remove empty ones and deduplicate
        val finalSections = sections
            .filter { it.items.isNotEmpty() }
            .distinctBy { it.title }

        MusicHomeFeed(
            chips = chips.distinct(),
            quickPicks = finalQuickPicks,
            sections = finalSections
        )
    }

    private fun postInnerTube(browseId: String, params: String? = null): JSONObject {
        val clientObj = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", "1.20240101.01.00")
            .put("hl", "en")
            .put("gl", "US")

        val body = JSONObject()
            .put("browseId", browseId)
            .put("context", JSONObject().put("client", clientObj))

        if (params != null) {
            body.put("params", params)
        }

        val request = Request.Builder()
            .url(BROWSE_URL)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240101.01.00")
            .build()

        Net.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("InnerTube request failed: ${response.code}")
            }
            val text = response.body?.string().orEmpty()
            return JSONObject(text)
        }
    }

    private fun parseChips(root: JSONObject, outChips: MutableList<String>) {
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer") ?: return

        val headerChips = contents.optJSONObject("header")
            ?.optJSONObject("chipCloudRenderer")
            ?.optJSONArray("chips") ?: return

        for (i in 0 until headerChips.length()) {
            val chip = headerChips.optJSONObject(i)?.optJSONObject("chipCloudChipRenderer")
            val text = chip?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            if (!text.isNullOrBlank()) {
                outChips.add(text)
            }
        }
    }

    private fun parseShelves(
        root: JSONObject,
        outSections: MutableList<MusicSection>,
        outQuickPicks: MutableList<MusicTrack>
    ) {
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return

        for (i in 0 until contents.length()) {
            val shelfRenderer = contents.optJSONObject(i)?.optJSONObject("musicCarouselShelfRenderer") ?: continue
            val header = shelfRenderer.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            val title = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val label = header?.optJSONObject("strapline")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            val itemsArray = shelfRenderer.optJSONArray("contents") ?: continue

            val tracks = mutableListOf<MusicTrack>()
            for (j in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(j) ?: continue
                val track = parseTrackItem(itemObj)
                if (track != null) {
                    tracks.add(track)
                }
            }

            if (tracks.isNotEmpty()) {
                val isQuickPickCandidate = title.contains("Quick pick", ignoreCase = true) ||
                        title.contains("Trending", ignoreCase = true) ||
                        title.contains("Hits", ignoreCase = true)
                if (isQuickPickCandidate && outQuickPicks.isEmpty()) {
                    outQuickPicks.addAll(tracks)
                }
                outSections.add(
                    MusicSection(
                        title = title,
                        label = label,
                        thumbnail = tracks.firstOrNull()?.thumbnailUrl,
                        items = tracks
                    )
                )
            }
        }
    }

    private fun parseTrackItem(itemObj: JSONObject): MusicTrack? {
        // Handle musicResponsiveListItemRenderer (common in Trending / charts / search)
        itemObj.optJSONObject("musicResponsiveListItemRenderer")?.let { responsive ->
            val videoId = responsive.optJSONObject("playlistItemData")?.optString("videoId")
                ?: responsive.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                ?: responsive.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
            if (videoId.isNullOrBlank()) return null

            val flexColumns = responsive.optJSONArray("flexColumns")
            val title = flexColumns?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text").orEmpty()

            val subRuns = flexColumns?.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            var artists = ""
            var album: String? = null
            var durationSec = 0L

            if (subRuns != null && subRuns.length() > 0) {
                val nonDelimiterRuns = mutableListOf<String>()
                for (r in 0 until subRuns.length()) {
                    val t = subRuns.optJSONObject(r)?.optString("text").orEmpty().trim()
                    if (t.isNotEmpty() && t != "•" && t != "·") {
                        if (t.contains(":") && t.matches(Regex("\\d+:\\d+(:\\d+)?"))) {
                            durationSec = parseTimeString(t)
                        } else {
                            nonDelimiterRuns.add(t)
                        }
                    }
                }
                if (nonDelimiterRuns.isNotEmpty()) {
                    artists = nonDelimiterRuns[0]
                    if (nonDelimiterRuns.size > 1) {
                        album = nonDelimiterRuns[1]
                    }
                }
            }

            val thumbUrl = responsive.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { thumbs ->
                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                }?.highQualityThumbnail()

            return MusicTrack(
                id = videoId,
                title = title.ifBlank { "Unknown Title" },
                artists = artists.ifBlank { "Various Artists" },
                album = album,
                thumbnailUrl = thumbUrl,
                durationSec = durationSec
            )
        }

        // Handle musicTwoRowItemRenderer (common in carousel cards)
        itemObj.optJSONObject("musicTwoRowItemRenderer")?.let { twoRow ->
            val watchEndpoint = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
            val videoId = watchEndpoint?.optString("videoId")
            if (videoId.isNullOrBlank()) return null

            val title = twoRow.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text").orEmpty()

            val subtitleRuns = twoRow.optJSONObject("subtitle")?.optJSONArray("runs")
            val artists = subtitleRuns?.optJSONObject(0)?.optString("text").orEmpty()

            val thumbUrl = twoRow.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { thumbs ->
                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                }?.highQualityThumbnail()

            return MusicTrack(
                id = videoId,
                title = title.ifBlank { "Unknown Title" },
                artists = artists.ifBlank { "Various Artists" },
                thumbnailUrl = thumbUrl
            )
        }

        return null
    }

    private fun postInnerTubeSearch(query: String, params: String? = null): JSONObject {
        val clientObj = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", "1.20240101.01.00")
            .put("hl", "en")
            .put("gl", "US")

        val body = JSONObject()
            .put("query", query)
            .put("context", JSONObject().put("client", clientObj))

        if (params != null) {
            body.put("params", params)
        }

        val request = Request.Builder()
            .url(SEARCH_URL)
            .post(body.toString().toRequestBody(JSON_TYPE))
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20240101.01.00")
            .build()

        Net.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("InnerTube search failed: ${response.code}")
            }
            val text = response.body?.string().orEmpty()
            return JSONObject(text)
        }
    }

    private fun parseSearchSongs(root: JSONObject): List<MusicTrack> {
        val list = mutableListOf<MusicTrack>()
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return list

        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i) ?: continue
            val shelf = section.optJSONObject("musicShelfRenderer") ?: continue
            val items = shelf.optJSONArray("contents") ?: continue

            for (j in 0 until items.length()) {
                val itemObj = items.optJSONObject(j) ?: continue
                val track = parseTrackItem(itemObj)
                if (track != null) {
                    list.add(track)
                }
            }
        }
        return list
    }

    /** Resolves the highest quality progressive audio stream URL for a given YouTube video ID. */
    suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val info = StreamCache.getOrLoad(videoId)
            val audio = info.audioStreams
                .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
                .sortedWith(
                    compareBy<AudioStream> { if (it.audioTrackType == null || it.audioTrackType == AudioTrackType.ORIGINAL) 0 else 1 }
                        .thenBy { if (it.format == MediaFormat.M4A) 0 else 1 }
                        .thenByDescending { it.averageBitrate }
                ).firstOrNull() ?: info.audioStreams.maxByOrNull { it.averageBitrate }

            audio?.url ?: info.videoStreams.maxByOrNull { it.height }?.url
        }.getOrNull()
    }

    /** Searches YouTube Music (InnerTube native) with fallback to NewPipeExtractor. */
    suspend fun searchMusic(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        // 1. Try YouTube Music InnerTube search first (AirBeats native behavior)
        try {
            val json = postInnerTubeSearch(query, SONG_PARAMS)
            val songs = parseSearchSongs(json)
            if (songs.isNotEmpty()) {
                return@withContext songs
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube search failed, falling back to NewPipe: ${e.message}")
        }

        // 2. Fallback to NewPipeExtractor
        runCatching {
            val service = ServiceList.YouTube
            val handler = service.searchQHFactory.fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
            SearchInfo.getInfo(service, handler).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val id = service.streamLHFactory.getId(item.url)
                    val thumb = item.thumbnails.maxByOrNull { it.width }?.url?.highQualityThumbnail()
                    MusicTrack(
                        id = id,
                        title = item.name.orEmpty(),
                        artists = item.uploaderName.orEmpty(),
                        thumbnailUrl = thumb,
                        durationSec = item.duration
                    )
                }
        }.getOrDefault(emptyList())
    }
}
