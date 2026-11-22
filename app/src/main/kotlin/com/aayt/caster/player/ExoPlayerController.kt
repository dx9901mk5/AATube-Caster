package com.aayt.caster.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.aayt.caster.data.CarBrowseTab
import com.aayt.caster.data.CarViewMode
import com.aayt.caster.data.OkHttpNewPipeDownloader
import com.aayt.caster.data.QueuePreferences
import com.aayt.caster.data.ResolvedStream
import com.aayt.caster.data.SeekFeedbackEvent
import com.aayt.caster.data.ShareBehavior
import com.aayt.caster.data.SubtitleOption
import com.aayt.caster.data.VideoAspectMode
import com.aayt.caster.data.VideoQualityOption
import com.aayt.caster.data.YtVideoItem
import com.aayt.caster.data.YoutubeExtractorRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentStream: ResolvedStream? = null,
    val selectedQuality: VideoQualityOption? = null,
    val selectedSubtitle: SubtitleOption? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoadingStream: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val aspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val carViewMode: CarViewMode = CarViewMode.BROWSE,
    val carBrowseTab: CarBrowseTab = CarBrowseTab.HOME,
    val isCarProjected: Boolean = false,
    val isPlayerControlsVisible: Boolean = true,
    val isRelatedSheetOpen: Boolean = false,
    val isTouchLocked: Boolean = false,
    val seekFeedback: SeekFeedbackEvent? = null,
    val zoomFeedback: com.aayt.caster.data.ZoomFeedbackEvent? = null,
    val homeFeed: List<YtVideoItem> = emptyList(),
    val liveFeed: List<YtVideoItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<YtVideoItem> = emptyList(),
    val isSearching: Boolean = false,
    val queue: List<YtVideoItem> = emptyList(),
    val history: List<YtVideoItem> = emptyList(),
    val preferredQualityHeight: Int = 720,
    val shareBehavior: ShareBehavior = ShareBehavior.ASK_WHEN_PLAYING
)

@OptIn(UnstableApi::class)
class ExoPlayerController(
    private val appContext: Context,
    private val extractorRepo: YoutubeExtractorRepo,
    private val prefs: QueuePreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(OkHttpNewPipeDownloader.USER_AGENT)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true // Enables automatic Audio Ducking during Google Maps navigation voice prompts
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    /**
     * Wrapped player for MediaSession so steering-wheel Next / Previous buttons always work
     * even though ExoPlayer plays one resolved MergingMediaSource at a time.
     */
    val sessionPlayer: Player = object : ForwardingPlayer(exoPlayer) {
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands()
                .buildUpon()
                .add(COMMAND_SEEK_TO_NEXT)
                .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(COMMAND_SEEK_TO_PREVIOUS)
                .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            if (command == COMMAND_SEEK_TO_NEXT ||
                command == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                command == COMMAND_SEEK_TO_PREVIOUS ||
                command == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            ) {
                return true
            }
            return super.isCommandAvailable(command)
        }

        override fun seekToNext() = playNextInQueueOrRelated()
        override fun seekToNextMediaItem() = playNextInQueueOrRelated()
        override fun seekToPrevious() = restartOrPlayPrevious()
        override fun seekToPreviousMediaItem() = restartOrPlayPrevious()
    }

    private val _state = MutableStateFlow(
        PlaybackUiState(
            queue = prefs.loadQueue(),
            history = prefs.loadHistory(),
            preferredQualityHeight = prefs.preferredQualityHeight,
            shareBehavior = prefs.shareBehavior
        )
    )
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var autoHideControlsJob: Job? = null
    private var clearSeekFeedbackJob: Job? = null
    private var clearZoomFeedbackJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    scheduleAutoHideControls()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
                }
                if (playbackState == Player.STATE_ENDED) {
                    playNextInQueueOrRelated()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = "串流播放發生錯誤 (${error.errorCodeName})，正在嘗試備援畫質或請重試"
                    )
                }
            }
        })

        // Position ticker
        scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying || _state.value.currentStream != null) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                    _state.update { it.copy(positionMs = pos, durationMs = dur) }
                }
                delay(500L)
            }
        }

        // Preload Home & Live feeds for instant Car UI browsing
        refreshFeeds()
    }

    fun refreshFeeds() {
        scope.launch {
            extractorRepo.fetchHomeFeed().onSuccess { items ->
                _state.update { it.copy(homeFeed = items) }
            }
        }
        scope.launch {
            extractorRepo.fetchLiveNewsFeed().onSuccess { items ->
                _state.update { it.copy(liveFeed = items) }
            }
        }
    }

    fun searchYoutube(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _state.update {
            it.copy(
                searchQuery = trimmed,
                isSearching = true,
                carViewMode = CarViewMode.BROWSE,
                errorMessage = null
            )
        }
        scope.launch {
            extractorRepo.searchVideos(trimmed)
                .onSuccess { results ->
                    _state.update { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = "搜尋失敗：${err.localizedMessage ?: "網路連線異常"}"
                        )
                    }
                }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun playUrlOrVideo(urlOrId: String, openFullscreenOnCar: Boolean = true) {
        ensureForegroundServiceStarted()
        loadJob?.cancel()
        _state.update {
            it.copy(
                isLoadingStream = true,
                errorMessage = null,
                isRelatedSheetOpen = false,
                carViewMode = if (openFullscreenOnCar) CarViewMode.FULLSCREEN_PLAYER else it.carViewMode
            )
        }
        loadJob = scope.launch {
            extractorRepo.resolveStream(urlOrId)
                .onSuccess { resolved ->
                    val preferredHeight = _state.value.preferredQualityHeight
                    val chosenQuality = resolved.qualities
                        .firstOrNull { it.height <= preferredHeight }
                        ?: resolved.qualities.firstOrNull()
                    val defaultSub = resolved.subtitles.firstOrNull {
                        it.languageCode.startsWith("zh", ignoreCase = true) && !it.label.contains("自動")
                    }

                    addToHistory(resolved.item)
                    _state.update {
                        it.copy(
                            currentStream = resolved,
                            selectedQuality = chosenQuality,
                            selectedSubtitle = defaultSub,
                            isLoadingStream = false,
                            isPlayerControlsVisible = true
                        )
                    }
                    prepareAndPlaySource(resolved, chosenQuality, defaultSub, startPositionMs = 0L)
                    scheduleAutoHideControls()
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoadingStream = false,
                            errorMessage = "無法解析 YouTube 影片：${err.localizedMessage ?: "請確認網址或網路"}"
                        )
                    }
                }
        }
    }

    fun playItem(item: YtVideoItem) {
        // Remove from queue if present
        removeFromQueue(item.videoId)
        playUrlOrVideo(item.url, openFullscreenOnCar = true)
    }

    fun enqueueItem(item: YtVideoItem, playNext: Boolean = false) {
        val currentQueue = _state.value.queue.filterNot { it.videoId == item.videoId }
        val updated = if (playNext) {
            listOf(item) + currentQueue
        } else {
            currentQueue + item
        }
        prefs.saveQueue(updated)
        _state.update { it.copy(queue = updated) }
    }

    fun enqueueSharedUrl(urlOrId: String, playNext: Boolean = true) {
        scope.launch {
            extractorRepo.resolveStream(urlOrId).onSuccess { resolved ->
                enqueueItem(resolved.item, playNext = playNext)
            }
        }
    }

    fun removeFromQueue(videoId: String) {
        val updated = _state.value.queue.filterNot { it.videoId == videoId }
        prefs.saveQueue(updated)
        _state.update { it.copy(queue = updated) }
    }

    fun selectQuality(quality: VideoQualityOption) {
        val stream = _state.value.currentStream ?: return
        val resumePos = exoPlayer.currentPosition.coerceAtLeast(0L)
        prefs.preferredQualityHeight = quality.height
        _state.update {
            it.copy(
                selectedQuality = quality,
                preferredQualityHeight = quality.height
            )
        }
        prepareAndPlaySource(stream, quality, _state.value.selectedSubtitle, resumePos)
    }

    fun selectSubtitle(subtitle: SubtitleOption?) {
        val stream = _state.value.currentStream ?: return
        val resumePos = exoPlayer.currentPosition.coerceAtLeast(0L)
        _state.update { it.copy(selectedSubtitle = subtitle) }
        prepareAndPlaySource(stream, _state.value.selectedQuality, subtitle, resumePos)
    }

    fun setShareBehavior(behavior: ShareBehavior) {
        prefs.shareBehavior = behavior
        _state.update { it.copy(shareBehavior = behavior) }
    }

    fun setPreferredQualityHeight(height: Int) {
        prefs.preferredQualityHeight = height
        _state.update { it.copy(preferredQualityHeight = height) }
    }

    private fun prepareAndPlaySource(
        resolved: ResolvedStream,
        quality: VideoQualityOption?,
        subtitle: SubtitleOption?,
        startPositionMs: Long
    ) {
        val metadata = MediaMetadata.Builder()
            .setTitle(resolved.item.title)
            .setArtist(resolved.item.uploaderName)
            .setArtworkUri(Uri.parse(resolved.item.thumbnailUrl))
            .build()

        val baseSource: MediaSource = when {
            resolved.isLiveStream && !resolved.hlsUrl.isNullOrBlank() -> {
                val item = MediaItem.Builder()
                    .setUri(resolved.hlsUrl)
                    .setMediaMetadata(metadata)
                    .build()
                HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(item)
            }
            quality != null && quality.isVideoOnly && !resolved.bestAudioUrl.isNullOrBlank() -> {
                val videoItem = MediaItem.Builder()
                    .setUri(quality.videoUrl)
                    .setMediaMetadata(metadata)
                    .build()
                val audioItem = MediaItem.Builder()
                    .setUri(resolved.bestAudioUrl)
                    .build()
                val videoSource = ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(videoItem)
                val audioSource = ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(audioItem)
                MergingMediaSource(videoSource, audioSource)
            }
            quality != null -> {
                val item = MediaItem.Builder()
                    .setUri(quality.videoUrl)
                    .setMediaMetadata(metadata)
                    .build()
                ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(item)
            }
            !resolved.dashMpdUrl.isNullOrBlank() -> {
                val item = MediaItem.Builder()
                    .setUri(resolved.dashMpdUrl)
                    .setMediaMetadata(metadata)
                    .build()
                DashMediaSource.Factory(httpDataSourceFactory).createMediaSource(item)
            }
            !resolved.hlsUrl.isNullOrBlank() -> {
                val item = MediaItem.Builder()
                    .setUri(resolved.hlsUrl)
                    .setMediaMetadata(metadata)
                    .build()
                HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(item)
            }
            else -> {
                _state.update { it.copy(errorMessage = "找不到可播放的影片串流來源") }
                return
            }
        }

        val finalSource = if (subtitle != null && !resolved.isLiveStream) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                .setMimeType(subtitle.mimeType)
                .setLanguage(subtitle.languageCode)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val subSource = SingleSampleMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(subConfig, C.TIME_UNSET)
            MergingMediaSource(baseSource, subSource)
        } else {
            baseSource
        }

        exoPlayer.setMediaSource(finalSource)
        exoPlayer.prepare()
        if (startPositionMs > 0L && !resolved.isLiveStream) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
        showControlsTemporarily()
    }

    fun seekToMs(targetMs: Long) {
        val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: Long.MAX_VALUE
        exoPlayer.seekTo(targetMs.coerceIn(0L, dur))
        _state.update { it.copy(positionMs = targetMs.coerceIn(0L, dur)) }
        showControlsTemporarily()
    }

    /**
     * YouTube signature Double-Tap ±10s seek with cumulative counter (±10s -> ±20s -> ±30s).
     */
    fun seekByDoubleTap(isForward: Boolean) {
        val deltaSec = if (isForward) 10 else -10
        val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: Long.MAX_VALUE
        val target = (exoPlayer.currentPosition + deltaSec * 1000L).coerceIn(0L, dur)
        exoPlayer.seekTo(target)

        val now = System.currentTimeMillis()
        val prev = _state.value.seekFeedback
        val accumulated = if (prev != null && prev.isForward == isForward && (now - prev.triggerTimeMs) < 1200L) {
            prev.accumulatedSeconds + 10
        } else {
            10
        }

        val event = SeekFeedbackEvent(
            accumulatedSeconds = accumulated,
            isForward = isForward,
            triggerTimeMs = now
        )
        _state.update { it.copy(seekFeedback = event, positionMs = target) }

        clearSeekFeedbackJob?.cancel()
        clearSeekFeedbackJob = scope.launch {
            delay(900L)
            _state.update { it.copy(seekFeedback = null) }
        }
    }

    fun playNextInQueueOrRelated() {
        val nextQueued = _state.value.queue.firstOrNull()
        if (nextQueued != null) {
            removeFromQueue(nextQueued.videoId)
            playUrlOrVideo(nextQueued.url, openFullscreenOnCar = _state.value.carViewMode == CarViewMode.FULLSCREEN_PLAYER)
            return
        }
        val firstRelated = _state.value.currentStream?.relatedVideos?.firstOrNull()
        if (firstRelated != null) {
            playUrlOrVideo(firstRelated.url, openFullscreenOnCar = _state.value.carViewMode == CarViewMode.FULLSCREEN_PLAYER)
        }
    }

    fun restartOrPlayPrevious() {
        if (exoPlayer.currentPosition > 5_000L) {
            exoPlayer.seekTo(0L)
            return
        }
        val history = _state.value.history
        if (history.size >= 2) {
            playUrlOrVideo(history[1].url, openFullscreenOnCar = _state.value.carViewMode == CarViewMode.FULLSCREEN_PLAYER)
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    // --- Car UI & Gesture State Transitions ---

    fun setCarProjected(projected: Boolean) {
        _state.update {
            it.copy(
                isCarProjected = projected,
                carViewMode = if (projected && it.currentStream != null) CarViewMode.FULLSCREEN_PLAYER else it.carViewMode
            )
        }
    }

    fun setCarViewMode(mode: CarViewMode) {
        _state.update {
            it.copy(
                carViewMode = mode,
                isRelatedSheetOpen = false,
                isPlayerControlsVisible = mode == CarViewMode.FULLSCREEN_PLAYER
            )
        }
        if (mode == CarViewMode.FULLSCREEN_PLAYER) {
            scheduleAutoHideControls()
        }
    }

    fun setCarBrowseTab(tab: CarBrowseTab) {
        clearSearch()
        _state.update { it.copy(carBrowseTab = tab, carViewMode = CarViewMode.BROWSE) }
    }

    fun setAspectMode(mode: VideoAspectMode) {
        if (_state.value.aspectMode == mode) return
        val event = com.aayt.caster.data.ZoomFeedbackEvent(mode = mode)
        _state.update { it.copy(aspectMode = mode, zoomFeedback = event) }
        clearZoomFeedbackJob?.cancel()
        clearZoomFeedbackJob = scope.launch {
            delay(1300L)
            _state.update { it.copy(zoomFeedback = null) }
        }
    }

    fun toggleAspectMode() {
        val next = if (_state.value.aspectMode == VideoAspectMode.FIT) {
            VideoAspectMode.ZOOM_TO_FILL
        } else {
            VideoAspectMode.FIT
        }
        setAspectMode(next)
    }

    fun togglePlayerControls() {
        val nextVisible = !_state.value.isPlayerControlsVisible
        _state.update { it.copy(isPlayerControlsVisible = nextVisible) }
        if (nextVisible) {
            scheduleAutoHideControls()
        } else {
            autoHideControlsJob?.cancel()
        }
    }

    fun showControlsTemporarily() {
        _state.update { it.copy(isPlayerControlsVisible = true) }
        scheduleAutoHideControls()
    }

    fun setRelatedSheetOpen(open: Boolean) {
        _state.update {
            it.copy(
                isRelatedSheetOpen = open,
                isPlayerControlsVisible = if (open) false else it.isPlayerControlsVisible
            )
        }
    }

    fun toggleTouchLock() {
        _state.update {
            it.copy(
                isTouchLocked = !it.isTouchLocked,
                isPlayerControlsVisible = true
            )
        }
        scheduleAutoHideControls()
    }

    private fun scheduleAutoHideControls() {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = scope.launch {
            delay(3800L)
            if (exoPlayer.isPlaying && !_state.value.isRelatedSheetOpen) {
                _state.update { it.copy(isPlayerControlsVisible = false) }
            }
        }
    }

    private fun addToHistory(item: YtVideoItem) {
        val updated = listOf(item) + _state.value.history.filterNot { it.videoId == item.videoId }
        prefs.saveHistory(updated)
        _state.update { it.copy(history = updated) }
    }

    private fun ensureForegroundServiceStarted() {
        runCatching {
            val intent = Intent(appContext, PlaybackService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }
    }
}
