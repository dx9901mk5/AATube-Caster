package com.aayt.caster.car

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.aayt.caster.data.CarViewMode
import com.aayt.caster.data.PlayerEngineMode
import com.aayt.caster.data.VideoAspectMode
import com.aayt.caster.player.ExoPlayerController
import kotlin.math.abs

/**
 * Translates Android Auto [androidx.car.app.SurfaceCallback] gestures
 * (`onClick`, `onScroll`, `onFling`, `onScale`) into:
 * 1) Synthetic [MotionEvent] streams dispatched to [CarYoutubePresentation]'s Compose DecorView
 *    so grids, lists, buttons, chips, and sliders respond natively to touch & drag.
 * 2) Native YouTube Fullscreen Player gestures:
 *    - Single tap -> show/hide player controls HUD
 *    - Double tap left / right -> -10s / +10s cumulative seek with ripple animation
 *    - Pinch out / in -> Zoom-to-Fill ultrawide vs Fit 16:9
 *    - Swipe up -> open Related Videos & Up Next drawer
 *    - Swipe down -> minimize into bottom-right MiniPlayer & browse videos
 *    - Horizontal drag -> scrub timeline
 */
class CarTouchGestureDispatcher(
    private val controller: ExoPlayerController,
    private val decorViewProvider: () -> View?,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    var surfaceWidth: Int = 1280
        private set
    var surfaceHeight: Int = 720
        private set

    // Single vs Double tap state
    private var lastTapTimeMs: Long = 0L
    private var lastTapX: Float = 0f
    private var lastTapY: Float = 0f
    private var pendingSingleTapRunnable: Runnable? = null

    // Continuous scroll / swipe tracking
    private var isScrolling = false
    private var scrollDownTimeMs: Long = 0L
    private var virtualCursorX: Float = 0f
    private var virtualCursorY: Float = 0f
    private var accumulatedDx: Float = 0f
    private var accumulatedDy: Float = 0f
    private var playerGestureHandledInBurst = false
    private val endScrollRunnable = Runnable { finishSyntheticScroll() }

    // Pinch scale tracking
    private var pinchScaleAccumulator: Float = 1.0f
    private var lastScaleEventTimeMs: Long = 0L

    fun updateSurfaceSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            surfaceWidth = width
            surfaceHeight = height
        }
    }

    fun onClick(x: Float, y: Float) {
        val state = controller.state.value

        // In Google Account WebView mode, pass all taps straight to WebView & top bar
        if (state.engineMode == PlayerEngineMode.GOOGLE_ACCOUNT_WEB) {
            dispatchSyntheticTap(x, y)
            return
        }

        // When touch-locked in fullscreen player, only allow tapping the top-right unlock area or revealing lock hint
        if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER && state.isTouchLocked) {
            if (y < surfaceHeight * 0.24f) {
                dispatchSyntheticTap(x, y)
            } else {
                controller.showControlsTemporarily()
            }
            return
        }

        // In Browse mode OR when Related Videos drawer is open -> dispatch tap directly to Compose UI
        if (state.carViewMode == CarViewMode.BROWSE || state.isRelatedSheetOpen) {
            dispatchSyntheticTap(x, y)
            return
        }

        // In Fullscreen Player mode:
        // If controls HUD is visible and tap lands on Top Bar, Center Transport Buttons, or Bottom Seekbar/Chips,
        // dispatch immediately to Compose so UI buttons & seekbar respond with 0ms delay!
        if (state.isPlayerControlsVisible && isInsideHudInteractiveZone(x, y)) {
            dispatchSyntheticTap(x, y)
            controller.showControlsTemporarily()
            return
        }

        val now = SystemClock.uptimeMillis()
        val isLeftZone = x < surfaceWidth * 0.38f
        val isRightZone = x > surfaceWidth * 0.62f

        // Fast cumulative tap while ±10s ripple is already visible on the same side
        val activeSeek = state.seekFeedback
        if (activeSeek != null && ((isLeftZone && !activeSeek.isForward) || (isRightZone && activeSeek.isForward))) {
            pendingSingleTapRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingSingleTapRunnable = null
            controller.seekByDoubleTap(isForward = isRightZone)
            return
        }

        // Detect rapid Double-Tap on Left / Right 38% of screen
        val dt = now - lastTapTimeMs
        val closeEnough = hypotRough(x - lastTapX, y - lastTapY) < surfaceWidth * 0.22f
        if (dt in 40L..320L && closeEnough && (isLeftZone || isRightZone)) {
            pendingSingleTapRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingSingleTapRunnable = null
            lastTapTimeMs = 0L
            controller.seekByDoubleTap(isForward = isRightZone)
            return
        }

        lastTapTimeMs = now
        lastTapX = x
        lastTapY = y

        pendingSingleTapRunnable?.let { mainHandler.removeCallbacks(it) }
        val singleTapTask = Runnable {
            pendingSingleTapRunnable = null
            controller.togglePlayerControls()
        }
        pendingSingleTapRunnable = singleTapTask
        mainHandler.postDelayed(singleTapTask, 260L)
    }

    /**
     * Note: SurfaceCallback.onScroll(distanceX, distanceY) follows GestureDetector convention:
     * distanceX = prevX - currX (positive when finger moves left),
     * distanceY = prevY - currY (positive when finger moves up).
     */
    fun onScroll(distanceX: Float, distanceY: Float) {
        val state = controller.state.value
        if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER && state.isTouchLocked) return

        val dx = -distanceX
        val dy = -distanceY

        mainHandler.removeCallbacks(endScrollRunnable)

        val isWebMode = state.engineMode == PlayerEngineMode.GOOGLE_ACCOUNT_WEB

        if (!isScrolling) {
            isScrolling = true
            playerGestureHandledInBurst = false
            accumulatedDx = 0f
            accumulatedDy = 0f
            scrollDownTimeMs = SystemClock.uptimeMillis()
            virtualCursorX = surfaceWidth * 0.5f
            virtualCursorY = if (state.isRelatedSheetOpen) surfaceHeight * 0.72f else surfaceHeight * 0.5f

            if (isWebMode || state.carViewMode == CarViewMode.BROWSE || state.isRelatedSheetOpen) {
                dispatchMotion(MotionEvent.ACTION_DOWN, scrollDownTimeMs, scrollDownTimeMs, virtualCursorX, virtualCursorY)
            }
        }

        accumulatedDx += dx
        accumulatedDy += dy

        if (isWebMode || state.carViewMode == CarViewMode.BROWSE || state.isRelatedSheetOpen) {
            virtualCursorX = (virtualCursorX + dx).coerceIn(10f, surfaceWidth - 10f)
            virtualCursorY = (virtualCursorY + dy).coerceIn(10f, surfaceHeight - 10f)
            dispatchMotion(
                MotionEvent.ACTION_MOVE,
                scrollDownTimeMs,
                SystemClock.uptimeMillis(),
                virtualCursorX,
                virtualCursorY
            )
        } else if (!playerGestureHandledInBurst) {
            val thresholdY = (surfaceHeight * 0.13f).coerceAtLeast(72f)
            val thresholdX = (surfaceWidth * 0.10f).coerceAtLeast(90f)

            when {
                // Swipe UP -> Open Related Videos & Queue Drawer (just like YouTube fullscreen!)
                accumulatedDy < -thresholdY && abs(accumulatedDy) > abs(accumulatedDx) * 1.3f -> {
                    playerGestureHandledInBurst = true
                    controller.setRelatedSheetOpen(true)
                }
                // Swipe DOWN -> Minimize player into bottom-right MiniPlayer & return to Browse
                accumulatedDy > thresholdY && abs(accumulatedDy) > abs(accumulatedDx) * 1.3f -> {
                    playerGestureHandledInBurst = true
                    controller.setCarViewMode(CarViewMode.BROWSE)
                }
                // Horizontal swipe -> Fine timeline seek (±45s full-width sweep)
                abs(accumulatedDx) > thresholdX && abs(accumulatedDx) > abs(accumulatedDy) * 1.5f -> {
                    val stream = state.currentStream
                    if (stream != null && !stream.isLiveStream && state.durationMs > 0L) {
                        val deltaMs = ((dx / surfaceWidth) * 60_000f).toLong()
                        controller.seekToMs(state.positionMs + deltaMs)
                    }
                }
            }
        }

        mainHandler.postDelayed(endScrollRunnable, 140L)
    }

    fun onFling(velocityX: Float, velocityY: Float) {
        val state = controller.state.value
        if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER && state.isTouchLocked) return

        if (state.carViewMode == CarViewMode.FULLSCREEN_PLAYER && !state.isRelatedSheetOpen && !playerGestureHandledInBurst) {
            if (velocityY < -900f && abs(velocityY) > abs(velocityX) * 1.3f) {
                playerGestureHandledInBurst = true
                controller.setRelatedSheetOpen(true)
            } else if (velocityY > 900f && abs(velocityY) > abs(velocityX) * 1.3f) {
                playerGestureHandledInBurst = true
                controller.setCarViewMode(CarViewMode.BROWSE)
            }
        }
    }

    fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        val state = controller.state.value
        if (state.carViewMode != CarViewMode.FULLSCREEN_PLAYER || state.isTouchLocked || state.isRelatedSheetOpen) {
            return
        }

        val now = SystemClock.uptimeMillis()

        // Android Auto Host converts rapid Double-Tap on surface into onScale(focusX, focusY, 2.0f)!
        if (scaleFactor >= 1.85f && (now - lastScaleEventTimeMs) > 250L) {
            val isLeft = focusX < surfaceWidth * 0.42f
            val isRight = focusX > surfaceWidth * 0.58f
            if (isLeft || isRight) {
                controller.seekByDoubleTap(isForward = isRight)
                lastScaleEventTimeMs = now
                return
            } else {
                controller.toggleAspectMode()
                lastScaleEventTimeMs = now
                return
            }
        }

        // Continuous two-finger Pinch-to-Zoom -> Toggle Ultrawide Fill vs 16:9 Fit
        if (now - lastScaleEventTimeMs > 450L) {
            pinchScaleAccumulator = 1.0f
        }
        lastScaleEventTimeMs = now
        pinchScaleAccumulator *= scaleFactor

        if (pinchScaleAccumulator > 1.16f) {
            controller.setAspectMode(VideoAspectMode.ZOOM_TO_FILL)
            pinchScaleAccumulator = 1.0f
        } else if (pinchScaleAccumulator < 0.85f) {
            controller.setAspectMode(VideoAspectMode.FIT)
            pinchScaleAccumulator = 1.0f
        }
    }

    private fun isInsideHudInteractiveZone(x: Float, y: Float): Boolean {
        // Top header bar (back to MiniPlayer, quality/subtitle chips, touch lock)
        if (y <= surfaceHeight * 0.22f) return true
        // Bottom progress bar & action buttons
        if (y >= surfaceHeight * 0.74f) return true
        // Center Play/Pause & Prev/Next buttons
        val inCenterY = y in (surfaceHeight * 0.36f)..(surfaceHeight * 0.64f)
        val inCenterX = x in (surfaceWidth * 0.28f)..(surfaceWidth * 0.72f)
        return inCenterX && inCenterY
    }

    private fun dispatchSyntheticTap(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        dispatchMotion(MotionEvent.ACTION_DOWN, downTime, downTime, x, y)
        mainHandler.postDelayed({
            dispatchMotion(MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), x, y)
        }, 25L)
    }

    private fun finishSyntheticScroll() {
        if (!isScrolling) return
        isScrolling = false
        val state = controller.state.value
        if (state.engineMode == PlayerEngineMode.GOOGLE_ACCOUNT_WEB ||
            state.carViewMode == CarViewMode.BROWSE ||
            state.isRelatedSheetOpen
        ) {
            dispatchMotion(
                MotionEvent.ACTION_UP,
                scrollDownTimeMs,
                SystemClock.uptimeMillis(),
                virtualCursorX,
                virtualCursorY
            )
        }
    }

    private fun dispatchMotion(action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) {
        val view = decorViewProvider() ?: return
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun hypotRough(dx: Float, dy: Float): Float = abs(dx) + abs(dy)
}
