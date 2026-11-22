package com.aayt.caster.car.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aayt.caster.data.CarBrowseTab
import com.aayt.caster.data.YtVideoItem
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.player.PlaybackUiState

@Composable
fun CarBrowseView(
    state: PlaybackUiState,
    controller: ExoPlayerController,
    onOpenCarSearch: () -> Unit,
    topSafePadding: Dp,
    bottomSafePadding: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = topSafePadding, bottom = bottomSafePadding)
    ) {
        // Left Car-sized Navigation Rail (Big touch targets for in-car ergonomics)
        Column(
            modifier = Modifier
                .width(108.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCarSearch() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "語音搜尋", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("搜尋", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                CarRailTabButton(
                    icon = Icons.Default.Home,
                    label = "熱門",
                    selected = state.searchQuery.isBlank() && state.carBrowseTab == CarBrowseTab.HOME,
                    onClick = { controller.setCarBrowseTab(CarBrowseTab.HOME) }
                )
                CarRailTabButton(
                    icon = Icons.Default.LiveTv,
                    label = "新聞直播",
                    selected = state.searchQuery.isBlank() && state.carBrowseTab == CarBrowseTab.LIVE_CHANNELS,
                    onClick = { controller.setCarBrowseTab(CarBrowseTab.LIVE_CHANNELS) }
                )
                CarRailTabButton(
                    icon = Icons.Default.QueueMusic,
                    label = "待播 (${state.queue.size})",
                    selected = state.searchQuery.isBlank() && state.carBrowseTab == CarBrowseTab.QUEUE,
                    onClick = { controller.setCarBrowseTab(CarBrowseTab.QUEUE) }
                )
                CarRailTabButton(
                    icon = Icons.Default.History,
                    label = "紀錄",
                    selected = state.searchQuery.isBlank() && state.carBrowseTab == CarBrowseTab.HISTORY,
                    onClick = { controller.setCarBrowseTab(CarBrowseTab.HISTORY) }
                )
            }

            IconButton(
                onClick = { controller.refreshFeeds() },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "重新整理", tint = Color.White)
            }
        }

        // Right Main Touchable YouTube Video Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Header / Active Search Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                if (state.searchQuery.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔍 搜尋結果：「${state.searchQuery}」",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { controller.clearSearch() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" 清除搜尋", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "AA Tube • ${state.carBrowseTab.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            val itemsToShow: List<YtVideoItem> = when {
                state.searchQuery.isNotBlank() -> state.searchResults
                state.carBrowseTab == CarBrowseTab.HOME -> state.homeFeed
                state.carBrowseTab == CarBrowseTab.LIVE_CHANNELS -> state.liveFeed
                state.carBrowseTab == CarBrowseTab.QUEUE -> state.queue
                state.carBrowseTab == CarBrowseTab.HISTORY -> state.history
                else -> emptyList()
            }

            if (state.isSearching || (itemsToShow.isEmpty() && state.carBrowseTab == CarBrowseTab.HOME)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("正在載入無廣告 YouTube 內容...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (itemsToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "尚無影片項目，可透過手機原生 YouTube「分享」或點擊左上角「搜尋」！",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 230.dp),
                    contentPadding = PaddingValues(bottom = 110.dp), // Leave room for MiniPlayer in bottom-right
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(itemsToShow, key = { it.videoId }) { video ->
                        CarVideoGridCard(
                            item = video,
                            onClick = { controller.playItem(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarRailTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun CarVideoGridCard(
    item: YtVideoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                        color = if (item.isLive) MaterialTheme.colorScheme.primary else Color(0xD9000000),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = item.formattedDuration,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 21.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(item.uploaderName, item.viewCountText)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
