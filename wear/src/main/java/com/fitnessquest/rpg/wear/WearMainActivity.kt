package com.fitnessquest.rpg.wear

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.wear.ambient.AmbientModeSupport
import com.fitnessquest.shared.wear.WearCapabilities
import com.fitnessquest.shared.wear.WearExerciseState
import com.fitnessquest.shared.wear.WearFeedbackEvent
import com.fitnessquest.shared.wear.WearFeedbackKind
import com.fitnessquest.shared.wear.WearLiveMetrics
import com.fitnessquest.shared.wear.WearLogSetCommand
import com.fitnessquest.shared.wear.WearPaths
import com.fitnessquest.shared.wear.WearRestAction
import com.fitnessquest.shared.wear.WearRestCommand
import com.fitnessquest.shared.wear.WearSessionState
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearMainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    private val viewModel: WearSessionViewModel by viewModels {
        WearSessionViewModel.Factory(application)
    }

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = object : AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            // Optional: update UI for ambient mode
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val hrGranted = results[WearExerciseEngine.hrPermission()] == true ||
            ContextCompat.checkSelfPermission(this, WearExerciseEngine.hrPermission()) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onSensorPermissionResult(hrGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ambientController = AmbientModeSupport.attach(this)
        
        setContent {
            val state by viewModel.uiState.collectAsState()
            WearApp(
                state = state,
                onSelectExercise = viewModel::selectExercise,
                onLogSet = viewModel::logSet,
                onSkipRest = { viewModel.sendRest(WearRestAction.SKIP) },
                onExtendRest = { viewModel.sendRest(WearRestAction.EXTEND, 30) },
                onDismissFeedback = viewModel::dismissFeedback,
                onAdjustWeight = viewModel::adjustWeight,
                onAdjustReps = viewModel::adjustReps,
                onAdjustDuration = viewModel::adjustDuration,
                onAdjustDistance = viewModel::adjustDistance,
                onRetryLink = viewModel::retryLink,
                onRequestHrPermission = { ensurePermissions(force = true) }
            )
            // Keep the watch screen alive while a quest is active.
            if (state.session.active) {
                DisposableEffect(Unit) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        ensurePermissions(force = false)
    }

    private fun ensurePermissions(force: Boolean) {
        val needed = WearExerciseEngine.requiredRuntimePermissions(includeLocation = true)
            .filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (needed.isEmpty()) {
            viewModel.onSensorPermissionResult(true)
        } else if (force || needed.any { shouldShowRequestPermissionRationale(it) } || true) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bind()
    }

    override fun onStop() {
        viewModel.unbind()
        super.onStop()
    }
}

data class WearUiState(
    val session: WearSessionState = WearSessionState(
        active = false,
        title = "FitnessRPG",
        imperial = false,
        heatStreak = 0,
        totalSets = 0,
        totalXp = 0,
        restEndsAt = null,
        restDurationSec = 90,
        currentIndex = 0,
        exercises = emptyList()
    ),
    val selectedIndex: Int = 0,
    val weightDisplay: Double = 45.0,
    val reps: Int = 8,
    val durationMin: Double = 10.0,
    val distanceDisplay: Double = 1.0,
    val metrics: WearLiveMetrics = WearLiveMetrics(),
    val hrStatus: String = "HR off",
    val feedback: WearFeedbackEvent? = null,
    val phoneConnected: Boolean = false,
    val linkStatus: String = "Looking for phone…",
    val lastLogFlash: String? = null
) {
    val localBpm: Int? get() = metrics.bpm
}

class WearSessionViewModel(private val app: Application) : ViewModel(),
    MessageClient.OnMessageReceivedListener {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState

    private val messageClient by lazy { Wearable.getMessageClient(app) }
    private val nodeClient by lazy { Wearable.getNodeClient(app) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(app) }

    private var workoutService: WearWorkoutService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as WearWorkoutService.LocalBinder).getService()
            workoutService = service
            isBound = true
            
            // Sync current state to service if needed
            if (_uiState.value.session.active) {
                service.startWorkout()
                _uiState.value.session.maxHr?.let { service.setMaxHr(it) }
                service.onRestChanged(_uiState.value.session.restEndsAt)
            }

            // Observe metrics from service
            viewModelScope.launch {
                service.metrics.collect { metrics ->
                    _uiState.update {
                        it.copy(
                            metrics = metrics,
                            hrStatus = metrics.bpm?.let { bpm -> "♥ $bpm" } ?: it.hrStatus
                        )
                    }
                    sendToPhone(WearPaths.METRICS, metrics.toJson())
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            workoutService = null
            isBound = false
        }
    }

    private var pollJob: Job? = null

    fun onSensorPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(hrStatus = "Reading HR…") }
            if (_uiState.value.session.active) workoutService?.startWorkout()
        } else {
            _uiState.update { it.copy(hrStatus = "Allow heart rate") }
        }
    }

    fun bind() {
        messageClient.addListener(this)
        
        val intent = Intent(app, WearWorkoutService::class.java)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshPhoneConnection(sendHello = true)
                delay(4_000)
            }
        }
    }

    fun unbind() {
        pollJob?.cancel()
        pollJob = null
        messageClient.removeListener(this)
        
        if (isBound) {
            app.unbindService(connection)
            isBound = false
        }
    }

    fun retryLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(linkStatus = "Searching…") }
            refreshPhoneConnection(sendHello = true)
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearPaths.HELLO_ACK -> {
                _uiState.update {
                    it.copy(
                        phoneConnected = true,
                        linkStatus = "Phone linked — start a quest"
                    )
                }
            }
            WearPaths.SESSION_STATE -> {
                val session = WearSessionState.fromJson(event.data)
                session.maxHr?.let { workoutService?.setMaxHr(it) }
                _uiState.update { s ->
                    val maxIdx = (session.exercises.size - 1).coerceAtLeast(0)
                    val phoneIdx = session.currentIndex.coerceIn(0, maxIdx)
                    val phoneChangedExercise = session.currentIndex != s.session.currentIndex
                    val idx = when {
                        !s.session.active && session.active -> phoneIdx
                        phoneChangedExercise -> phoneIdx
                        s.selectedIndex in session.exercises.indices -> s.selectedIndex
                        else -> phoneIdx
                    }
                    val ex = session.exercises.getOrNull(idx)
                    val switching = idx != s.selectedIndex || (!s.session.active && session.active)
                    s.copy(
                        session = session,
                        selectedIndex = idx,
                        weightDisplay = when {
                            switching -> defaultWeight(session, ex)
                            else -> s.weightDisplay
                        },
                        reps = when {
                            switching && (ex?.lastReps ?: 0) > 0 -> ex!!.lastReps
                            switching -> (ex?.targetReps ?: 8).coerceAtLeast(1)
                            else -> s.reps
                        },
                        durationMin = if (switching) defaultDuration(ex) else s.durationMin,
                        distanceDisplay = if (switching) defaultDistance(session, s.metrics) else s.distanceDisplay,
                        phoneConnected = true,
                        linkStatus = if (session.active) "Quest live" else "Phone linked — start a quest"
                    )
                }
                if (session.active) {
                    workoutService?.startWorkout()
                    workoutService?.onRestChanged(session.restEndsAt)
                } else {
                    workoutService?.stopWorkout()
                }
            }
            WearPaths.SESSION_ENDED -> {
                workoutService?.stopWorkout()
                _uiState.update {
                    it.copy(
                        session = it.session.copy(active = false, restEndsAt = null),
                        feedback = WearFeedbackEvent(
                            WearFeedbackKind.REST_END,
                            "Quest complete — check your phone"
                        ),
                        linkStatus = "Phone linked — start a quest"
                    )
                }
                WearHaptics.pulse(app, WearHaptics.Pattern.REST_END)
            }
            WearPaths.FEEDBACK -> {
                val fb = WearFeedbackEvent.fromJson(event.data)
                _uiState.update { it.copy(feedback = fb, lastLogFlash = fb.message) }
                WearHaptics.forKind(app, fb.kind)
            }
        }
    }

    fun selectExercise(index: Int) {
        _uiState.update { s ->
            val ex = s.session.exercises.getOrNull(index) ?: return@update s
            s.copy(
                selectedIndex = index,
                weightDisplay = defaultWeight(s.session, ex),
                reps = ex.lastReps.takeIf { it > 0 } ?: ex.targetReps.coerceAtLeast(1),
                durationMin = defaultDuration(ex),
                distanceDisplay = defaultDistance(s.session, s.metrics),
                lastLogFlash = null
            )
        }
    }

    fun adjustWeight(delta: Double) {
        _uiState.update { it.copy(weightDisplay = (it.weightDisplay + delta).coerceAtLeast(0.0)) }
    }

    fun adjustReps(delta: Int) {
        _uiState.update { it.copy(reps = (it.reps + delta).coerceAtLeast(1)) }
    }

    fun adjustDuration(delta: Double) {
        _uiState.update {
            it.copy(durationMin = (it.durationMin + delta).coerceAtLeast(0.5))
        }
    }

    fun adjustDistance(delta: Double) {
        _uiState.update {
            it.copy(distanceDisplay = (it.distanceDisplay + delta).coerceAtLeast(0.0))
        }
    }

    fun logSet() {
        val s = _uiState.value
        if (!s.session.active || s.session.exercises.isEmpty()) return
        val ex = s.session.exercises.getOrNull(s.selectedIndex)
        val category = ex?.category?.uppercase().orEmpty()
        val tracking = ex?.trackingType?.uppercase()?.takeIf { it.isNotBlank() } ?: when (category) {
            "CARDIO" -> "DISTANCE_TIME"
            "FLEXIBILITY" -> "TIME_ONLY"
            "BODYWEIGHT" -> "BODYWEIGHT_REPS"
            else -> "WEIGHT_REPS"
        }
        val cmd = when (tracking) {
            "CARDIO_MACHINE", "DISTANCE_TIME" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = 0.0,
                reps = 0,
                durationMin = s.durationMin,
                distanceDisplay = s.distanceDisplay,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            "DISTANCE_ONLY" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = 0.0,
                reps = 0,
                durationMin = 0.0,
                distanceDisplay = s.distanceDisplay,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            "TIME_ONLY" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = 0.0,
                reps = 0,
                durationMin = s.durationMin,
                distanceDisplay = 0.0,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            "BODYWEIGHT_REPS", "REPS_ONLY", "ASSISTED_REPS" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = s.weightDisplay,
                reps = s.reps,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            else -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = s.weightDisplay,
                reps = s.reps,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
        }
        val flashOk = when {
            tracking == "DISTANCE_ONLY" -> {
                val distUnit = if (s.session.imperial) "mi" else "km"
                "Set sent - ${fmt(cmd.distanceDisplay)} $distUnit"
            }
            tracking == "TIME_ONLY" -> "Set sent - ${fmt(cmd.durationMin)} min"
            tracking in setOf("BODYWEIGHT_REPS", "REPS_ONLY") -> "Set sent - ${cmd.reps} reps"
            tracking in setOf("CARDIO_MACHINE", "DISTANCE_TIME") -> {
                val distUnit = if (s.session.imperial) "mi" else "km"
                "Set sent · ${fmt(cmd.durationMin)} min · ${fmt(cmd.distanceDisplay)} $distUnit"
            }
            tracking == "FLEXIBILITY" -> "Set sent · ${fmt(cmd.durationMin)} min"
            tracking == "BODYWEIGHT" -> "Set sent · ${cmd.reps} reps"
            else -> {
                val unit = if (s.session.imperial) "lb" else "kg"
                "Set sent · ${cmd.reps}×${fmt(cmd.weightDisplay)} $unit"
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(lastLogFlash = "Sending set…") }
            val ok = sendToPhone(WearPaths.LOG_SET, cmd.toJson())
            WearHaptics.pulse(app, WearHaptics.Pattern.SET_LOGGED)
            _uiState.update {
                it.copy(
                    lastLogFlash = if (ok) flashOk else "Not linked — open phone session"
                )
            }
        }
    }

    fun sendRest(action: WearRestAction, seconds: Int = 30) {
        viewModelScope.launch {
            sendToPhone(WearPaths.REST, WearRestCommand(action, seconds).toJson())
        }
    }

    fun dismissFeedback() = _uiState.update { it.copy(feedback = null) }

    private suspend fun refreshPhoneConnection(sendHello: Boolean) {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
        val phoneCap = runCatching {
            capabilityClient
                .getCapability(WearCapabilities.PHONE, CapabilityClient.FILTER_REACHABLE)
                .await()
        }.getOrNull()
        val phoneNodes = phoneCap?.nodes.orEmpty()
        val linked = phoneNodes.isNotEmpty()
        val status = when {
            phoneNodes.isNotEmpty() -> "Phone linked — start a quest"
            nodes.isNotEmpty() -> "Wear network up — waiting for FitnessRPG on phone"
            else -> "Open FitnessRPG on your phone"
        }
        _uiState.update {
            it.copy(
                phoneConnected = linked,
                linkStatus = if (it.session.active) "Quest live" else status
            )
        }
        if (sendHello && nodes.isNotEmpty()) {
            sendToPhone(WearPaths.HELLO, ByteArray(0))
        }
    }

    private suspend fun sendToPhone(path: String, data: ByteArray): Boolean {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrDefault(emptyList())
        if (nodes.isEmpty()) return false
        var any = false
        nodes.forEach { node ->
            val ok = runCatching { messageClient.sendMessage(node.id, path, data).await() }.isSuccess
            any = any || ok
        }
        if (any && path != WearPaths.HELLO && path != WearPaths.METRICS && path != WearPaths.HR_SAMPLE) {
            _uiState.update { it.copy(phoneConnected = true) }
        }
        return any
    }

    override fun onCleared() {
        unbind()
        super.onCleared()
    }

    companion object {
        fun Factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WearSessionViewModel(app) as T
        }

        private fun fmt(value: Double): String =
            if (value % 1.0 < 0.05) value.toInt().toString() else "%.1f".format(value)

        private fun defaultWeight(
            session: WearSessionState,
            ex: WearExerciseState?
        ): Double {
            if (ex != null && ex.lastWeightDisplay > 0) return ex.lastWeightDisplay
            val isBodyweight = ex?.category.equals("BODYWEIGHT", ignoreCase = true) == true
            if (isBodyweight) {
                val body = session.bodyWeightKg
                if (body != null && body > 0) {
                    return if (session.imperial) body * 2.2046226 else body
                }
            }
            return if (session.imperial) 95.0 else 45.0
        }

        private fun defaultDuration(ex: WearExerciseState?): Double {
            val target = ex?.targetReps ?: 0
            val tracking = ex?.trackingType?.uppercase().orEmpty()
            // Phone often stores cardio/flexibility target minutes in targetReps.
            if (target > 0 &&
                (tracking in setOf("CARDIO_MACHINE", "DISTANCE_TIME", "TIME_ONLY") ||
                    ex?.category.equals("CARDIO", true) == true ||
                    ex?.category.equals("FLEXIBILITY", true) == true)
            ) {
                return target.toDouble()
            }
            return 10.0
        }

        private fun defaultDistance(
            session: WearSessionState,
            metrics: WearLiveMetrics
        ): Double {
            val meters = metrics.distanceMeters?.takeIf { it > 20 } ?: return 1.0
            return if (session.imperial) meters / 1609.344 else meters / 1000.0
        }
    }
}
