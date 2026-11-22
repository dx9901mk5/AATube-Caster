package com.aayt.caster.car

import android.app.Presentation
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aayt.caster.car.ui.CarYoutubeRoot
import com.aayt.caster.player.ExoPlayerController
import kotlinx.coroutines.flow.StateFlow

class CarYoutubePresentation(
    outerContext: Context,
    display: Display,
    private val controller: ExoPlayerController,
    private val visibleAreaFlow: StateFlow<Rect>,
    private val surfaceSizeFlow: StateFlow<Pair<Int, Int>>,
    private val onOpenCarSearch: () -> Unit
) : Presentation(outerContext, display),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            decorView.setViewTreeLifecycleOwner(this@CarYoutubePresentation)
            decorView.setViewTreeViewModelStoreOwner(this@CarYoutubePresentation)
            decorView.setViewTreeSavedStateRegistryOwner(this@CarYoutubePresentation)
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@CarYoutubePresentation)
            setViewTreeViewModelStoreOwner(this@CarYoutubePresentation)
            setViewTreeSavedStateRegistryOwner(this@CarYoutubePresentation)
            setContent {
                CarYoutubeRoot(
                    controller = controller,
                    visibleAreaFlow = visibleAreaFlow,
                    surfaceSizeFlow = surfaceSizeFlow,
                    onOpenCarSearch = onOpenCarSearch
                )
            }
        }

        setContentView(composeView)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onStop()
    }
}
