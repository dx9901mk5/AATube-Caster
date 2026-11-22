package com.aayt.caster

import android.app.Application
import android.content.Context
import com.aayt.caster.data.OkHttpNewPipeDownloader
import com.aayt.caster.data.QueuePreferences
import com.aayt.caster.data.YoutubeExtractorRepo
import com.aayt.caster.player.ExoPlayerController
import com.aayt.caster.shizuku.ShizukuAaManager
import com.aayt.caster.update.UpdateManager
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class AAYtApplication : Application() {

    lateinit var extractorRepo: YoutubeExtractorRepo
        private set

    lateinit var queuePreferences: QueuePreferences
        private set

    lateinit var playerController: ExoPlayerController
        private set

    lateinit var shizukuManager: ShizukuAaManager
        private set

    lateinit var updateManager: UpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(
            OkHttpNewPipeDownloader(),
            Localization("zh", "TW"),
            ContentCountry("TW")
        )

        extractorRepo = YoutubeExtractorRepo()
        queuePreferences = QueuePreferences(this)
        playerController = ExoPlayerController(
            appContext = this,
            extractorRepo = extractorRepo,
            prefs = queuePreferences
        )
        shizukuManager = ShizukuAaManager(this).apply { init() }
        updateManager = UpdateManager(this, shizukuManager)
    }

    companion object {
        fun from(context: Context): AAYtApplication =
            context.applicationContext as AAYtApplication
    }
}
