package com.fitnessquest.rpg.data.wear

import android.content.Context
import android.util.Log
import com.fitnessquest.shared.wear.HrZone
import com.fitnessquest.shared.wear.WearCapabilities
import com.fitnessquest.shared.wear.WearFeedbackEvent
import com.fitnessquest.shared.wear.WearFeedbackKind
import com.fitnessquest.shared.wear.WearHrSample
import com.fitnessquest.shared.wear.WearLiveMetrics
import com.fitnessquest.shared.wear.WearLogSetCommand
import com.fitnessquest.shared.wear.WearPaths
import com.fitnessquest.shared.wear.WearRestCommand
import com.fitnessquest.shared.wear.WearSessionState
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WearLinkState(
    val watchConnected: Boolean = false,
    val bpm: Int? = null,
    val zone: HrZone? = null,
    val metrics: WearLiveMetrics = WearLiveMetrics(),
    val banner: String? = null
)

/**
 * Phone-side Data Layer bridge for an active workout session.
 * Phone owns session truth; watch is a remote + biometric sensor.
 */
class WearSessionBridge(context: Context) : MessageClient.OnMessageReceivedListener {

    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messageClient by lazy { Wearable.getMessageClient(app) }
    private val nodeClient by lazy { Wearable.getNodeClient(app) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(app) }

    private val _link = MutableStateFlow(WearLinkState())
    val link: StateFlow<WearLinkState> = _link

    private val _logSetCommands = MutableSharedFlow<WearLogSetCommand>(extraBufferCapacity = 8)
    val logSetCommands: SharedFlow<WearLogSetCommand> = _logSetCommands

    private val _restCommands = MutableSharedFlow<WearRestCommand>(extraBufferCapacity = 8)
    val restCommands: SharedFlow<WearRestCommand> = _restCommands

    private var bound = false
    private var discoverJob: Job? = null
    private val hrWindow = ArrayDeque<Int>()
    private var lastZone: HrZone? = null
    private var resting = false
    private var heatStreak = 0
    private var lastRecoveryCue = false
    private var lastCalorieCue = 0
    private var maxHr: Int = HrZone.estimatedMaxHr()
    private var lastPublishedState: WearSessionState? = null
    private var requestedWatchLaunchForSession = false

    fun bind() {
        if (bound) return
        bound = true
        messageClient.addListener(this)
        discoverJob = scope.launch { refreshWatchPresence() }
    }

    fun unbind() {
        if (!bound) return
        bound = false
        messageClient.removeListener(this)
        discoverJob?.cancel()
        scope.launch {
            runCatching {
                sendToWear(WearPaths.SESSION_ENDED, ByteArray(0))
            }
        }
        _link.value = WearLinkState()
        hrWindow.clear()
        lastRecoveryCue = false
        lastCalorieCue = 0
        lastPublishedState = null
        requestedWatchLaunchForSession = false
    }

    fun publishSession(state: WearSessionState) {
        resting = state.restEndsAt != null
        heatStreak = state.heatStreak
        maxHr = state.maxHr ?: HrZone.estimatedMaxHr(state.ageYears ?: 30)
        if (!resting) lastRecoveryCue = false
        scope.launch {
            refreshWatchPresence()
            val m = _link.value.metrics
            val enriched = state.copy(
                heartRateBpm = _link.value.bpm ?: m.bpm,
                hrZone = _link.value.zone?.label ?: m.zone,
                watchLinked = _link.value.watchConnected,
                maxHr = maxHr
            )
            lastPublishedState = enriched
            if (enriched.active && !requestedWatchLaunchForSession) {
                requestedWatchLaunchForSession = true
                sendToWear(WearPaths.OPEN_APP, ByteArray(0))
            } else if (!enriched.active) {
                requestedWatchLaunchForSession = false
            }
            sendToWear(WearPaths.SESSION_STATE, enriched.toJson())
        }
    }

    fun pushFeedback(kind: WearFeedbackKind, message: String) {
        _link.update { it.copy(banner = message) }
        scope.launch {
            sendToWear(WearPaths.FEEDBACK, WearFeedbackEvent(kind, message).toJson())
        }
    }

    fun clearBanner() = _link.update { it.copy(banner = null) }

    fun sessionHrStats(): Pair<Int?, Int?> {
        if (hrWindow.isEmpty()) return null to null
        return (hrWindow.sum() / hrWindow.size) to hrWindow.maxOrNull()
    }

    fun noteSetLogged() {
        pushFeedback(WearFeedbackKind.SET_LOGGED, "Set forged. Your heart fuels the next strike.")
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearPaths.LOG_SET -> {
                runCatching { WearLogSetCommand.fromJson(event.data) }
                    .onSuccess { _logSetCommands.tryEmit(it) }
            }
            WearPaths.REST -> {
                runCatching { WearRestCommand.fromJson(event.data) }
                    .onSuccess { _restCommands.tryEmit(it) }
            }
            WearPaths.METRICS -> {
                runCatching { WearLiveMetrics.fromJson(event.data) }
                    .onSuccess { onMetrics(it) }
            }
            WearPaths.HR_SAMPLE -> {
                runCatching { WearHrSample.fromJson(event.data) }
                    .onSuccess { sample ->
                        if (sample.bpm > 0) {
                            onMetrics(WearLiveMetrics(bpm = sample.bpm, atMillis = sample.atMillis))
                        }
                    }
            }
            WearPaths.HELLO -> {
                _link.update { it.copy(watchConnected = true) }
                scope.launch {
                    runCatching {
                        messageClient
                            .sendMessage(event.sourceNodeId, WearPaths.HELLO_ACK, ByteArray(0))
                            .await()
                    }
                    lastPublishedState?.let { state ->
                        runCatching {
                            messageClient
                                .sendMessage(event.sourceNodeId, WearPaths.SESSION_STATE, state.toJson())
                                .await()
                        }
                    }
                }
            }
        }
    }

    private fun onMetrics(metrics: WearLiveMetrics) {
        val bpm = metrics.bpm
        if (bpm != null && bpm > 0) {
            hrWindow.addLast(bpm)
            while (hrWindow.size > 60) hrWindow.removeFirst()
        }
        val zone = metrics.zone?.let { label ->
            HrZone.entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
        } ?: bpm?.let { HrZone.fromBpm(it, maxHr) }

        val previous = lastZone
        if (zone != null) lastZone = zone

        _link.update {
            it.copy(
                bpm = bpm ?: it.bpm,
                zone = zone ?: it.zone,
                metrics = metrics,
                watchConnected = true
            )
        }

        if (previous != null && zone != null && previous != zone) {
            when {
                zone == HrZone.HIGH -> pushFeedback(
                    WearFeedbackKind.ZONE_HIGH,
                    "High heart! Breathe — the forge runs hot."
                )
                resting && (zone == HrZone.WORK || zone == HrZone.HIGH) -> pushFeedback(
                    WearFeedbackKind.ZONE_RECOVER,
                    "Recover on the rest. Soften the flame."
                )
                zone == HrZone.WORK && heatStreak >= 3 -> pushFeedback(
                    WearFeedbackKind.HEAT,
                    "Work zone + heat streak. The realm feels your pace."
                )
                else -> Unit
            }
        }

        if (resting && metrics.restHrGoalMet && !lastRecoveryCue) {
            lastRecoveryCue = true
            pushFeedback(
                WearFeedbackKind.RECOVERY_READY,
                "Heart recovered — ready for the next strike."
            )
        }

        val kcal = metrics.caloriesKcal?.toInt() ?: 0
        if (kcal >= 50 && kcal / 50 > lastCalorieCue / 50) {
            lastCalorieCue = kcal
            pushFeedback(
                WearFeedbackKind.CALORIES,
                "$kcal kcal forged in this quest."
            )
        }

        metrics.goalLabel?.takeIf { it.contains("steps", ignoreCase = true) }?.let { label ->
            // Watch already haptics; mirror a light phone banner occasionally.
            if (label != _link.value.banner) {
                // Avoid spam — only when milestone text changes and includes "forged"
                if (label.contains("forged")) {
                    _link.update { it.copy(banner = label) }
                }
            }
        }
    }

    private suspend fun refreshWatchPresence() {
        val nodes = runCatching {
            capabilityClient
                .getCapability(WearCapabilities.WEAR, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
        }.getOrElse {
            runCatching { nodeClient.connectedNodes.await().toSet() }.getOrDefault(emptySet())
        }
        _link.update { it.copy(watchConnected = nodes.isNotEmpty()) }
    }

    private suspend fun sendToWear(path: String, data: ByteArray) {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
        if (nodes.isEmpty()) {
            Log.d(TAG, "No wear nodes for $path")
            return
        }
        nodes.forEach { node ->
            runCatching { messageClient.sendMessage(node.id, path, data).await() }
                .onFailure { Log.w(TAG, "send $path failed: ${it.message}") }
        }
        _link.update { it.copy(watchConnected = true) }
    }

    companion object {
        private const val TAG = "WearBridge"
    }
}
