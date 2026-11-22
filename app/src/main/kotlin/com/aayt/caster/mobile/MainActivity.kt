package com.aayt.caster.mobile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.aayt.caster.AAYtApplication
import com.aayt.caster.data.ShareBehavior
import com.aayt.caster.data.YoutubeUrlParser
import com.aayt.caster.data.YtVideoItem
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.player.PlaybackUiState
import com.aayt.caster.ui.theme.AATubeTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Optional notification permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val controller = AAYtApplication.from(this).playerController

        setContent {
            AATubeTheme {
                MobileCompanionRoot(controller = controller)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileCompanionRoot(controller: ExoPlayerController) {
    val state by controller.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AA Tube 車載無廣告影音", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Surface(
                        color = if (state.isCarProjected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (state.isCarProjected) "🚗 車機投影中" else "📱 手機待機",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text("搜尋/投放") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    label = { Text("待播佇列 (${state.queue.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("播放設定") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("免Root上車") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Player / Car Remote Card
            if (state.currentStream != null || state.isLoadingStream) {
                MobileNowPlayingSection(
                    state = state,
                    controller = controller
                )
            }

            state.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> MobileExploreTab(state = state, controller = controller)
                    1 -> MobileQueueTab(state = state, controller = controller)
                    2 -> MobileSettingsTab(state = state, controller = controller)
                    3 -> MobileInstallGuideTab()
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MobileNowPlayingSection(
    state: PlaybackUiState,
    controller: ExoPlayerController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (state.isLoadingStream) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在無廣告解析 YouTube 高畫質串流...")
                }
            } else {
                val stream = state.currentStream ?: return@Column

                // Only attach local PlayerView when NOT projecting to Android Auto
                // so the car VirtualDisplay always owns the hardware video surface exclusively!
                if (!state.isCarProjected) {
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
                        update = { view ->
                            if (view.player !== controller.exoPlayer) {
                                view.player = controller.exoPlayer
                            }
                        },
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Surface(
                        color = Color(0xFF102815),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFF69F0AE)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "畫面已投射至車機螢幕（手機端轉為省電遙控模式）",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB9F6CA)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = stream.item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stream.item.uploaderName} • ${state.selectedQuality?.label ?: "自動畫質"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!stream.isLiveStream && state.durationMs > 0L) {
                    Slider(
                        value = (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f),
                        onValueChange = { ratio ->
                            controller.seekToMs((ratio * state.durationMs).toLong())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { controller.seekByDoubleTap(isForward = false) }) {
                        Icon(Icons.Default.FastRewind, contentDescription = "倒退 10 秒")
                    }
                    IconButton(onClick = { controller.togglePlayPause() }) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暫停",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { controller.seekByDoubleTap(isForward = true) }) {
                        Icon(Icons.Default.FastForward, contentDescription = "快轉 10 秒")
                    }
                    IconButton(onClick = { controller.playNextInQueueOrRelated() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "下一部")
                    }
                    AssistChip(
                        onClick = { controller.toggleAspectMode() },
                        label = { Text(state.aspectMode.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileExploreTab(
    state: PlaybackUiState,
    controller: ExoPlayerController
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("輸入關鍵字搜尋，或貼上 YouTube 網址直接投放") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = {
                            inputText = ""
                            controller.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        val directUrl = YoutubeUrlParser.extractCanonicalUrl(inputText)
                        if (directUrl != null) {
                            controller.playUrlOrVideo(directUrl, openFullscreenOnCar = true)
                        } else {
                            controller.searchYoutube(inputText)
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    keyboard?.hide()
                    val directUrl = YoutubeUrlParser.extractCanonicalUrl(inputText)
                    if (directUrl != null) {
                        controller.playUrlOrVideo(directUrl, openFullscreenOnCar = true)
                    } else {
                        controller.searchYoutube(inputText)
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("GO")
            }
        }

        // Quick preset search tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("台灣新聞直播", "華語流行音樂 MV", "Podcast 訪談", "Lofi 音樂").forEach { tag ->
                AssistChip(
                    onClick = {
                        inputText = tag
                        controller.searchYoutube(tag)
                    },
                    label = { Text(tag) }
                )
            }
        }

        val displayList = if (state.searchQuery.isNotBlank()) state.searchResults else state.homeFeed

        if (state.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayList, key = { it.videoId }) { item ->
                    MobileVideoRow(
                        item = item,
                        onPlay = { controller.playItem(item) },
                        onEnqueue = {
                            controller.enqueueItem(item)
                            Toast.makeText(context, "已加入待播佇列", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileVideoRow(
    item: YtVideoItem,
    onPlay: () -> Unit,
    onEnqueue: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
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
                        color = if (item.isLive) MaterialTheme.colorScheme.primary else Color(0xCC000000),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = item.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(item.uploaderName, item.viewCountText)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onEnqueue != null) {
                IconButton(onClick = onEnqueue) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "加入待播佇列")
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "移除")
                }
            }
        }
    }
}

@Composable
private fun MobileQueueTab(
    state: PlaybackUiState,
    controller: ExoPlayerController
) {
    if (state.queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "目前待播佇列為空\n您可以在原生 YouTube App 按「分享 -> 投放到車機」隨時加入！",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.queue, key = { it.videoId }) { item ->
                MobileVideoRow(
                    item = item,
                    onPlay = { controller.playItem(item) },
                    onRemove = { controller.removeFromQueue(item.videoId) }
                )
            }
        }
    }
}

@Composable
private fun MobileSettingsTab(
    state: PlaybackUiState,
    controller: ExoPlayerController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("預設串流畫質上限", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(1080 to "1080p 高清", 720 to "720p 省電順暢", 480 to "480p 省流量").forEach { (h, label) ->
                FilterChip(
                    selected = state.preferredQualityHeight == h,
                    onClick = { controller.setPreferredQualityHeight(h) },
                    label = { Text(label) }
                )
            }
        }

        Text(
            "從原生 YouTube App「分享」時的行為",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        ShareBehavior.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { controller.setShareBehavior(option) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = state.shareBehavior == option,
                    onClick = { controller.setShareBehavior(option) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(option.title)
            }
        }
    }
}

@Composable
private fun MobileInstallGuideTab() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Android 14 / 15 / 16 免 Root 實車顯示指南",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("步驟 1：開啟 Android Auto「未知的來源」", fontWeight = FontWeight.Bold)
                Text(
                    "1. 打開手機「設定 -> 連接的裝置 -> 連線偏好設定 -> Android Auto」。\n" +
                        "2. 滑到最底部連點 10 次「版本」啟用開發人員模式。\n" +
                        "3. 點右上角「⋮ -> 開發人員設定」，勾選「未知的來源 (Unknown sources)」。"
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("步驟 2：標記為 Google Play 安裝來源（二選一）", fontWeight = FontWeight.Bold)
                Text(
                    "Android 14+ 會檢查 APK 的安裝器名稱是否為 Google Play (com.android.vending)：\n\n" +
                        "• 方法 A（免費免帳號）：使用 Shizuku 搭配「Install with Options」安裝本 APK，在 Installer 欄位填入 com.android.vending。\n" +
                        "• 方法 B（最穩定）：將打包檔案上傳至 Google Play Console「內部應用程式分享（免審核）」，透過產生的連結一鍵安裝。"
                )
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("installer", "com.android.vending"))
                        Toast.makeText(context, "已複製 com.android.vending", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("複製 com.android.vending")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("車機螢幕支援的 YouTube 原生手勢", fontWeight = FontWeight.Bold)
                Text(
                    "• 單擊畫面：顯示／隱藏播放控制列與時間軸\n" +
                        "• 雙擊左側／右側：快退／快轉 10 秒（支援連續累加 ±10s/20s/30s）\n" +
                        "• 雙指張開／捏合：切換「填滿超寬螢幕」與「原始 16:9」\n" +
                        "• 全螢幕底部上滑：拉出「相關推薦影片 & 待播清單」抽屜\n" +
                        "• 全螢幕往下滑：縮小為右下角「子母畫面 MiniPlayer」繼續逛首頁"
                )
            }
        }
    }
}
