package com.tonmoy.ytplayer.util

/**
 * Application-wide constants.
 */
object Constants {

    /** YouTube mobile site URL. */
    const val YOUTUBE_MOBILE_URL = "https://m.youtube.com"

    /** YouTube desktop site URL. */
    const val YOUTUBE_DESKTOP_URL = "https://www.youtube.com"

    // ── GitHub Repository (for in-app updates) ──────────────────────────
    const val GITHUB_OWNER = "nazmaapa98-bit"
    const val GITHUB_REPO = "Youtube-2.0"

    /**
     * Sanitized Chrome mobile User-Agent string.
     * Strips the "; wv" and "Version/4.0" markers that trigger Google's
     * "403: disallowed_useragent" block on OAuth/login flows.
     */
    const val CHROME_MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/128.0.6613.88 Mobile Safari/537.36"

    /** Name for the JavaScript interface bridge injected into the WebView. */
    const val JS_BRIDGE_NAME = "AndroidBridge"

    /** Asset paths for injectable JavaScript files. */
    object JsAssets {
        const val YOUTUBE_BRIDGE = "js/youtube_bridge.js"
        const val ADBLOCK_COSMETIC = "js/adblock_cosmetic.js"
        const val PLAYBACK_CONTROL = "js/playback_control.js"
    }

    /** Asset path for the ad-block hosts list. */
    const val ADBLOCK_HOSTS_ASSET = "adblock_hosts.txt"

    /** Notification channel ID for media playback. */
    const val PLAYBACK_CHANNEL_ID = "yt_player_playback"

    /** WakeLock tag for background audio playback. */
    const val WAKELOCK_TAG = "YTPlayer::BackgroundAudioWakeLock"

    /** Maximum WakeLock duration (3 hours) in milliseconds. */
    const val WAKELOCK_TIMEOUT_MS = 3L * 60L * 60L * 1000L

    /** YouTube video URL regex pattern for video ID extraction. */
    val VIDEO_ID_REGEX = Regex("""(?:v=|shorts/|embed/)([a-zA-Z0-9_-]{11})""")

    /** PiP action intent actions. */
    object PipActions {
        const val ACTION_PLAY = "com.tonmoy.ytplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.tonmoy.ytplayer.ACTION_PAUSE"
    }
}
