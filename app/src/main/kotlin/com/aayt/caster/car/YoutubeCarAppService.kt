package com.aayt.caster.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class YoutubeCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allow both Android Auto physical head units and Desktop Head Unit (DHU) simulator
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return YoutubeCarSession()
    }
}
