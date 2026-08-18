package com.fitnessquest.rpg.wear

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.fitnessquest.shared.wear.WearRoutineSummary
import com.fitnessquest.shared.wear.WearSessionState
import org.json.JSONObject
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
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
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
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
                onToggleCardioTimer = viewModel::toggleCardioTimer,
                onResetCardioTimer = viewModel::resetCardioTimer,
                onStartWorkout = viewModel::startWorkout,
                onSelectRoutine = viewModel::selectRoutine,
                onRetryLink = viewModel::retryLink,
                onRequestHrPermission = { ensurePermissions(force = true) }
            )
            // Keep the watch screen alive while a quest is active.
            if (state.session.active) {
                DisposableEffect(state.session.active) {
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
    val cardioTimerActive: Boolean = false,
    val cardioTimerSeconds: Int = 0,
    val metrics: WearLiveMetrics = WearLiveMetrics(),
    val hrStatus: String = "HR off",
    val feedback: WearFeedbackEvent? = null,
    val phoneConnected: Boolean = false,
    val linkStatus: String = "Looking for phone…",
    val lastLogFlash: String? = null,
    val avatarBitmap: Bitmap? = null,
    val routines: List<WearRoutineSummary> = emptyList(),
    val selectedRoutineIndex: Int = 0
) {
    val localBpm: Int? get() = metrics.bpm
}

class WearSessionViewModel(private val app: Application) : ViewModel(),
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState

    private val messageClient by lazy { Wearable.getMessageClient(app) }
    private val dataClient by lazy { Wearable.getDataClient(app) }
    private val nodeClient by lazy { Wearable.getNodeClient(app) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(app) }

    private var workoutService: WearWorkoutService? = null
    private var isBound = false
    private var cardioTimerJob: Job? = null

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

    init {
        messageClient.addListener(this)
        dataClient.addListener(this)
        refreshPhoneConnection(sendHello = true)
        loadInitialAvatar()
    }

    private fun loadInitialAvatar() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Load instantly from local storage cache
            runCatching {
                val file = java.io.File(app.filesDir, "hero_avatar.png")
                if (file.exists()) {
                    val bytes = file.readBytes()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        _uiState.update { it.copy(avatarBitmap = bmp) }
                    }
                }
            }
            // 2. Query Wear Data Layer for latest
            runCatching {
                val buffer = dataClient.dataItems.await()
                for (item in buffer) {
                    if (item.uri.path == WearPaths.AVATAR_WATCH_FACE) {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val bytes = dataMap.getByteArray("image")
                        if (bytes != null && bytes.isNotEmpty()) {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                _uiState.update { it.copy(avatarBitmap = bmp) }
                                runCatching {
                                    java.io.File(app.filesDir, "hero_avatar.png").writeBytes(bytes)
                                }
                            }
                        }
                    }
                }
                buffer.release()
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearPaths.AVATAR_WATCH_FACE) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val bytes = dataMap.getByteArray("image")
                if (bytes != null && bytes.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        _uiState.update { it.copy(avatarBitmap = bmp) }
                        runCatching {
                            java.io.File(app.filesDir, "hero_avatar.png").writeBytes(bytes)
                        }
                    }
                }
            }
        }
    }

    fun bind() {
        if (!isBound) {
            val intent = Intent(app, WearWorkoutService::class.java)
            app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        if (isBound) {
            app.unbindService(connection)
            isBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(this)
        dataClient.removeListener(this)
        cardioTimerJob?.cancel()
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
            WearPaths.AVATAR_WATCH_FACE -> {
                if (event.data.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(event.data, 0, event.data.size)
                    if (bmp != null) {
                        _uiState.update { it.copy(avatarBitmap = bmp) }
                        runCatching {
                            java.io.File(app.filesDir, "hero_avatar.png").writeBytes(event.data)
                        }
                    }
                }
            }
            WearPaths.HELLO_ACK -> {
                _uiState.update {
                    it.copy(
                        phoneConnected = true,
                        linkStatus = "Phone linked — select a quest"
                    )
                }
                sendToPhone(WearPaths.ROUTINES_LIST, ByteArray(0))
            }
            WearPaths.ROUTINES_LIST -> {
                val routines = WearRoutineSummary.listFromJson(event.data)
                _uiState.update { it.copy(routines = routines, phoneConnected = true) }
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
                            switching -> ex?.suggestedReps ?: ex?.lastReps?.takeIf { it > 0 } ?: (ex?.targetReps ?: 8).coerceAtLeast(1)
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
                cardioTimerJob?.cancel()
                _uiState.update {
                    it.copy(
                        session = it.session.copy(active = false, restEndsAt = null),
                        feedback = WearFeedbackEvent(
                            WearFeedbackKind.REST_END,
                            "Quest complete — check your phone"
                        ),
                        linkStatus = "Phone linked — start a quest",
                        cardioTimerActive = false,
                        cardioTimerSeconds = 0
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
                reps = ex.suggestedReps ?: ex.lastReps.takeIf { it > 0 } ?: ex.targetReps.coerceAtLeast(1),
                durationMin = defaultDuration(ex),
                distanceDisplay = defaultDistance(s.session, s.metrics),
                lastLogFlash = null
            )
        }
    }

    fun selectRoutine(index: Int) {
        _uiState.update { s ->
            val maxIdx = (s.routines.size - 1).coerceAtLeast(0)
            s.copy(selectedRoutineIndex = index.coerceIn(0, maxIdx))
        }
    }

    fun startWorkout(routineId: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(linkStatus = "Starting quest…") }
            val req = JSONObject().apply {
                if (routineId != null && routineId > 0) put("routineId", routineId)
            }
            sendToPhone(WearPaths.START_WORKOUT, req.toString().toByteArray(Charsets.UTF_8))
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

    fun toggleCardioTimer() {
        val currentlyActive = _uiState.value.cardioTimerActive
        if (!currentlyActive) {
            _uiState.update { it.copy(cardioTimerActive = true) }
            cardioTimerJob?.cancel()
            cardioTimerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000L)
                    _uiState.update {
                        val nextSec = it.cardioTimerSeconds + 1
                        it.copy(
                            cardioTimerSeconds = nextSec,
                            durationMin = (nextSec / 60.0).coerceAtLeast(0.5)
                        )
                    }
                }
            }
        } else {
            cardioTimerJob?.cancel()
            _uiState.update { it.copy(cardioTimerActive = false) }
        }
    }

    fun resetCardioTimer() {
        cardioTimerJob?.cancel()
        _uiState.update { it.copy(cardioTimerActive = false, cardioTimerSeconds = 0, durationMin = 10.0) }
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
        
        val effectiveDuration = if (s.cardioTimerSeconds > 0) {
            (s.cardioTimerSeconds / 60.0).coerceAtLeast(0.5)
        } else {
            s.durationMin
        }

        val cmd = when (tracking) {
            "CARDIO_MACHINE", "DISTANCE_TIME" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = 0.0,
                reps = 0,
                durationMin = effectiveDuration,
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
                durationMin = effectiveDuration,
                distanceDisplay = 0.0,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            "BODYWEIGHT_REPS" -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = 0.0,
                reps = s.reps,
                durationMin = 0.0,
                distanceDisplay = 0.0,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
            else -> WearLogSetCommand(
                exerciseIndex = s.selectedIndex,
                weightDisplay = s.weightDisplay,
                reps = s.reps,
                durationMin = 0.0,
                distanceDisplay = 0.0,
                avgHr = s.metrics.bpm,
                maxHr = s.metrics.bpm
            )
        }

        // Reset timer if it was running
        if (s.cardioTimerActive || s.cardioTimerSeconds > 0) {
            cardioTimerJob?.cancel()
            _uiState.update { it.copy(cardioTimerActive = false, cardioTimerSeconds = 0) }
        }

        sendToPhone(WearPaths.LOG_SET, cmd.toJson())
        WearHaptics.pulse(app, WearHaptics.Pattern.SET_LOGGED)

        val summary = when (tracking) {
            "CARDIO_MACHINE", "DISTANCE_TIME" -> "${fmt(s.distanceDisplay)} ${if (s.session.imperial) "mi" else "km"}"
            "TIME_ONLY" -> "${fmt(effectiveDuration)} min"
            "BODYWEIGHT_REPS" -> "${s.reps} reps"
            else -> "${fmt(s.weightDisplay)}${if (s.session.imperial) "lb" else "kg"} × ${s.reps}"
        }
        _uiState.update { it.copy(lastLogFlash = "Logged: $summary") }
    }

    fun sendRest(action: WearRestAction, seconds: Int = 0) {
        val cmd = WearRestCommand(action, seconds)
        sendToPhone(WearPaths.REST, cmd.toJson())
        if (action == WearRestAction.SKIP) {
            _uiState.update { it.copy(session = it.session.copy(restEndsAt = null)) }
            workoutService?.onRestChanged(null)
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedback = null, lastLogFlash = null) }
    }

    fun onSensorPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(hrStatus = "HR active") }
            bind()
        } else {
            _uiState.update { it.copy(hrStatus = "Permission needed") }
        }
    }

    private fun refreshPhoneConnection(sendHello: Boolean) {
        viewModelScope.launch {
            val node = findPhoneNode()
            if (node == null) {
                _uiState.update {
                    it.copy(
                        phoneConnected = false,
                        linkStatus = "No phone found — ensure Bluetooth is on"
                    )
                }
                return@launch
            }
            if (sendHello) {
                try {
                    messageClient.sendMessage(node.id, WearPaths.HELLO, ByteArray(0)).await()
                    _uiState.update {
                        it.copy(
                            phoneConnected = true,
                            linkStatus = "Phone linked — start a quest"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            phoneConnected = false,
                            linkStatus = "Link failed: ${e.localizedMessage ?: "timeout"}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun findPhoneNode() = try {
        val capability = capabilityClient
            .getCapability(WearCapabilities.PHONE, CapabilityClient.FILTER_REACHABLE)
            .await()
        capability.nodes.firstOrNull { it.isNearby } ?: capability.nodes.firstOrNull()
            ?: nodeClient.connectedNodes.await().firstOrNull()
    } catch (_: Exception) {
        null
    }

    private fun sendToPhone(path: String, payload: ByteArray) {
        viewModelScope.launch {
            val node = findPhoneNode() ?: return@launch
            try {
                messageClient.sendMessage(node.id, path, payload).await()
            } catch (_: Exception) {
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WearSessionViewModel(app) as T
    }

    companion object {
        private fun fmt(value: Double): String =
            if (value % 1.0 < 0.05) value.toInt().toString() else "%.1f".format(value)

        private fun defaultWeight(
            session: WearSessionState,
            ex: WearExerciseState?
        ): Double {
            val suggested = ex?.suggestedWeightDisplay
            if (suggested != null && suggested > 0) return suggested
            
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
