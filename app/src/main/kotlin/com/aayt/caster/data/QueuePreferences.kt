package com.aayt.caster.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QueuePreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("aa_tube_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadQueue(): List<YtVideoItem> = decodeList(KEY_QUEUE)

    fun saveQueue(items: List<YtVideoItem>) {
        prefs.edit().putString(KEY_QUEUE, json.encodeToString(items.take(100))).apply()
    }

    fun loadHistory(): List<YtVideoItem> = decodeList(KEY_HISTORY)

    fun saveHistory(items: List<YtVideoItem>) {
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(items.take(100))).apply()
    }

    var preferredQualityHeight: Int
        get() = prefs.getInt(KEY_QUALITY, 720)
        set(value) = prefs.edit().putInt(KEY_QUALITY, value).apply()

    var shareBehavior: ShareBehavior
        get() {
            val raw = prefs.getString(KEY_SHARE_BEHAVIOR, ShareBehavior.ASK_WHEN_PLAYING.name)
            return ShareBehavior.entries.firstOrNull { it.name == raw } ?: ShareBehavior.ASK_WHEN_PLAYING
        }
        set(value) = prefs.edit().putString(KEY_SHARE_BEHAVIOR, value.name).apply()

    var autoCheckUpdate: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATE, value).apply()

    var engineMode: PlayerEngineMode
        get() {
            val raw = prefs.getString(KEY_ENGINE_MODE, PlayerEngineMode.NATIVE_AD_FREE.name)
            return PlayerEngineMode.entries.firstOrNull { it.name == raw } ?: PlayerEngineMode.NATIVE_AD_FREE
        }
        set(value) = prefs.edit().putString(KEY_ENGINE_MODE, value.name).apply()

    private fun decodeList(key: String): List<YtVideoItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<YtVideoItem>>(raw) }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_QUEUE = "watch_queue"
        private const val KEY_HISTORY = "watch_history"
        private const val KEY_QUALITY = "preferred_quality_height"
        private const val KEY_SHARE_BEHAVIOR = "share_behavior"
        private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
        private const val KEY_ENGINE_MODE = "player_engine_mode"
    }
}
