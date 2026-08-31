package com.tonmoy.ytplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for YT Player.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class YTPlayerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // NewPipe Extractor is initialized lazily in AudioExtractor
        // to avoid slow startup on the main thread.
    }
}
