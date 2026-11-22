package com.aayt.caster.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aayt.caster.AAYtApplication
import com.aayt.caster.data.ShareBehavior
import com.aayt.caster.data.YoutubeUrlParser
import com.aayt.caster.ui.theme.AATubeTheme

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }

        val canonicalUrl = YoutubeUrlParser.extractCanonicalUrl(sharedText)
        if (canonicalUrl == null) {
            Toast.makeText(this, "未偵測到有效的 YouTube 影片連結", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val controller = AAYtApplication.from(this).playerController
        val currentState = controller.state.value
        val hasActivePlayback = currentState.currentStream != null && currentState.isPlaying

        when {
            !hasActivePlayback || currentState.shareBehavior == ShareBehavior.PLAY_IMMEDIATELY -> {
                controller.playUrlOrVideo(canonicalUrl, openFullscreenOnCar = true)
                val targetHint = if (currentState.isCarProjected) "車機螢幕" else "AA Tube"
                Toast.makeText(this, "🎬 已無廣告投放至 $targetHint 播放", Toast.LENGTH_SHORT).show()
                finish()
            }
            currentState.shareBehavior == ShareBehavior.ENQUEUE_NEXT -> {
                controller.enqueueSharedUrl(canonicalUrl, playNext = true)
                Toast.makeText(this, "➕ 已排入車機下一首待播清單", Toast.LENGTH_SHORT).show()
                finish()
            }
            else -> {
                setContent {
                    AATubeTheme {
                        AlertDialog(
                            onDismissRequest = { finish() },
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = "投放到 AA Tube 車載影音",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    Text("目前正在播放：${currentState.currentStream?.item?.title.orEmpty()}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "請選擇要如何處理新分享的 YouTube 影片：",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            controller.enqueueSharedUrl(canonicalUrl, playNext = true)
                                            Toast.makeText(
                                                this@ShareReceiverActivity,
                                                "➕ 已加入下一首待播",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                        Text(" 排入下一首", modifier = Modifier.padding(start = 4.dp))
                                    }
                                    Button(
                                        onClick = {
                                            controller.playUrlOrVideo(canonicalUrl, openFullscreenOnCar = true)
                                            Toast.makeText(
                                                this@ShareReceiverActivity,
                                                "🎬 立即插播至車機",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Text(" 立即插播", modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { finish() }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
