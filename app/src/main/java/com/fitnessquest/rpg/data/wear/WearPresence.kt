package com.fitnessquest.rpg.data.wear

import android.app.Application
import android.util.Log
import com.fitnessquest.shared.wear.WearCapabilities
import com.fitnessquest.shared.wear.WearPaths
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WearPresenceState(
    val watchLinked: Boolean = false,
    val watchName: String? = null,
    val nodeCount: Int = 0,
    val lastHelloAt: Long? = null,
    val statusText: String = "Looking for watch…"
)

/**
 * App-wide watch presence (not only during workouts).
 * Answers watch HELLO pings and exposes link status for Settings / Hero.
 */
class WearPresence(app: Application) : MessageClient.OnMessageReceivedListener {

    private val appCtx = app.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageClient by lazy { Wearable.getMessageClient(appCtx) }
    private val nodeClient by lazy { Wearable.getNodeClient(appCtx) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(appCtx) }

    private val _state = MutableStateFlow(WearPresenceState())
    val state: StateFlow<WearPresenceState> = _state

    private var started = false
    private var pollJob: Job? = null

    fun start() {
        if (started) return
        started = true
        runCatching { messageClient.addListener(this) }
        runCatching {
            capabilityClient.addListener(
                {
                    scope.launch { refresh() }
                },
                WearCapabilities.WEAR
            )
        }
        pollJob = scope.launch {
            while (isActive) {
                refresh()
                delay(5_000)
            }
        }
    }

    fun refreshNow() {
        scope.launch { refresh() }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearPaths.HELLO -> {
                scope.launch {
                    runCatching {
                        messageClient
                            .sendMessage(messageEvent.sourceNodeId, WearPaths.HELLO_ACK, ByteArray(0))
                            .await()
                    }
                    _state.update {
                        it.copy(
                            watchLinked = true,
                            lastHelloAt = System.currentTimeMillis(),
                            statusText = "Watch linked"
                        )
                    }
                    refresh()
                }
            }
        }
    }

    private suspend fun refresh() {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
        val wearCap = runCatching {
            capabilityClient
                .getCapability(WearCapabilities.WEAR, CapabilityClient.FILTER_REACHABLE)
                .await()
        }.getOrNull()
        val wearNodes = wearCap?.nodes.orEmpty()
        val linked = wearNodes.isNotEmpty()
        val name = wearNodes.firstOrNull()?.displayName
            ?: nodes.firstOrNull()?.displayName
        _state.update {
            it.copy(
                watchLinked = linked,
                watchName = name,
                nodeCount = nodes.size,
                statusText = when {
                    linked -> "Watch linked${name?.let { n -> " · $n" } ?: ""}"
                    nodes.isNotEmpty() -> "Wear network up — open FitnessRPG on the watch"
                    else -> "No watch reachable — open the watch app"
                }
            )
        }
        Log.d(TAG, "presence linked=$linked nodes=${nodes.size} wearCap=${wearNodes.size}")
    }

    companion object {
        private const val TAG = "WearPresence"
    }
}
