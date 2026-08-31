package com.tonmoy.ytplayer.playback

import android.content.Context
import android.os.PowerManager
import com.tonmoy.ytplayer.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages partial wake lock for background audio playback.
 */
@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Acquires a partial wake lock to keep the CPU running for background audio.
     */
    fun acquire() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK_TAG).apply {
                setReferenceCounted(false)
            }
        }
        
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(Constants.WAKELOCK_TIMEOUT_MS)
            }
        }
    }

    /**
     * Releases the acquired wake lock.
     */
    fun release() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
