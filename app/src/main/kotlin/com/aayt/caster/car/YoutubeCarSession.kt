package com.aayt.caster.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aayt.caster.AAYtApplication

class YoutubeCarSession : Session() {

    private var surfaceBridge: SurfaceBridge? = null
    private var mainCarScreen: YoutubeCarScreen? = null

    override fun onCreateScreen(intent: Intent): Screen {
        val controller = AAYtApplication.from(carContext).playerController
        val screen = YoutubeCarScreen(carContext, controller)
        mainCarScreen = screen

        val bridge = SurfaceBridge(
            carContext = carContext,
            controller = controller,
            onOpenCarSearch = { mainCarScreen?.pushSearchScreen() }
        )
        surfaceBridge = bridge

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                bridge.register()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                bridge.unregister()
                surfaceBridge = null
                mainCarScreen = null
            }
        })

        return screen
    }
}
