package com.aayt.caster.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeUrlParserTest {

    @Test
    fun extractsCanonicalUrlFromNativeShareText() {
        val shared = "快來看看這部超棒的 MV！ https://youtu.be/dQw4w9WgXcQ?si=9a8b7c6d5e"
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            YoutubeUrlParser.extractCanonicalUrl(shared)
        )
    }

    @Test
    fun extractsCanonicalUrlFromShortsAndLiveLinks() {
        assertEquals(
            "https://www.youtube.com/watch?v=abcdefghijk",
            YoutubeUrlParser.extractCanonicalUrl("https://www.youtube.com/shorts/abcdefghijk?feature=share")
        )
        assertEquals(
            "https://www.youtube.com/watch?v=1234567890A",
            YoutubeUrlParser.extractCanonicalUrl("https://www.youtube.com/live/1234567890A?si=xyz")
        )
    }

    @Test
    fun extractsCanonicalUrlFromMobileWatchLink() {
        assertEquals(
            "https://www.youtube.com/watch?v=_A-B1234567",
            YoutubeUrlParser.extractCanonicalUrl("https://m.youtube.com/watch?app=m&v=_A-B1234567")
        )
    }

    @Test
    fun returnsNullForNonYoutubeText() {
        assertNull(YoutubeUrlParser.extractCanonicalUrl("今天天氣真好"))
        assertNull(YoutubeUrlParser.extractCanonicalUrl("https://example.com/video/123"))
    }

    @Test
    fun formatsVideoDurationAndLiveBadgeCleanly() {
        val normal = YtVideoItem(
            videoId = "dQw4w9WgXcQ",
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            title = "Test",
            uploaderName = "Channel",
            thumbnailUrl = "",
            durationSec = 3725L,
            isLive = false
        )
        assertEquals("1:02:05", normal.formattedDuration)

        val live = normal.copy(isLive = true)
        assertEquals("LIVE 直播", live.formattedDuration)
    }
}
