package com.fitnessquest.rpg.data.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null
)

class MediaControllerManager(private val context: Context) {

    private val sessionManager = runCatching {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }.getOrNull()
    private var activeController: MediaController? = null

    private val _state = MutableStateFlow(MediaState())
    val state: StateFlow<MediaState> = _state

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateState()
        }
    }

    fun start() {
        findActiveSession()
    }

    fun stop() {
        activeController?.unregisterCallback(callback)
        activeController = null
    }

    fun togglePlayPause() {
        val controller = activeController ?: return
        if (state.value.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    private fun findActiveSession() {
        val sm = sessionManager ?: return
        try {
            // This requires NotificationListenerService permission or being the system UI
            val sessions = sm.getActiveSessions(
                ComponentName(context, "com.fitnessquest.rpg.data.media.NotificationService") // Placeholder or actual service
            )
            val controller = sessions.firstOrNull { 
                it.packageName.contains("music", ignoreCase = true) || 
                it.packageName.contains("youtube", ignoreCase = true) ||
                it.packageName.contains("spotify", ignoreCase = true)
            } ?: sessions.firstOrNull()
            
            if (controller != activeController) {
                activeController?.unregisterCallback(callback)
                activeController = controller
                activeController?.registerCallback(callback)
                updateState()
            }
        } catch (e: Exception) {
            // Cannot get sessions without permission or restricted by device policy
        }
    }

    private fun updateState() {
        val controller = activeController
        val metadata = controller?.metadata
        val playback = controller?.playbackState
        
        _state.value = MediaState(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            packageName = controller?.packageName
        )
    }
}
