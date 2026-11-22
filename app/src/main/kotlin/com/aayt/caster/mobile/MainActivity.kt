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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.aayt.caster.AAYtApplication
import com.aayt.caster.BuildConfig
import com.aayt.caster.data.QueuePreferences
import com.aayt.caster.data.ShareBehavior
import com.aayt.caster.data.YoutubeUrlParser
import com.aayt.caster.data.YtVideoItem
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.player.PlaybackUiState
import com.aayt.caster.shizuku.ShizukuAaManager
import com.aayt.caster.shizuku.ShizukuUiState
import com.aayt.caster.ui.theme.AATubeTheme
import com.aayt.caster.update.UpdateManager
import com.aayt.caster.update.UpdateUiState

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Refreshed automatically on ON_RESUME */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val app = AAYtApplication.from(this)

        setContent {
            AATubeTheme {
                MobileCompanionRoot(
                    controller = app.playerController,
                    updateManager = app.updateManager,
                    shizukuManager = app.shizukuManager,
                    prefs = app.queuePreferences,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionHelper.openNotificationSettings(this)
                        }
                    }
                )
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
fun MobileCompanionRoot(
    controller: ExoPlayerController,
    updateManager: UpdateManager,
    shizukuManager: ShizukuAaManager,
    prefs: QueuePreferences,
    onRequestNotificationPermission: () -> Unit
) {
    val state by controller.state.collectAsState()
    val updateState by updateManager.uiState.collectAsState()
    val shizukuState by shizukuManager.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permStatus by remember { mutableStateOf(PermissionHelper.checkStatus(context)) }

    // Auto-refresh permission statuses, Shizuku binder, & resume pending APK install whenever user returns!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permStatus = PermissionHelper.checkStatus(context)
                shizukuManager.refreshAndBind()
                updateManager.resumePendingInstallIfPermitted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Silent background update check on startup if enabled
    LaunchedEffect(Unit) {
        if (prefs.autoCheckUpdate) {
            updateManager.checkForUpdates(silentIfUpToDate = true)
        }
    }

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
                        Text("AA Tube 車載影音", fontWeight = FontWeight.Bold)
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
                    icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                    label = { Text("待播 (${state.queue.size})") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("設定/權限") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("上車教學") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top New Version Banner if update detected while on non-settings tab
            val availableUpdate = (updateState as? UpdateUiState.UpdateAvailable)?.info
            if (availableUpdate != null && selectedTab != 2) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { selectedTab = 2 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "發現新版本 v${availableUpdate.latestVersion}！點擊前往一鍵更新",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }

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
                    2 -> MobileSettingsTab(
                        state = state,
                        controller = controller,
                        permStatus = permStatus,
                        shizukuState = shizukuState,
                        shizukuManager = shizukuManager,
                        updateState = updateState,
                        updateManager = updateManager,
                        prefs = prefs,
                        onRequestNotificationPermission = onRequestNotificationPermission
                    )
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
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "加入待播佇列")
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
    controller: ExoPlayerController,
    permStatus: AppPermissionStatus,
    shizukuState: ShizukuUiState,
    shizukuManager: ShizukuAaManager,
    updateState: UpdateUiState,
    updateManager: UpdateManager,
    prefs: QueuePreferences,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var autoCheck by remember { mutableStateOf(prefs.autoCheckUpdate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==================== SECTION 0: 內建 Shizuku 一鍵寫入 AA App 辨識 ====================
        Text(
            text = "Shizuku 一鍵寫入 Android Auto 辨識",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val statusText = when {
                    !shizukuState.isInstalled -> "❌ 未安裝 Shizuku App"
                    !shizukuState.binderAlive -> "⚠️ Shizuku 尚未啟動（請先透過無線偵錯啟動 Shizuku）"
                    !shizukuState.permissionGranted -> "🔐 Shizuku 運行中，尚未授權本 App"
                    shizukuState.serviceBound -> "✅ Shizuku 特權服務已連線就緒 (UID 2000)"
                    else -> "⏳ 正在連線 Shizuku 特權服務..."
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (shizukuState.serviceBound) Color(0xFF69F0AE) else Color(0xFFFFB74D)
                )

                Text(
                    text = "若 Android Auto 看不到「AA Tube 車載影音」，請點擊下方按鈕執行內建 Shizuku 腳本：自動寫入 Play 商店安裝來源 (com.android.vending)、省電與背景媒體權限，並重整 Android Auto (gearhead) 快取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    !shizukuState.binderAlive -> {
                        Button(
                            onClick = { shizukuManager.openShizukuApp() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("前往開啟 Shizuku App")
                        }
                    }
                    !shizukuState.permissionGranted -> {
                        Button(
                            onClick = { shizukuManager.requestPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("立即授權 Shizuku 權限")
                        }
                    }
                    else -> {
                        Button(
                            onClick = { shizukuManager.runAaRecognitionScript(forceReinstallPlayStore = true) },
                            enabled = !shizukuState.isRunningScript,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (shizukuState.isRunningScript) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在寫入 AA 辨識腳本...")
                            } else {
                                Text("⚡ 執行 Shizuku 腳本：寫入 AA App 辨識", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                shizukuState.lastScriptOutput?.let { output ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = output,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // ==================== SECTION 1: 權限檢查與一鍵導向開啟 ====================
        Text(
            text = "權限與車機環境檢查（點擊直接前往設定）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PermissionRowItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "媒體播放與更新通知權限",
                    subtitle = if (permStatus.notificationsGranted) {
                        "已開啟 • 確保背景車載播放服務穩定常駐"
                    } else {
                        "尚未開啟 • 點擊前往開啟通知權限"
                    },
                    isGranted = permStatus.notificationsGranted,
                    onClick = onRequestNotificationPermission
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                PermissionRowItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "背景省電無限制（忽略電池最佳化）",
                    subtitle = if (permStatus.batteryOptimizationIgnored) {
                        "已設為無限制 • 手機鎖屏投影車機不中斷"
                    } else {
                        "受省電限制 • 點擊一鍵允許背景無限制執行"
                    },
                    isGranted = permStatus.batteryOptimizationIgnored,
                    onClick = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                PermissionRowItem(
                    icon = Icons.Default.InstallMobile,
                    title = "允許安裝未知應用程式（自動更新用）",
                    subtitle = if (permStatus.installUnknownAppsGranted) {
                        "已授權 • 下載新版 APK 後可直接進入安裝"
                    } else {
                        "尚未授權 • 點擊前往開啟，讓更新包可自動安裝"
                    },
                    isGranted = permStatus.installUnknownAppsGranted,
                    onClick = { PermissionHelper.openInstallUnknownAppsSettings(context) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                PermissionRowItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Android Auto 安裝來源驗證",
                    subtitle = if (permStatus.isPlayStoreInstaller) {
                        "已識別為 Google Play (com.android.vending)"
                    } else {
                        "目前來源：${permStatus.installerPackageName ?: "系統預設"}（點擊複製 com.android.vending）"
                    },
                    isGranted = permStatus.isPlayStoreInstaller,
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("installer", "com.android.vending"))
                        Toast.makeText(
                            context,
                            "已複製 com.android.vending（請搭配 Shizuku 安裝器或電腦 ADB 腳本安裝）",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                PermissionRowItem(
                    icon = Icons.Default.DirectionsCar,
                    title = "Android Auto 系統設定與自訂啟動器",
                    subtitle = if (permStatus.isAndroidAutoInstalled) {
                        "點擊開啟 Android Auto 設定（檢查「未知的來源」與「自訂啟動器」）"
                    } else {
                        "未偵測到 Android Auto，點擊開啟連接裝置設定"
                    },
                    isGranted = permStatus.isAndroidAutoInstalled,
                    onClick = { PermissionHelper.openAndroidAutoSettings(context) }
                )
            }
        }

        // ==================== SECTION 2: 播放與分享偏好 ====================
        Text(
            text = "播放與分享設定",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("預設串流畫質上限", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1080 to "1080p 高清", 720 to "720p 順暢", 480 to "480p 省流").forEach { (h, label) ->
                        FilterChip(
                            selected = state.preferredQualityHeight == h,
                            onClick = { controller.setPreferredQualityHeight(h) },
                            label = { Text(label) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text("從原生 YouTube App「分享」時的預設行為", fontWeight = FontWeight.SemiBold)
                ShareBehavior.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { controller.setShareBehavior(option) }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = state.shareBehavior == option,
                            onClick = { controller.setShareBehavior(option) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(option.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // ==================== SECTION 3: 自動檢查更新 & GitHub 專案連結 ====================
        Text(
            text = "版本更新與開源專案",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("啟動時自動檢查更新", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "目前安裝版本：v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCheck,
                        onCheckedChange = {
                            autoCheck = it
                            prefs.autoCheckUpdate = it
                        }
                    )
                }

                // Check for Updates Button
                Button(
                    onClick = { updateManager.checkForUpdates(silentIfUpToDate = false) },
                    enabled = updateState !is UpdateUiState.Checking && updateState !is UpdateUiState.Downloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (updateState is UpdateUiState.Checking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在連線 GitHub 檢查最新版本...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("檢查更新 (Check for Updates)")
                    }
                }

                // Dynamic Update Status Area
                when (updateState) {
                    is UpdateUiState.Idle -> Unit

                    is UpdateUiState.Checking -> Unit

                    is UpdateUiState.UpToDate -> {
                        Surface(
                            color = Color(0xFF132A17),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "✅ 目前已是最新版本 (v${updateState.currentVersion})",
                                    color = Color(0xFFA5D6A7),
                                    fontWeight = FontWeight.Bold
                                )
                                updateState.latestRelease?.let { latest ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { updateManager.downloadAndInstall(latest) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("重新下載並覆蓋安裝 v${latest.latestVersion}")
                                    }
                                }
                            }
                        }
                    }

                    is UpdateUiState.UpdateAvailable -> {
                        val info = updateState.info
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎉 發現新版本：v${info.latestVersion}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (info.changelog.isNotBlank()) {
                                    Text(
                                        text = info.changelog,
                                        color = Color(0xFFFFD9DE),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 8,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = { updateManager.downloadAndInstall(info) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("立即自動下載並進入安裝", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateUiState.Downloading -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sizeText = if (updateState.totalMb > 0f) {
                                    "%.1f MB / %.1f MB (%d%%)".format(
                                        updateState.downloadedMb,
                                        updateState.totalMb,
                                        updateState.progressPercent
                                    )
                                } else {
                                    "已下載 %.1f MB".format(updateState.downloadedMb)
                                }
                                Text(
                                    text = "⬇️ 正在下載 v${updateState.info.latestVersion} 更新包... $sizeText",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(
                                    progress = { (updateState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    is UpdateUiState.WaitingInstallPermission -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "APK 已下載完成！請先開啟「允許安裝未知應用程式」權限即可自動安裝：",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { PermissionHelper.openInstallUnknownAppsSettings(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("前往開啟安裝權限")
                                }
                            }
                        }
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        Button(
                            onClick = { updateManager.triggerPackageInstaller(updateState.apkFile) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.InstallMobile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("再次啟動安裝 v${updateState.info.latestVersion}")
                        }
                    }

                    is UpdateUiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = updateState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Bottom GitHub Repository Link Row
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { PermissionHelper.openGitHubRepo(context, UpdateManager.GITHUB_REPO_URL) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GitHub 開源儲存庫 (AATube-Caster)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "github.com/dx9901mk5/AATube-Caster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "開啟 GitHub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isGranted) Color(0xFF14331B) else Color(0xFF3B2314),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF69F0AE) else Color(0xFFFFB74D),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF69F0AE) else Color(0xFFFFB74D),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                        "3. 點右上角「⋮ -> 開發人員設定」，將應用程式模式設為「開發人員」並勾選「未知的來源 (Unknown sources)」。"
                )
                OutlinedButton(onClick = { PermissionHelper.openAndroidAutoSettings(context) }) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("直接打開 Android Auto 設定")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("步驟 2：標記為 Google Play 安裝來源", fontWeight = FontWeight.Bold)
                Text(
                    "Android 14+ 會檢查 APK 的安裝器名稱是否為 Google Play (com.android.vending)：\n\n" +
                        "• 方法 A（手機端 Shizuku）：使用 InstallerX Revived 或 Install with Options 安裝本 APK，將 Installer 設為 com.android.vending。\n" +
                        "• 方法 B（電腦端 ADB）：接上電腦執行專案內的 Install-ToPhone.ps1 腳本一鍵完成。"
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
