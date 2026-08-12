package com.fitnessquest.rpg.wear

import android.content.Intent
import android.util.Log
import com.fitnessquest.shared.wear.WearPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Keeps the wear process eligible for Data Layer callbacks when the activity is not foreground.
 * Session UI still owns parsing via [WearSessionViewModel]; this service is a capability anchor.
 */
class WearDataListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearPaths.OPEN_APP) {
            runCatching {
                startActivity(
                    Intent(this, WearMainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
            }.onFailure {
                Log.w(TAG, "Unable to open wear activity: ${it.message}")
            }
            return
        }
        // Activity listener handles live session; service ensures delivery wakeups.
        super.onMessageReceived(messageEvent)
    }

    companion object {
        private const val TAG = "WearDataListener"
    }
}
