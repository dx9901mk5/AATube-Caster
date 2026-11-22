package com.aayt.caster.data

object YoutubeUrlParser {

    private val urlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

    private val idPatterns = listOf(
        Regex("""(?:youtube\.com/watch\?.*?v=|youtu\.be/|youtube\.com/shorts/|youtube\.com/live/|youtube\.com/embed/)([A-Za-z0-9_-]{11})"""),
        Regex("""^([A-Za-z0-9_-]{11})$""")
    )

    /**
     * Extracts a canonical YouTube watch URL from arbitrary shared text or URL.
     * Returns null if no valid YouTube link or 11-char video ID is found.
     */
    fun extractCanonicalUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()

        // Check direct 11-char ID first
        if (trimmed.matches(Regex("""^[A-Za-z0-9_-]{11}$"""))) {
            return "https://www.youtube.com/watch?v=$trimmed"
        }

        // Extract candidate URL from share text (e.g. "Watch this! https://youtu.be/xxxx?si=yyyy")
        val candidate = urlRegex.find(trimmed)?.value ?: trimmed
        val videoId = extractVideoId(candidate)
        if (videoId != null) {
            return "https://www.youtube.com/watch?v=$videoId"
        }

        // Allow direct youtube URLs if NewPipe can parse them
        if (candidate.contains("youtube.com", ignoreCase = true) ||
            candidate.contains("youtu.be", ignoreCase = true)
        ) {
            return candidate
        }
        return null
    }

    fun extractVideoId(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        for (pattern in idPatterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun thumbnailForVideoId(videoId: String): String =
        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}
