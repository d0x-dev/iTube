package com.github.itube

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Android's one music session and notification owner.
 *
 * This follows AirBeats' lifecycle: the app binds this service before playback, then Media3
 * promotes its own notification only once the shared player is actually ready. Starting a
 * foreground service after audio has already begun races Android's deadline and was the source
 * of the ForegroundServiceDidNotStartInTime crash.
 */
class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setListener(object : MediaSessionService.Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                // A bound service retries with the next player update instead of crashing on
                // Android 12+ background-start restrictions.
            }
        })
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.itube)
            },
        )
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, MusicPlayerState.playerForSession(this))
            .setId("itube_music")
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        MusicPlayerState.onServiceDestroyed()
        session?.release()
        session = null
        super.onDestroy()
    }
}
