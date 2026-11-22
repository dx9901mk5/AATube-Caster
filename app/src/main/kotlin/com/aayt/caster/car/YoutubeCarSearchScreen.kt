package com.aayt.caster.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.aayt.caster.player.ExoPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Car-native Voice & Keyboard Search Screen (SearchTemplate).
 * Selecting a result immediately starts ad-free playback on the Car Surface!
 */
class YoutubeCarSearchScreen(
    carContext: CarContext,
    private val controller: ExoPlayerController
) : Screen(carContext) {

    private var debounceJob: Job? = null

    init {
        controller.state
            .onEach { invalidate() }
            .launchIn(lifecycleScope)
    }

    override fun onGetTemplate(): Template {
        val state = controller.state.value
        val listBuilder = ItemList.Builder()
            .setNoItemsMessage("請點擊鍵盤或語音麥克風輸入關鍵字搜尋 YouTube")

        state.searchResults.take(6).forEach { item ->
            val subtitle = listOf(item.uploaderName, item.formattedDuration)
                .filter { it.isNotBlank() }
                .joinToString(" • ")

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(item.title)
                    .addText(subtitle)
                    .setOnClickListener {
                        controller.playItem(item)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    debounceJob?.cancel()
                    if (searchText.trim().length >= 2) {
                        debounceJob = lifecycleScope.launch {
                            delay(550L)
                            controller.searchYoutube(searchText)
                        }
                    }
                }

                override fun onSearchSubmitted(searchTerm: String) {
                    debounceJob?.cancel()
                    if (searchTerm.isNotBlank()) {
                        controller.searchYoutube(searchTerm)
                        // Pop back to touchable CarBrowseView grid if user wants visual thumbnails!
                        screenManager.pop()
                    }
                }
            }
        )
            .setHeaderAction(Action.BACK)
            .setShowKeyboardByDefault(true)
            .setSearchHint("語音或打字搜尋 YouTube 影片 / 直播...")
            .setLoading(state.isSearching)
            .setItemList(listBuilder.build())
            .build()
    }
}
