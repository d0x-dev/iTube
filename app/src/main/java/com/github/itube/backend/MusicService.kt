package com.github.itube

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.github.itube.backend.CoilBitmapLoader
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    companion object {
        const val TAG = "MusicService"
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 2002

        var instance: MusicService? = null
            private set
    }

    private var session: MediaSession? = null
    private var mediaController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this
        ensureNotificationChannel()

        setListener(object : MediaSessionService.Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                Log.w(TAG, "MediaSessionService listener: onForegroundServiceStartNotAllowedException caught and suppressed")
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player,
            ).apply {
                setSmallIcon(R.drawable.ic_stat_music)
            },
        )

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = "ACTION_OPEN_MUSIC"
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val player = MusicPlayerState.getOrCreatePlayer(this)
        session = MediaSession.Builder(this, player)
            .setId("iMusicSession")
            .setSessionActivity(openApp)
            .setBitmapLoader(CoilBitmapLoader(this, serviceScope))
            .setCallback(MusicSessionCallback())
            .build()

        MusicPlayerState.attachMediaSession(session)

        // Keep a connected controller so that notification & media controls work continuously
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                runCatching { mediaController = controllerFuture.get() }
                    .onFailure { Log.w(TAG, "Controller connection error", it) }
            },
            MoreExecutors.directExecutor(),
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun startForegroundService(service: Intent?): ComponentName? {
        return try {
            super.startForegroundService(service)
        } catch (e: Exception) {
            Log.w(TAG, "ForegroundServiceStartNotAllowedException in startForegroundService: ${e.message}")
            null
        }
    }

    override fun startService(service: Intent?): ComponentName? {
        return try {
            super.startService(service)
        } catch (e: Exception) {
            Log.w(TAG, "Exception in startService: ${e.message}")
            null
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val player = session.player
        val hasPlaybackToPromote = player.currentMediaItem != null && player.playWhenReady
        val requiresForeground = startInForegroundRequired && hasPlaybackToPromote
        try {
            super.onUpdateNotification(session, requiresForeground)
        } catch (e: Exception) {
            Log.w(TAG, "Suppressed onUpdateNotification error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!MusicPlayerState.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { mediaController?.release() }
        mediaController = null
        MusicPlayerState.detachMediaSession()
        session?.release()
        session = null
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Music",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Media playback controls for music"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private inner class MusicSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val baseSessionCommands =
                if (connectionResult.availableSessionCommands.commands.isEmpty()) {
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                } else {
                    connectionResult.availableSessionCommands
                }
            val playerCommands =
                if (connectionResult.availablePlayerCommands.size() == 0) {
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                } else {
                    connectionResult.availablePlayerCommands.buildUpon()
                        .addAll(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        .build()
                }

            return MediaSession.ConnectionResult.accept(
                baseSessionCommands,
                playerCommands,
            )
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    mainHandler.post {
                        MusicPlayerState.skipNext()
                    }
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    mainHandler.post {
                        MusicPlayerState.skipPrevious()
                    }
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_PLAY_PAUSE -> {
                    mainHandler.post {
                        if (MusicPlayerState.isPlaying) {
                            MusicPlayerState.pause()
                        } else {
                            MusicPlayerState.resume()
                        }
                    }
                    return SessionResult.RESULT_SUCCESS
                }
                Player.COMMAND_STOP -> {
                    mainHandler.post {
                        MusicPlayerState.close()
                    }
                    return SessionResult.RESULT_SUCCESS
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }
}
