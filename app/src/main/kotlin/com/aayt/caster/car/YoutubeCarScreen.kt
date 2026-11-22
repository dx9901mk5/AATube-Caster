package com.aayt.caster.car

import androidx.annotation.DrawableRes
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.aayt.caster.R
import com.aayt.caster.data.CarViewMode
import com.aayt.caster.player.ExoPlayerController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class YoutubeCarScreen(
    carContext: CarContext,
    private val controller: ExoPlayerController
) : Screen(carContext) {

    init {
        // Re-render ActionStrip icon state only when play/pause or view mode changes
        controller.state
            .map { it.isPlaying to it.carViewMode }
            .distinctUntilChanged()
            .onEach { invalidate() }
            .launchIn(lifecycleScope)
    }

    fun pushSearchScreen() {
        screenManager.push(YoutubeCarSearchScreen(carContext, controller))
    }

    override fun onGetTemplate(): Template {
        val state = controller.state.value

        // Top/Side System ActionStrip: quick hardware-friendly shortcuts
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(R.drawable.ic_car_search))
                    .setOnClickListener { pushSearchScreen() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(
                        carIcon(
                            if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER) {
                                R.drawable.ic_car_home
                            } else {
                                R.drawable.ic_car_fullscreen
                            }
                        )
                    )
                    .setOnClickListener {
                        val nextMode = if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER) {
                            CarViewMode.BROWSE
                        } else {
                            CarViewMode.FULLSCREEN_PLAYER
                        }
                        controller.setCarViewMode(nextMode)
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(if (state.isPlaying) R.drawable.ic_car_pause else R.drawable.ic_car_play))
                    .setOnClickListener { controller.togglePlayPause() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(carIcon(R.drawable.ic_car_skip_next))
                    .setOnClickListener { controller.playNextInQueueOrRelated() }
                    .build()
            )
            .build()

        // CRITICAL: Including Action.PAN in mapActionStrip instructs Android Auto Host
        // to deliver touchscreen gestures (onClick, onScroll, onFling, onScale) to SurfaceCallback!
        // On touchscreen head units, Android Auto hides the PAN icon automatically.
        val mapActionStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setMapActionStrip(mapActionStrip)
            .build()
    }

    private fun carIcon(@DrawableRes resId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()
}
