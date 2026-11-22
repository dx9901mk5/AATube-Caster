package com.aayt.caster.data

import androidx.media3.common.MimeTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class YoutubeExtractorRepo {

    private val ytService get() = ServiceList.YouTube

    suspend fun fetchHomeFeed(): Result<List<YtVideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val kioskItems = runCatching {
                val kioskList = ytService.kioskList
                val defaultKioskId = kioskList.defaultKioskId
                val url = kioskList.getListLinkHandlerFactoryByType(defaultKioskId).fromId(defaultKioskId).url
                val info = KioskInfo.getInfo(ytService, url)
                info.relatedItems.mapNotNull { it.toYtVideoItem() }
            }.getOrDefault(emptyList())

            if (kioskItems.isNotEmpty()) {
                kioskItems
            } else {
                searchInternal("台灣 熱門 音樂 MV Podcast")
            }
        }
    }

    suspend fun fetchLiveNewsFeed(): Result<List<YtVideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            searchInternal("台灣 新聞 24小時 直播 LIVE")
        }
    }

    suspend fun searchVideos(query: String): Result<List<YtVideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            searchInternal(query)
        }
    }

    private fun searchInternal(query: String): List<YtVideoItem> {
        val searchHandler = ytService.searchQHFactory.fromQuery(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.VIDEOS),
            ""
        )
        val searchInfo = SearchInfo.getInfo(ytService, searchHandler)
        return searchInfo.relatedItems.mapNotNull { it.toYtVideoItem() }
    }

    suspend fun resolveStream(urlOrId: String): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        runCatching {
            val canonicalUrl = YoutubeUrlParser.extractCanonicalUrl(urlOrId)
                ?: throw IllegalArgumentException("無效的 YouTube 連結或影片 ID：$urlOrId")

            val info = StreamInfo.getInfo(ytService, canonicalUrl)
            val isLive = info.streamType == StreamType.LIVE_STREAM ||
                info.streamType == StreamType.AUDIO_LIVE_STREAM

            val videoId = info.id.ifBlank {
                YoutubeUrlParser.extractVideoId(canonicalUrl) ?: canonicalUrl.hashCode().toString()
            }
            val thumbUrl = info.thumbnails.maxByOrNull { it.width * it.height }?.url
                ?: YoutubeUrlParser.thumbnailForVideoId(videoId)

            val item = YtVideoItem(
                videoId = videoId,
                url = canonicalUrl,
                title = info.name.ifBlank { "YouTube 影片" },
                uploaderName = info.uploaderName.ifBlank { "YouTube 頻道" },
                thumbnailUrl = thumbUrl,
                durationSec = info.duration.coerceAtLeast(0L),
                isLive = isLive,
                viewCountText = formatViews(info.viewCount)
            )

            // Best audio stream (prefer M4A / MP4A for universal hardware decode compatibility, then Opus/WebM)
            val bestAudio = info.audioStreams
                .filter { it.content.isNotBlank() }
                .sortedWith(
                    compareByDescending<org.schabi.newpipe.extractor.stream.AudioStream> {
                        it.format?.mimeType?.contains("mp4", ignoreCase = true) == true
                    }.thenByDescending { it.averageBitrate }
                )
                .firstOrNull()

            // Progressive streams (video + audio combined)
            val progressiveOptions = info.videoStreams
                .filter { it.content.isNotBlank() }
                .map { stream ->
                    val height = parseHeight(stream.resolution)
                    VideoQualityOption(
                        label = stream.resolution.ifBlank { "${height}p" },
                        height = height,
                        videoUrl = stream.content,
                        isVideoOnly = false
                    )
                }

            // Adaptive video-only streams (1080p / 720p60 / 480p), prefer H.264/MP4 or VP9 over AV1 for low heat in car
            val adaptiveOptions = info.videoOnlyStreams
                .filter { it.content.isNotBlank() && bestAudio != null }
                .sortedWith(
                    compareByDescending<org.schabi.newpipe.extractor.stream.VideoStream> {
                        parseHeight(it.resolution)
                    }.thenByDescending {
                        val codec = it.codec?.lowercase().orEmpty()
                        when {
                            codec.startsWith("avc") -> 3
                            codec.startsWith("vp9") -> 2
                            else -> 1
                        }
                    }
                )
                .map { stream ->
                    val height = parseHeight(stream.resolution)
                    VideoQualityOption(
                        label = stream.resolution.ifBlank { "${height}p" },
                        height = height,
                        videoUrl = stream.content,
                        isVideoOnly = true
                    )
                }

            // Deduplicate by height (cap at 1080p so phone stays cool during Android Auto projection)
            val mergedQualities = (adaptiveOptions + progressiveOptions)
                .filter { it.height in 144..1080 }
                .distinctBy { it.height }
                .sortedByDescending { it.height }

            val subtitles = info.subtitles
                .filter { it.content.isNotBlank() }
                .map { sub ->
                    val mime = when (sub.format?.suffix?.lowercase()) {
                        "vtt" -> MimeTypes.TEXT_VTT
                        "srt" -> MimeTypes.APPLICATION_SUBRIP
                        else -> MimeTypes.APPLICATION_TTML
                    }
                    val autoSuffix = if (sub.isAutoGenerated) " (自動)" else ""
                    SubtitleOption(
                        label = (sub.displayLanguageName ?: sub.languageTag) + autoSuffix,
                        languageCode = sub.languageTag,
                        url = sub.content,
                        mimeType = mime
                    )
                }

            val related = info.relatedItems
                .mapNotNull { it.toYtVideoItem() }
                .take(24)

            ResolvedStream(
                item = item,
                isLiveStream = isLive,
                hlsUrl = info.hlsUrl?.takeIf { it.isNotBlank() },
                dashMpdUrl = info.dashMpdUrl?.takeIf { it.isNotBlank() },
                bestAudioUrl = bestAudio?.content,
                qualities = mergedQualities,
                subtitles = subtitles,
                relatedVideos = related
            )
        }
    }

    private fun InfoItem.toYtVideoItem(): YtVideoItem? {
        if (this !is StreamInfoItem) return null
        val canonical = YoutubeUrlParser.extractCanonicalUrl(url) ?: url
        val id = YoutubeUrlParser.extractVideoId(canonical) ?: canonical.hashCode().toString()
        val thumb = thumbnails.maxByOrNull { it.width * it.height }?.url
            ?: YoutubeUrlParser.thumbnailForVideoId(id)
        val live = streamType == StreamType.LIVE_STREAM ||
            streamType == StreamType.AUDIO_LIVE_STREAM ||
            duration < 0L
        return YtVideoItem(
            videoId = id,
            url = canonical,
            title = name.ifBlank { "YouTube 影片" },
            uploaderName = uploaderName?.ifBlank { "YouTube" } ?: "YouTube",
            thumbnailUrl = thumb,
            durationSec = duration.coerceAtLeast(0L),
            isLive = live,
            viewCountText = formatViews(viewCount)
        )
    }

    private fun parseHeight(resolution: String?): Int {
        if (resolution.isNullOrBlank()) return 360
        val digits = Regex("""(\d{3,4})p""").find(resolution)?.groupValues?.getOrNull(1)
            ?: Regex("""^(\d{3,4})""").find(resolution)?.groupValues?.getOrNull(1)
        return digits?.toIntOrNull() ?: 360
    }

    private fun formatViews(views: Long): String {
        if (views <= 0L) return ""
        return when {
            views >= 100_000_000L -> "%.1f 億次觀看".format(views / 100_000_000.0)
            views >= 10_000L -> "%.1f 萬次觀看".format(views / 10_000.0)
            else -> "$views 次觀看"
        }
    }
}
