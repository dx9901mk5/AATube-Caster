package com.aayt.caster.car.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aayt.caster.data.CarViewMode
import com.aayt.caster.data.YtVideoItem
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.player.PlaybackUiState

@Composable
fun CarPlayerOverlay(
    state: PlaybackUiState,
    controller: ExoPlayerController,
    topSafePadding: Dp,
    bottomSafePadding: Dp
) {
    var showQualityMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Loading / Buffering indicator
        if (state.isLoadingStream || state.isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // YouTube Signature Left / Right Double-Tap ±10s Ripple Feedback!
        val seekEvent = state.seekFeedback
        AnimatedVisibility(
            visible = seekEvent != null && !seekEvent.isForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.82f)
                .fillMaxWidth(0.36f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                    .background(Color(0x66FFFFFF))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "-${seekEvent?.accumulatedSeconds ?: 10} 秒",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekEvent != null && seekEvent.isForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.82f)
                .fillMaxWidth(0.36f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                    .background(Color(0x66FFFFFF))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "+${seekEvent?.accumulatedSeconds ?: 10} 秒",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Pinch-to-Zoom Aspect Ratio Feedback Pill
        AnimatedVisibility(
            visible = state.zoomFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topSafePadding + 24.dp)
        ) {
            Surface(
                color = Color(0xCC000000),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "🔍 ${state.zoomFeedback?.mode?.label.orEmpty()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }

        // Touch-Lock Shield Mode (prevents accidental bumps in car)
        if (state.isTouchLocked) {
            AnimatedVisibility(
                visible = state.isPlayerControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topSafePadding + 14.dp, end = 18.dp)
            ) {
                Surface(
                    color = Color(0xCC1E1E1E),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.clickable { controller.toggleTouchLock() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("螢幕已鎖定（點擊解鎖）", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Box
        }

        // Full YouTube HUD Controls Overlay
        AnimatedVisibility(
            visible = state.isPlayerControlsVisible && !state.isRelatedSheetOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB3000000),
                                Color(0x33000000),
                                Color(0xCC000000)
                            )
                        )
                    )
                    .padding(top = topSafePadding, bottom = bottomSafePadding)
            ) {
                // Top Bar: Minimize to MiniPlayer + Title + Quality / Subtitle / Aspect / Lock Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = { controller.setCarViewMode(CarViewMode.BROWSE) },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "縮小為子母畫面",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentStream?.item?.title ?: "準備播放中...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.currentStream?.item?.uploaderName.orEmpty(),
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    // Quality Chip
                    HudPillButton(
                        icon = Icons.Default.Hd,
                        text = state.selectedQuality?.label ?: "畫質",
                        onClick = { showQualityMenu = !showQualityMenu }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Subtitles toggle
                    val hasSubs = !state.currentStream?.subtitles.isNullOrEmpty()
                    if (hasSubs) {
                        HudPillButton(
                            icon = Icons.Default.ClosedCaption,
                            text = if (state.selectedSubtitle != null) "字幕:開" else "字幕:關",
                            active = state.selectedSubtitle != null,
                            onClick = {
                                val nextSub = if (state.selectedSubtitle != null) {
                                    null
                                } else {
                                    state.currentStream?.subtitles?.firstOrNull()
                                }
                                controller.selectSubtitle(nextSub)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Aspect Ratio Toggle
                    HudPillButton(
                        icon = Icons.Default.AspectRatio,
                        text = state.aspectMode.label,
                        onClick = { controller.toggleAspectMode() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Touch Lock Button
                    IconButton(
                        onClick = { controller.toggleTouchLock() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "防誤觸鎖定", tint = Color.White)
                    }
                }

                // Floating Quality Picker Row if toggled
                if (showQualityMenu) {
                    val qualities = state.currentStream?.qualities.orEmpty()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 64.dp, end = 20.dp)
                            .background(Color(0xE61E1E1E), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        qualities.forEach { q ->
                            Surface(
                                color = if (state.selectedQuality?.height == q.height) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    controller.selectQuality(q)
                                    showQualityMenu = false
                                }
                            ) {
                                Text(
                                    text = q.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Center Large Transport Buttons (Previous / Play-Pause / Next)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    IconButton(
                        onClick = { controller.restartOrPlayPrevious() },
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color(0x73000000), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "上一首",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = { controller.togglePlayPause() },
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暫停",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    IconButton(
                        onClick = { controller.playNextInQueueOrRelated() },
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color(0x73000000), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "下一首",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom Timeline Seekbar + Swipe-Up Related Videos Trigger
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    val isLive = state.currentStream?.isLiveStream == true
                    if (!isLive && state.durationMs > 0L) {
                        Slider(
                            value = (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                controller.seekToMs((frac * state.durationMs).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLive) {
                                "🔴 LIVE 即時直播"
                            } else {
                                "${formatMillis(state.positionMs)} / ${formatMillis(state.durationMs)}"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Surface(
                            color = Color(0x802A2A2A),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.clickable { controller.setRelatedSheetOpen(true) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White)
                                Text(
                                    text = " 上滑開啟相關推薦 / 待播清單",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swipe-Up Related Videos & Queue Drawer (Authentic YouTube Fullscreen Up-Swipe Sheet!)
        AnimatedVisibility(
            visible = state.isRelatedSheetOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val combinedList = state.queue + state.currentStream?.relatedVideos.orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .background(
                        Color(0xF2141414),
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                    )
                    .padding(top = 12.dp, bottom = bottomSafePadding + 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.queue.isNotEmpty()) {
                            "接下來播放（待播 ${state.queue.size} 部 + 相關推薦）"
                        } else {
                            "相關推薦影片"
                        },
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { controller.setRelatedSheetOpen(false) }
                    ) {
                        Text(
                            text = "收合 ✕",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(combinedList, key = { it.videoId }) { item ->
                        RelatedHorizontalCard(
                            item = item,
                            onClick = { controller.playItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary else Color(0x801F1F1F),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RelatedHorizontalCard(
    item: YtVideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.formattedDuration.isNotBlank()) {
                    Surface(
                        color = Color(0xCC000000),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = item.formattedDuration,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.uploaderName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
