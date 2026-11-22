package com.aayt.caster.car

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.DisplayMetrics
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.aayt.caster.player.ExoPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SurfaceBridge(
    private val carContext: CarContext,
    private val controller: ExoPlayerController,
    private val onOpenCarSearch: () -> Unit
) : SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarYoutubePresentation? = null

    private val _visibleAreaFlow = MutableStateFlow(Rect())
    val visibleAreaFlow = _visibleAreaFlow.asStateFlow()

    private val _surfaceSizeFlow = MutableStateFlow(1280 to 720)
    val surfaceSizeFlow = _surfaceSizeFlow.asStateFlow()

    private val gestureDispatcher = CarTouchGestureDispatcher(
        controller = controller,
        decorViewProvider = { presentation?.window?.decorView }
    )

    fun register() {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    fun unregister() {
        runCatching {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
        }
        releaseSurface()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        val width = surfaceContainer.width.takeIf { it > 0 } ?: 1280
        val height = surfaceContainer.height.takeIf { it > 0 } ?: 720
        val dpi = surfaceContainer.dpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_HIGH

        releaseSurface()

        gestureDispatcher.updateSurfaceSize(width, height)
        _surfaceSizeFlow.value = width to height

        val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val vd = displayManager.createVirtualDisplay(
            "AATubeCarDisplay",
            width,
            height,
            dpi,
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        virtualDisplay = vd

        val pres = CarYoutubePresentation(
            outerContext = carContext,
            display = vd.display,
            controller = controller,
            visibleAreaFlow = visibleAreaFlow,
            surfaceSizeFlow = surfaceSizeFlow,
            onOpenCarSearch = onOpenCarSearch
        )
        pres.show()
        presentation = pres
        controller.setCarProjected(true)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        _visibleAreaFlow.value = Rect(visibleArea)
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        if (_visibleAreaFlow.value.isEmpty) {
            _visibleAreaFlow.value = Rect(stableArea)
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        releaseSurface()
    }

    override fun onClick(x: Float, y: Float) {
        gestureDispatcher.onClick(x, y)
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        gestureDispatcher.onScroll(distanceX, distanceY)
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        gestureDispatcher.onFling(velocityX, velocityY)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        gestureDispatcher.onScale(focusX, focusY, scaleFactor)
    }

    private fun releaseSurface() {
        runCatching { presentation?.dismiss() }
        presentation = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        controller.setCarProjected(false)
    }
}
