package com.aayt.caster.car.ui

import android.graphics.Rect
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aayt.caster.data.CarViewMode
import com.aayt.caster.data.PlayerEngineMode
import com.aayt.caster.data.VideoAspectMode
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.ui.theme.AATubeTheme
import com.aayt.caster.ui.web.YoutubeAccountWebPlayer
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
@Composable
fun CarYoutubeRoot(
    controller: ExoPlayerController,
    visibleAreaFlow: StateFlow<Rect>,
    surfaceSizeFlow: StateFlow<Pair<Int, Int>>,
    onOpenCarSearch: () -> Unit
) {
    val state by controller.state.collectAsState()
    val visibleRect by visibleAreaFlow.collectAsState()
    val surfaceSize by surfaceSizeFlow.collectAsState()
    val density = LocalDensity.current

    val topSafePadding = with(density) {
        visibleRect.top.coerceAtLeast(0).toDp()
    }
    val bottomSafePadding = with(density) {
        val bottomInsetPx = (surfaceSize.second - visibleRect.bottom).coerceAtLeast(0)
        if (visibleRect.bottom > 0) bottomInsetPx.toDp() else 0.dp
    }

    AATubeTheme {
        if (state.engineMode == PlayerEngineMode.GOOGLE_ACCOUNT_WEB) {
            YoutubeAccountWebPlayer(
                targetUrl = state.webTargetUrl,
                triggerNonce = state.webUrlTriggerNonce,
                isCarMode = true,
                onSwitchToAdFreeMode = { controller.setEngineMode(PlayerEngineMode.NATIVE_AD_FREE) },
                topSafePadding = topSafePadding,
                bottomSafePadding = bottomSafePadding
            )
            return@AATubeTheme
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val isFullscreen = state.carViewMode == CarViewMode.FULLSCREEN_PLAYER
            val hasStream = state.currentStream != null || state.isLoadingStream

            // 1) Browse Grid Layer (visible whenever in BROWSE mode)
            if (!isFullscreen) {
                CarBrowseView(
                    state = state,
                    controller = controller,
                    onOpenCarSearch = onOpenCarSearch,
                    topSafePadding = topSafePadding,
                    bottomSafePadding = bottomSafePadding
                )
            }

            // 2) Single Persistent Hardware Video Surface!
            // Morphs between Full-Screen Player and Bottom-Right Picture-in-Picture MiniPlayer
            // without ever dropping the ExoPlayer Surface!
            if (hasStream || isFullscreen) {
                val videoModifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomSafePadding + 14.dp)
                        .width(276.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .clickable { controller.setCarViewMode(CarViewMode.FULLSCREEN_PLAYER) }
                }

                Box(modifier = videoModifier.background(Color.Black)) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                player = controller.exoPlayer
                            }
                        },
                        update = { playerView ->
                            if (playerView.player !== controller.exoPlayer) {
                                playerView.player = controller.exoPlayer
                            }
                            playerView.resizeMode = when {
                                !isFullscreen -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                state.aspectMode == VideoAspectMode.ZOOM_TO_FILL ->
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        onRelease = { playerView ->
                            playerView.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // MiniPlayer Compact Bar Overlay when minimized in bottom-right
                    if (!isFullscreen) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0xBF000000))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = state.currentStream?.item?.title ?: "播放中",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { controller.togglePlayPause() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "播放/暫停",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { controller.playNextInQueueOrRelated() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "下一首",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { controller.setCarViewMode(CarViewMode.FULLSCREEN_PLAYER) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = "全螢幕",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3) Fullscreen Gesture & HUD Layer
            if (isFullscreen) {
                CarPlayerOverlay(
                    state = state,
                    controller = controller,
                    topSafePadding = topSafePadding,
                    bottomSafePadding = bottomSafePadding
                )
            }
        }
    }
}
