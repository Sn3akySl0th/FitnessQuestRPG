package com.fitnessquest.rpg.data.wear

import com.fitnessquest.shared.wear.WearPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Ensures wear messages can wake the phone process; answers HELLO even when no session is open.
 */
class PhoneWearListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearPaths.HELLO) {
            scope.launch {
                runCatching {
                    Wearable.getMessageClient(this@PhoneWearListenerService)
                        .sendMessage(messageEvent.sourceNodeId, WearPaths.HELLO_ACK, ByteArray(0))
                        .await()
                }
            }
        }
        super.onMessageReceived(messageEvent)
    }
}
