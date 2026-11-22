package com.aayt.caster.data

import kotlinx.serialization.Serializable

@Serializable
data class YtVideoItem(
    val videoId: String,
    val url: String,
    val title: String,
    val uploaderName: String,
    val thumbnailUrl: String,
    val durationSec: Long = 0L,
    val isLive: Boolean = false,
    val viewCountText: String = ""
) {
    val formattedDuration: String
        get() {
            if (isLive || durationSec <= 0L) return if (isLive) "LIVE 直播" else ""
            val hours = durationSec / 3600
            val minutes = (durationSec % 3600) / 60
            val seconds = durationSec % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
}

data class VideoQualityOption(
    val label: String,
    val height: Int,
    val videoUrl: String,
    val isVideoOnly: Boolean
)

data class SubtitleOption(
    val label: String,
    val languageCode: String,
    val url: String,
    val mimeType: String
)

data class ResolvedStream(
    val item: YtVideoItem,
    val isLiveStream: Boolean,
    val hlsUrl: String?,
    val dashMpdUrl: String?,
    val bestAudioUrl: String?,
    val qualities: List<VideoQualityOption>,
    val subtitles: List<SubtitleOption>,
    val relatedVideos: List<YtVideoItem>
)

@Serializable
enum class ShareBehavior(val title: String) {
    PLAY_IMMEDIATELY("立即插播"),
    ASK_WHEN_PLAYING("播放中時詢問（立即插播／排入下一首）"),
    ENQUEUE_NEXT("排入下一首待播")
}

enum class VideoAspectMode(val label: String) {
    FIT("原始比例 (16:9)"),
    ZOOM_TO_FILL("填滿車機寬螢幕")
}

enum class CarViewMode {
    BROWSE,
    FULLSCREEN_PLAYER
}

enum class CarBrowseTab(val title: String) {
    HOME("熱門推薦"),
    LIVE_CHANNELS("新聞/直播"),
    QUEUE("待播佇列"),
    HISTORY("觀看紀錄")
}

data class SeekFeedbackEvent(
    val accumulatedSeconds: Int,
    val isForward: Boolean,
    val triggerTimeMs: Long = System.currentTimeMillis()
)

data class ZoomFeedbackEvent(
    val mode: VideoAspectMode,
    val triggerTimeMs: Long = System.currentTimeMillis()
)
