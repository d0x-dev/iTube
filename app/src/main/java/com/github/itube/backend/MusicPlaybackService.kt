package com.github.itube

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the Android media-session surface for the dedicated music player.  The music UI keeps
 * its existing player state, while this service publishes that exact player to Android so the
 * notification, lock screen, Bluetooth buttons and system media controls work in the background.
 */
class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, MusicPlayerState.playerForSession(this))
            // The video service can be alive at the same time. Media3 rejects two sessions
            // with its default empty ID, which previously crashed the whole process.
            .setId("itube_music")
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
