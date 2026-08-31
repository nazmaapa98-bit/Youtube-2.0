package com.tonmoy.ytplayer.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Media3 MediaSessionService for background audio playback.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var wakeLockManager: WakeLockManager? = null

    override fun onCreate() {
        super.onCreate()
        
        wakeLockManager = WakeLockManager(this.applicationContext)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateWakeLockState(isPlaying, playbackState)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateWakeLockState(isPlaying, playbackState)
                    }
                })
            }

        mediaSession = exoPlayer?.let {
            MediaSession.Builder(this, it).build()
        }
    }
    
    private fun updateWakeLockState(isPlaying: Boolean, playbackState: Int) {
        if (isPlaying && playbackState == Player.STATE_READY) {
            wakeLockManager?.acquire()
        } else if (!isPlaying || playbackState == Player.STATE_IDLE) {
            wakeLockManager?.release()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        wakeLockManager?.release()
        wakeLockManager = null
        super.onDestroy()
    }

    companion object {
        /**
         * Builds a MediaItem with the given audio stream information.
         */
        fun buildMediaItem(title: String, channelName: String, streamUrl: String, thumbnailUrl: String, duration: Long): MediaItem {
            return MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(channelName)
                        .setArtworkUri(Uri.parse(thumbnailUrl))
                        .build()
                )
                .build()
        }
    }
}
