package com.cyberkalki.n250v2

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.content.ComponentName

class MediaListener : NotificationListenerService() {
    companion object { @Volatile var instance: MediaListener? = null }
    override fun onListenerConnected() { super.onListenerConnected(); instance = this }
    override fun onListenerDisconnected() { instance = null; super.onListenerDisconnected() }
    override fun onDestroy() { instance = null; super.onDestroy() }
    fun transport(command: String) {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(ComponentName(this, MediaListener::class.java))
            val controller = sessions.firstOrNull() ?: return
            val c = controller.transportControls
            when(command) {
                "play" -> {
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING) c.pause() else c.play()
                }
                "pause" -> c.pause()
                "next" -> c.skipToNext()
                "previous" -> c.skipToPrevious()
            }
        } catch (_: SecurityException) { }
    }
}