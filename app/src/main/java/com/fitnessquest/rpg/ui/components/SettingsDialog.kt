package com.fitnessquest.rpg.ui.components

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.font.FontWeight
import com.fitnessquest.rpg.data.ai.DownloadState
import com.fitnessquest.rpg.data.ai.LocalModelDownloader
import com.fitnessquest.rpg.data.ai.ModelCatalog
import com.fitnessquest.rpg.data.ai.PlayAssetModelProvider

import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.data.auth.AccountState
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.health.BodyProfile
import com.fitnessquest.rpg.data.health.BodyProfileSource
import com.fitnessquest.rpg.data.health.HealthConnectProfile
import com.fitnessquest.rpg.data.health.HeightFormat
import com.fitnessquest.rpg.data.sync.SyncStatus
import com.fitnessquest.rpg.domain.EffortMethod
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.ui.appContainer
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.ui.theme.Gold
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate

import java.time.ZoneId
import java.util.Date
import java.util.Locale

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val userPrefs = container.prefs
    val repository = container.repository
    val gemini = container.gemini
    val downloader = container.localAiModel
    val playAssetProvider = container.playAssetModel



    val character: StateFlow<CharacterEntity?> = container.repository.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val account: StateFlow<AccountState> = container.auth.state
    val syncStatus: StateFlow<SyncStatus> = container.sync.status
    val imperial: StateFlow<Boolean> = container.prefs.imperial
    val effortMethod: StateFlow<EffortMethod> = container.prefs.effortMethod
    val sound: StateFlow<Boolean> = container.prefs.sound
    val haptics: StateFlow<Boolean> = container.prefs.haptics
    val lowPowerUi: StateFlow<Boolean> = container.prefs.lowPowerUi
    val bodyProfile: StateFlow<BodyProfile> = container.prefs.bodyProfile
    val wearPresence = container.wearPresence.state
    val healthConnect: HealthConnectProfile = container.healthConnect
    val developerSandbox: StateFlow<Boolean> = container.prefs.developerSandbox
    val useLocalAi: StateFlow<Boolean> = container.prefs.useLocalAi

    val loginProvider: String

        get() = when {
            container.auth.hasGoogleProvider() -> "Google"
            container.auth.hasEmailPasswordProvider() -> "Email"
            else -> "Guest"
        }

    /** Debug builds only — never true in release. */
    val developerToolsAvailable: Boolean = BuildConfig.DEBUG

    private val _developerToolsUnlocked = MutableStateFlow(value = false)
    val developerToolsUnlocked: StateFlow<Boolean> = _developerToolsUnlocked

    private val _bodySyncMessage = MutableStateFlow<String?>(null)
    val bodySyncMessage: StateFlow<String?> = _bodySyncMessage

    private val _devMessage = MutableStateFlow<String?>(null)
    val devMessage: StateFlow<String?> = _devMessage

    private val _resetMessage = MutableStateFlow<String?>(null)
    val resetMessage: StateFlow<String?> = _resetMessage

    fun refreshWearPresence() = container.wearPresence.refreshNow()

    fun setEffortMethod(method: EffortMethod) = container.prefs.setEffortMethod(method)

    fun setSound(value: Boolean) {
        container.prefs.setSound(value)
        AudioEffects.soundEnabled = value
    }

    fun setHaptics(value: Boolean) {
        container.prefs.setHaptics(value)
        HapticEffects.hapticsEnabled = value
    }

    fun setLowPowerUi(value: Boolean) = container.prefs.setLowPowerUi(value)

    fun setLeftHanded(value: Boolean) = container.prefs.setLeftHanded(value)
    val leftHanded: StateFlow<Boolean> = container.prefs.leftHanded

    fun setUseLocalAi(value: Boolean) = container.prefs.setUseLocalAi(value)

    fun forceSync() {

        viewModelScope.launch {
            _authMessage.value = "Starting manual sync..."
            container.sync.forceReconcile()
            _authMessage.value = "Cloud sync complete."
        }
    }

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage


    fun setImperial(value: Boolean) = container.prefs.setImperial(value)

    fun redeemPremiumCode(code: String) {
        if (container.prefs.redeemPremiumCode(code)) {
            container.sync.setCloudPremium(enabled = true)
            _authMessage.value = "✦ Premium unlocked! Fantasy races, extra classes & hairstyles are yours."
        } else {
            _authMessage.value = "That code didn't work. Check the spelling and try again."
        }
    }

    fun setPremium(value: Boolean) {
        container.prefs.setPremium(value)
        // Keep Firestore in sync so debug override / revoke sticks for signed-in accounts.
        container.sync.setCloudPremium(value)
    }

    fun saveApiKey(key: String) {
        container.gemini.apiKey = key
    }

    fun currentApiKey(): String = container.gemini.userApiKey

    fun unlockDeveloperTools() {
        if (!developerToolsAvailable) return
        _developerToolsUnlocked.value = true
        _devMessage.value = "Developer tools unlocked"
    }

    fun enterDeveloperSandbox() {
        if (!developerToolsAvailable) return
        viewModelScope.launch {
            container.prefs.setDeveloperSandbox(true)
            container.prefs.setPremium(true)
            container.auth.signOut()
            container.repository.debugStartSandboxHero()
            _devMessage.value =
                "Sandbox on — signed out. Cheats stay local. Sign in with Google to restore your live hero."
        }
    }

    fun debugGrantGold() {
        if (!developerToolsAvailable || !container.prefs.developerSandbox.value) return
        viewModelScope.launch {
            container.repository.debugGrantGold()
            _devMessage.value = "+1000 gold (local only)"
        }
    }

    fun debugGrantXp() {
        if (!developerToolsAvailable || !container.prefs.developerSandbox.value) return
        viewModelScope.launch {
            container.repository.debugGrantXp()
            _devMessage.value = "+500 XP (local only)"
        }
    }

    fun debugFillEnergy() {
        if (!developerToolsAvailable || !container.prefs.developerSandbox.value) return
        viewModelScope.launch {
            container.repository.debugFillEnergy()
            _devMessage.value = "Energy filled (local only)"
        }
    }

    fun debugResetHero() {
        if (!developerToolsAvailable || !container.prefs.developerSandbox.value) return
        viewModelScope.launch {
            container.repository.debugStartSandboxHero()
            _devMessage.value = "Sandbox hero reset"
        }
    }

    fun repairDuplicateGear() {
        viewModelScope.launch {
            val result = container.repository.repairDuplicateGearInstances()
            _resetMessage.value =
                "Gear repair complete: removed ${result.removedCount} duplicates (${result.beforeCount} -> ${result.afterCount})."
        }
    }

    fun recalculateMasteries() {
        viewModelScope.launch {
            _resetMessage.value = "Recalculating masteries from history..."
            val count = container.repository.recalculateAllMovementMasteriesFromHistory()
            _resetMessage.value = "Success! Rebuilt $count masteries from your workout chronicle."
        }
    }

    fun clearDevMessage() {
        _devMessage.value = null
    }

    val isPremium: StateFlow<Boolean> = container.prefs.isPremium

    /**
     * Permanent fresh start for the live hero (local + cloud when signed in).
     * Not available while developer sandbox is active.
     */
    fun resetLiveHeroFresh() {
        if (container.prefs.developerSandbox.value) {
            _resetMessage.value = "Exit sandbox first (sign in with Google), then reset live."
            return
        }
        viewModelScope.launch {
            // Capture the name before wipe — local prefs alone are not always enough
            // to free the Firestore uniqueness claim.
            val previousName = container.repository.getCharacter().name
            container.usernames.releaseClaim(knownDisplay = previousName)
            container.repository.resetLiveProgress()
            if (container.auth.state.value.hasAccount) {
                container.sync.overwriteCloudAfterFreshStart()
            }
            _resetMessage.value = "Live hero reset — pick a class and username to start fresh."
        }
    }

    fun clearResetMessage() {
        _resetMessage.value = null
    }

    fun saveBodyProfileManual(
        weightDisplay: String,
        feetText: String,
        inchesText: String,
        heightCmOrCombined: String,
        dobMilli: Long?,
        useFeetInches: Boolean,
    ) {
        val imperial = container.prefs.imperial.value
        val weightKg = weightDisplay.toDoubleOrNull()?.let { Units.toKg(it, imperial) }
        val heightM = if (imperial && useFeetInches) {
            HeightFormat.parseFeetInchesFields(feetText, inchesText)
                ?: HeightFormat.parseToMeters(heightCmOrCombined, imperial = true)
        } else {
            HeightFormat.parseToMeters(heightCmOrCombined, imperial)
        }
        if ((weightKg == null) && (heightM == null) && (dobMilli == null) &&
            weightDisplay.isBlank() && heightCmOrCombined.isBlank() &&
            feetText.isBlank() && inchesText.isBlank()
        ) {
            _bodySyncMessage.value = "Enter weight, height, or DOB first"
            return
        }
        container.prefs.saveBodyProfileManual(weightKg, heightM, dobMilli, clearMissing = false)
        if (weightKg != null) {
            viewModelScope.launch {
                container.healthConnect.writeWeight(weightKg)
            }
        }
        _bodySyncMessage.value = "Body metrics saved"
    }

    fun applyHealthConnectProfile(profile: BodyProfile) {
        if (profile.isEmpty) {
            _bodySyncMessage.value =
                "No height/weight found. Allow Height + history in Health Connect, or enter manually."
            return
        }
        container.prefs.saveBodyProfileFromHealthConnect(profile)
        val parts = buildList {
            if (profile.bodyWeightKg != null) add("weight")
            if (profile.heightM != null) add("height")
        }
        val missing = buildList {
            if (profile.heightM == null) add("height")
            add("age")
        }
        _bodySyncMessage.value = buildString {
            append("Synced ${parts.joinToString(" + ")} from Health Connect.")
            if (missing.isNotEmpty()) {
                append(" Enter ${missing.joinToString(" + ")} manually if needed.")
            }
        }
    }

    fun setBodySyncMessage(message: String?) {
        _bodySyncMessage.value = message
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            container.auth.signInWithGoogle(activity)
                .onSuccess { _authMessage.value = "Signed in with Google!" }
                .onFailure { _authMessage.value = it.message ?: "Sign-in failed." }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            container.auth.signUpWithEmail(email.trim(), password)
                .onSuccess { _authMessage.value = "Account created!" }
                .onFailure { _authMessage.value = it.message ?: "Could not create account." }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            container.auth.signInWithEmail(email.trim(), password)
                .onSuccess { _authMessage.value = "Signed in!" }
                .onFailure { _authMessage.value = it.message ?: "Sign-in failed." }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Leave party while still authenticated
            runCatching { container.party.leaveParty() }

            // Wipe local data to prevent leakage into next signed-in account
            container.repository.resetLiveProgress()
            container.auth.signOut()
            _authMessage.value = "Signed out. Playing as a fresh guest."
        }
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    private val _deleteBusy = MutableStateFlow(false)
    val deleteBusy: StateFlow<Boolean> = _deleteBusy

    /** Email/password accounts need the password field to reauthenticate for deletion. */
    fun needsEmailPasswordForDelete(): Boolean =
        container.auth.state.value.hasAccount &&
            container.auth.hasEmailPasswordProvider() &&
            !container.auth.hasGoogleProvider()

    /**
     * Permanently deletes the signed-in Firebase account and associated cloud/local
     * game data. [emailPassword] is required for email/password accounts (reauth).
     */
    fun deleteAccount(activity: Activity?, emailPassword: String?) {
        if (!container.auth.state.value.hasAccount) {
            _authMessage.value = "Sign in first to delete an account."
            return
        }
        if (container.prefs.developerSandbox.value) {
            _authMessage.value = "Exit sandbox first (sign in with Google), then delete the live account."
            return
        }
        if (_deleteBusy.value) return
        viewModelScope.launch {
            _deleteBusy.value = true
            try {
                val previousName = container.repository.getCharacter().name
                // Leave social groups while still authenticated.
                runCatching { container.party.leaveParty() }
                runCatching { container.guild.leaveGuild() }
                container.usernames.releaseClaim(knownDisplay = previousName)
                container.sync.deleteCloudAccountData().getOrElse { error ->
                    _authMessage.value = error.message ?: "Could not delete cloud data."
                    return@launch
                }
                container.auth.deleteAccount(activity, emailPassword).getOrElse { error ->
                    _authMessage.value = error.message ?: "Could not delete account."
                    return@launch
                }
                container.repository.resetLiveProgress()
                _authMessage.value =
                    "Account deleted. You're playing as a guest — create a new account anytime."
            } finally {
                _deleteBusy.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer) }
        }
    }
}

/** Gear icon that opens the app settings dialog. Drop into any screen header. */
@Composable
fun SettingsIconButton() {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.Settings, contentDescription = "Settings")
    }
    if (show) {
        SettingsDialog(onDismiss = { show = false })
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val account by viewModel.account.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()
    val imperial by viewModel.imperial.collectAsState()
    val leftHanded by viewModel.leftHanded.collectAsState()
    val activity = LocalActivity.current
    var key by remember { mutableStateOf(viewModel.currentApiKey()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    fun close() {
        viewModel.clearAuthMessage()
        viewModel.clearDevMessage()
        viewModel.clearResetMessage()
        onDismiss()
    }

    var confirmFreshStart by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var titleTaps by remember { mutableIntStateOf(0) }
    var titleTapWindowStart by remember { mutableLongStateOf(0L) }
    var showEmailAuth by remember { mutableStateOf(false) }
    val sandbox by viewModel.developerSandbox.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val toolsUnlocked by viewModel.developerToolsUnlocked.collectAsState()
    val showDeveloperSection =
        viewModel.developerToolsAvailable && (toolsUnlocked || sandbox)

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Header Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (!viewModel.developerToolsAvailable || toolsUnlocked) return@clickable
                            val now = System.currentTimeMillis()
                            if ((now - titleTapWindowStart) > 3_000L) {
                                titleTapWindowStart = now
                                titleTaps = 1
                            } else {
                                titleTaps += 1
                            }
                            if (titleTaps >= 7) {
                                titleTaps = 0
                                viewModel.unlockDeveloperTools()
                            }
                        }
                    )
                    Button(
                        onClick = {
                            viewModel.saveApiKey(key)
                            close()
                        }
                    ) {
                        Text("Done")
                    }
                }

                HorizontalDivider()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (showDeveloperSection) {
                        Text(
                            "Developer",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (sandbox) {
                            Text(
                                "Sandbox active — cloud sync is off. Sign in with Google below to restore your live hero (sandbox progress is discarded).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = viewModel::debugGrantGold) { Text("+Gold") }
                                TextButton(onClick = viewModel::debugGrantXp) { Text("+XP") }
                                TextButton(onClick = viewModel::debugFillEnergy) { Text("Energy") }
                            }
                            TextButton(onClick = viewModel::debugResetHero) { Text("Reset sandbox hero") }
                        } else {
                            Text(
                                "Enter sandbox to experiment locally. You will be signed out and a disposable Dev Hero is created. Your live cloud save is left alone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = viewModel::enterDeveloperSandbox,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Enter developer sandbox") }
                        }
                        val devMessage by viewModel.devMessage.collectAsState()
                        devMessage?.let { msg ->
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Premium User Override", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isPremium,
                                onCheckedChange = { viewModel.setPremium(it) }
                            )
                        }

                        HorizontalDivider()
                    }
                    Text("Account", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    if (account.hasAccount) {
                        Text(
                            "Signed in via ${viewModel.loginProvider}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            account.email ?: account.displayName ?: "your account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "UID: ${account.uid?.take(8)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        val syncStatus by viewModel.syncStatus.collectAsState()
                        Text(
                            when {
                                syncStatus.error != null ->
                                    "\u26A0\uFE0F Backup issue: ${syncStatus.error}"
                                syncStatus.lastSyncAt != null -> {
                                    val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                                    "\u2601\uFE0F Hero backed up \u00B7 ${fmt.format(Date(syncStatus.lastSyncAt!!))}"
                                }
                                else -> "\u2601\uFE0F Cloud backup is on"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = viewModel::forceSync) { Text("Sync Now") }
                            TextButton(onClick = { confirmSignOut = true }) { Text("Sign out") }
                        }
                        TextButton(onClick = { confirmFreshStart = true }) {
                            Text("Start fresh (reset live hero)")
                        }
                        TextButton(onClick = { confirmDeleteAccount = true }) {
                            Text(
                                "Delete account",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            "Sign in to back up your hero and keep progress across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { activity?.let(viewModel::signInWithGoogle) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Sign in with Google") }

                        TextButton(
                            onClick = { showEmailAuth = !showEmailAuth },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(if (showEmailAuth) "Hide email sign-in" else "Or sign in with email")
                        }

                        AnimatedVisibility(visible = showEmailAuth) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    singleLine = true,
                                    label = { Text("Email") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    singleLine = true,
                                    label = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = { viewModel.signUpWithEmail(email, password) },
                                        enabled = email.isNotBlank() && password.length >= 6
                                    ) { Text("Create account") }
                                    TextButton(
                                        onClick = { viewModel.signInWithEmail(email, password) },
                                        enabled = email.isNotBlank() && password.isNotEmpty()
                                    ) { Text("Sign in") }
                                }
                            }
                        }

                        TextButton(onClick = { confirmFreshStart = true }) {
                            Text("Start fresh (local hero)")
                        }
                    }

                    // Unified Hero Maintenance Section (SD-4)
                    Spacer(Modifier.size(4.dp))
                    Text("Hero Maintenance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = viewModel::repairDuplicateGear,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Repair duplicate gear")
                    }
                    OutlinedButton(
                        onClick = viewModel::recalculateMasteries,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recalculate masteries from history")
                    }

                    authMessage?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    val resetMessage by viewModel.resetMessage.collectAsState()
                    resetMessage?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider()
                    Text("Premium", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (isPremium) "✦ Premium active — RGB sliders, fantasy races, Paladin / Necromancer, and extra hairstyles unlocked."
                        else "Free: a few color picks + Human race. Premium unlocks RGB sliders, races, extra classes, and more hairstyles. Signed-in accounts can also receive Premium remotely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPremium) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isPremium) {
                        var redeemCode by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = redeemCode,
                            onValueChange = { redeemCode = it },
                            singleLine = true,
                            label = { Text("Redeem code") },
                            placeholder = { Text("Enter code...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.redeemPremiumCode(redeemCode) },
                            enabled = redeemCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Unlock Premium") }
                    }
                    HorizontalDivider()
                    Text("Units", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !imperial,
                            onClick = { viewModel.setImperial(false) },
                            label = { Text("Metric (kg \u00B7 km)") }
                        )
                        FilterChip(
                            selected = imperial,
                            onClick = { viewModel.setImperial(true) },
                            label = { Text("Imperial (lb \u00B7 mi)") }
                        )
                    }
                    HorizontalDivider()
                    Text("Navigation hand", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Places the navigation pill on your preferred side.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = leftHanded,
                            onClick = { viewModel.setLeftHanded(true) },
                            label = { Text("Left") }
                        )
                        FilterChip(
                            selected = !leftHanded,
                            onClick = { viewModel.setLeftHanded(false) },
                            label = { Text("Right") }
                        )
                    }
                    HorizontalDivider()
                    BodyMetricsSection(viewModel = viewModel, imperial = imperial)
                    HorizontalDivider()
                    Text("Watch", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    val wear by viewModel.wearPresence.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (wear.watchLinked) Color(0xFF4CAF50) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            wear.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (wear.watchLinked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Text(
                        if (wear.watchLinked) {
                            "Open FitnessRPG on the watch, then start a quest on Train."
                        } else {
                            "Keep FitnessRPG open on the watch. Phone and watch must use the same install signing (debug↔debug)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = viewModel::refreshWearPresence,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh watch link")
                    }
                    HorizontalDivider()
                    Text("Feedback", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val sound by viewModel.sound.collectAsState()
                        val haptics by viewModel.haptics.collectAsState()
                        FilterChip(
                            selected = sound,
                            onClick = { viewModel.setSound(!sound) },
                            label = { Text("\uD83D\uDD0A Sound") }
                        )
                        FilterChip(
                            selected = haptics,
                            onClick = { viewModel.setHaptics(!haptics) },
                            label = { Text("\uD83D\uDCF3 Haptics") }
                        )
                    }
                    HorizontalDivider()
                    Text("Graphics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    val lowPowerUi by viewModel.lowPowerUi.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Low Power Mode", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Disables intensive fantasy effects (embers & glows) for better performance and stability.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = lowPowerUi,
                            onCheckedChange = { viewModel.setLowPowerUi(it) }
                        )
                    }
                    HorizontalDivider()
                    Text("Effort tracking", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    val effortMethod by viewModel.effortMethod.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EffortMethod.entries.forEach { method ->
                            FilterChip(
                                selected = effortMethod == method,
                                onClick = { viewModel.setEffortMethod(method) },
                                label = { Text(method.label) }
                            )
                        }
                    }
                    Text(
                        effortMethod.description +
                            if (effortMethod != EffortMethod.OFF) {
                                ". Rate each strength set and the app adjusts your next set's weight and reps."
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Gemini API key", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Powers AI workout generation and battle narration. " +
                            "Get a free key at aistudio.google.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                    var keyVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            viewModel.saveApiKey(it)
                        },
                        singleLine = true,
                        label = { Text("API key") },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (keyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(icon, contentDescription = if (keyVisible) "Hide API key" else "Show API key")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                    val useLocalAi by viewModel.useLocalAi.collectAsState()
                    LocalAiModelSection(
                        viewModel = viewModel,
                        useLocalAi = useLocalAi,
                        onUseLocalAiChange = viewModel::setUseLocalAi
                    )
                    HorizontalDivider()

                    HevySyncSection(userPrefs = viewModel.userPrefs, repository = viewModel.repository, gemini = viewModel.gemini)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "FitQuest v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
                    )
                }
            }
        }
    }

    if (confirmFreshStart) {
        AlertDialog(
            onDismissRequest = { confirmFreshStart = false },
            title = { Text("Reset live hero?") },
            text = {
                Text(
                    if (account.hasAccount) {
                        "This permanently deletes your hero progress on this device and in the cloud " +
                            "(level, gold, gear, workout history). Workout templates are kept. This cannot be undone."
                    } else {
                        "This permanently deletes your local hero progress (level, gold, gear, history). " +
                            "Workout templates are kept."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFreshStart = false
                    viewModel.resetLiveHeroFresh()
                }) { Text("Reset forever") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFreshStart = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Signing out will wipe local character progress to ensure your accounts stay isolated. " +
                        "Your data is safe in the cloud and will be restored when you sign back in."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text("Sign out and wipe local") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteAccount && account.hasAccount) {
        val deleteBusy by viewModel.deleteBusy.collectAsState()
        val needsPassword = viewModel.needsEmailPasswordForDelete()
        AlertDialog(
            onDismissRequest = {
                if (!deleteBusy) {
                    confirmDeleteAccount = false
                    deletePassword = ""
                }
            },
            title = { Text("Delete account permanently?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This permanently deletes your Fitness Quest RPG account, cloud backup, " +
                            "username claim, leaderboard entry, and local hero progress. " +
                            "You will leave any party or guild. Workout templates on this device are kept. " +
                            "This cannot be undone."
                    )
                    if (needsPassword) {
                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it },
                            singleLine = true,
                            enabled = !deleteBusy,
                            label = { Text("Confirm with password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "You'll confirm with Google before the account is removed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (deleteBusy) {
                        Text(
                            "Deleting…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleteBusy && (!needsPassword || deletePassword.isNotBlank()),
                    onClick = {
                        viewModel.deleteAccount(
                            activity = activity,
                            emailPassword = deletePassword.takeIf { it.isNotBlank() }
                        )
                    }
                ) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleteBusy,
                    onClick = {
                        confirmDeleteAccount = false
                        deletePassword = ""
                    }
                ) { Text("Cancel") }
            }
        )
    }

    // Close the delete dialog once deletion succeeds.
    androidx.compose.runtime.LaunchedEffect(authMessage) {
        if (authMessage?.startsWith("Account deleted") == true) {
            confirmDeleteAccount = false
            deletePassword = ""
        }
    }
}

@Composable
private fun BodyMetricsSection(viewModel: SettingsViewModel, imperial: Boolean) {
    val body by viewModel.bodyProfile.collectAsState()
    val syncMsg by viewModel.bodySyncMessage.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hc = viewModel.healthConnect

    var weightText by remember(body.bodyWeightKg, imperial) {
        mutableStateOf(
            body.bodyWeightKg?.let { Units.toDisplay(it, imperial).let(HeightFormat::trimNum) }.orEmpty()
        )
    }
    var feetText by remember(body.heightM, imperial) {
        mutableStateOf(
            body.heightM?.takeIf { imperial }?.let { HeightFormat.metersToFeetInches(it).feet.toString() }
                .orEmpty()
        )
    }
    var inchesText by remember(body.heightM, imperial) {
        mutableStateOf(
            body.heightM?.takeIf { imperial }?.let {
                HeightFormat.trimNum(HeightFormat.metersToFeetInches(it).inches)
            }.orEmpty()
        )
    }
    var heightCmText by remember(body.heightM, imperial) {
        mutableStateOf(
            body.heightM?.takeIf { !imperial }?.let { HeightFormat.trimNum(HeightFormat.metersToCm(it)) }
                .orEmpty()
        )
    }
    
    val initialDob = body.dateOfBirthEpoch?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var month by remember(body.dateOfBirthEpoch) { mutableStateOf(initialDob?.monthValue?.toString() ?: "") }
    var day by remember(body.dateOfBirthEpoch) { mutableStateOf(initialDob?.dayOfMonth?.toString() ?: "") }
    var year by remember(body.dateOfBirthEpoch) { mutableStateOf(initialDob?.year?.toString() ?: "") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        scope.launch {
            if (granted.isEmpty()) {
                viewModel.setBodySyncMessage("Health Connect permission denied")
                return@launch
            }
            viewModel.applyHealthConnectProfile(hc.readLatest())
        }
    }

    Text("Body metrics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(
        when (body.source) {
            BodyProfileSource.HEALTH_CONNECT -> "Source: Health Connect"
            BodyProfileSource.MANUAL -> "Source: manual"
            BodyProfileSource.NONE -> "Not set yet"
        } + " \u00B7 age ${body.ageYears ?: "\u2014"} \u00B7 max HR ${body.maxHr}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = weightText,
        onValueChange = { weightText = it },
        singleLine = true,
        label = { Text(if (imperial) "Weight (lb)" else "Weight (kg)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    if (imperial) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = feetText,
                onValueChange = { feetText = it.filter { ch -> ch.isDigit() } },
                singleLine = true,
                label = { Text("Height (ft)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = inchesText,
                onValueChange = { inchesText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                singleLine = true,
                label = { Text("Inches") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        OutlinedTextField(
            value = heightCmText,
            onValueChange = { heightCmText = it },
            singleLine = true,
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }

    var birthdayError by remember { mutableStateOf<String?>(null) }

    Text("Birthday", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = month,
            onValueChange = { month = it.filter { ch -> ch.isDigit() }.take(2); birthdayError = null },
            label = { Text("MM") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = day,
            onValueChange = { day = it.filter { ch -> ch.isDigit() }.take(2); birthdayError = null },
            label = { Text("DD") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4); birthdayError = null },
            label = { Text("YYYY") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.5f)
        )
    }
    birthdayError?.let { err ->
        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    Button(
        onClick = {
            val m = month.toIntOrNull()
            val d = day.toIntOrNull()
            val y = year.toIntOrNull()
            val currentYear = LocalDate.now().year
            val dob = if (month.isNotBlank() || day.isNotBlank() || year.isNotBlank()) {
                if (m == null || m !in 1..12) {
                    birthdayError = "Invalid month (1-12)"
                    return@Button
                }
                if (d == null || d !in 1..31) {
                    birthdayError = "Invalid day (1-31)"
                    return@Button
                }
                if (y == null || y !in 1900..currentYear) {
                    birthdayError = "Invalid year (1900-$currentYear)"
                    return@Button
                }
                try {
                    LocalDate.of(y, m, d)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    birthdayError = "Invalid calendar date"
                    return@Button
                }
            } else null
            birthdayError = null

            viewModel.saveBodyProfileManual(
                weightDisplay = weightText,
                feetText = feetText,
                inchesText = inchesText,
                heightCmOrCombined = heightCmText,
                dobMilli = dob,
                useFeetInches = imperial
            )
        },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) { Text("Save body metrics") }
    TextButton(
        onClick = {
            when (hc.sdkStatus()) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    scope.launch {
                        if (hc.hasCoreReadPermissions()) {
                            // Still request full set so history/height can be granted if missing.
                            if (!hc.hasAllPermissions()) {
                                permissionLauncher.launch(hc.permissions.toTypedArray())
                            } else {
                                viewModel.applyHealthConnectProfile(profile = hc.readLatest())
                            }
                        } else {
                            permissionLauncher.launch(hc.permissions.toTypedArray())
                        }
                    }
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    HealthConnectProfile.providerInstallIntent()?.let { context.startActivity(it) }
                    viewModel.setBodySyncMessage("Install or update Health Connect")
                }
                else -> viewModel.setBodySyncMessage("Health Connect unavailable on this device")
            }
        }
    ) { Text("Sync weight / height from Health Connect") }
    Text(
        "Used for HR zones and bodyweight exercise load. Watch calories still use your Fitbit / watch profile.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    syncMsg?.let { msg ->
        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LocalAiModelSection(
    viewModel: SettingsViewModel,
    useLocalAi: Boolean,
    onUseLocalAiChange: (Boolean) -> Unit
) {
    val playAssetProvider = viewModel.playAssetProvider
    val downloader = viewModel.downloader
    val playAssetState by playAssetProvider.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current ?: context.findActivity()
    val isReady = playAssetState.installed || downloader.getModelFile("gemma_2b_it").exists()

    DisposableEffect(playAssetState.downloading) {
        if (playAssetState.downloading) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("🤖 On-Device Local AI Engine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Use Google's Gemma 2B model for 100% offline workout names, coaching, and battle narration with zero quotas or keys.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Google Play Offline AI Model", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                if (playAssetState.installed) "✅ Official Play Store model is installed and active (~1.34 GB)."
                else "Install the official Gemma 2B model with 1 tap from Google Play. No tokens or account required.",
                style = MaterialTheme.typography.bodySmall,
                color = if (playAssetState.installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (playAssetState.downloading) {
                LinearProgressIndicator(
                    progress = { playAssetState.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${playAssetState.message} ${playAssetState.progressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (playAssetState.statusCode == AssetPackStatus.WAITING_FOR_WIFI) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
                if (playAssetState.statusCode == AssetPackStatus.WAITING_FOR_WIFI) {
                    Text(
                        "⚠️ Connect to Wi-Fi to continue, or tap 'Approve Cellular Download' below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (playAssetState.totalBytes > 0L) {
                    Text(
                        "${android.text.format.Formatter.formatShortFileSize(context, playAssetState.downloadedBytes)} of ${android.text.format.Formatter.formatShortFileSize(context, playAssetState.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (playAssetState.statusCode == AssetPackStatus.WAITING_FOR_WIFI ||
                    playAssetState.statusCode == AssetPackStatus.REQUIRES_USER_CONFIRMATION) {
                    Button(
                        onClick = {
                            val act = activity ?: context.findActivity()
                            if (act != null) {
                                scope.launch {
                                    viewModel.playAssetProvider.showConsentDialog(act)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(
                            if (playAssetState.statusCode == AssetPackStatus.REQUIRES_USER_CONFIRMATION) "Confirm Download with Google Play"
                            else "Approve Cellular Download"
                        )
                    }
                }

            } else if (playAssetState.message.isNotBlank() && !playAssetState.installed) {
                Text(
                    playAssetState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (!playAssetState.installed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val act = activity ?: context.findActivity()
                            scope.launch {
                                runCatching { playAssetProvider.requestModel(act) }
                            }
                        },
                        enabled = !playAssetState.downloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Install From Play")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching { playAssetProvider.refreshState() }
                            }
                        },
                        enabled = !playAssetState.downloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh")
                    }
                }
            }

            if (BuildConfig.DEBUG && (
                playAssetState.statusCode != com.google.android.play.core.assetpacks.model.AssetPackStatus.UNKNOWN ||
                playAssetState.errorCode != com.google.android.play.core.assetpacks.model.AssetPackErrorCode.NO_ERROR
            )) {
                Text(
                    "Play diagnostics: status ${playAssetState.statusCode}, error ${playAssetState.errorCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (playAssetState.installed) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching { playAssetProvider.removeModel() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Play Model")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use Local AI by default", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Uses the downloaded offline model for text tasks first. Image import uses cloud AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = useLocalAi,
                onCheckedChange = onUseLocalAiChange,
                enabled = isReady
            )
        }
    }
}

@Composable
private fun HevySyncSection(
    userPrefs: com.fitnessquest.rpg.data.UserPrefs,
    repository: com.fitnessquest.rpg.data.GameRepository,
    gemini: com.fitnessquest.rpg.data.ai.GeminiService
) {
    val scope = rememberCoroutineScope()
    val apiKey by userPrefs.hevyApiKey.collectAsState()
    val autoSyncEnabled by userPrefs.hevyAutoSyncEnabled.collectAsState()
    val lastSync by userPrefs.hevyLastSyncTimestamp.collectAsState()

    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncIsError by remember { mutableStateOf(false) }
    var isRepairing by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("⚡ Hevy API & Auto-Sync", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Automatically sync routines, trainer programs, and past workout sessions from Hevy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = keyInput,
            onValueChange = {
                keyInput = it
                userPrefs.setHevyApiKey(it)
            },
            singleLine = true,
            label = { Text("Hevy API Key") },
            placeholder = { Text("Paste your Hevy API key") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Automatic Background Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Sync routines & sessions automatically on app launch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoSyncEnabled,
                onCheckedChange = { userPrefs.setHevyAutoSyncEnabled(it) }
            )
        }

        if (lastSync > 0) {
            val dateStr = remember(lastSync) {
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastSync))
            }
            Text(
                "🔄 Last Synced: $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (syncMessage != null) {
            Text(
                syncMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (syncIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                scope.launch {
                    isSyncing = true
                    syncIsError = false
                    syncMessage = "Syncing with Hevy..."
                    val res = com.fitnessquest.rpg.data.importexport.WorkoutImportService.performBackgroundSync(
                        apiKey = keyInput,
                        userPrefs = userPrefs,
                        gameRepository = repository,
                        gemini = gemini,
                        renameTemplates = true
                    )
                    isSyncing = false
                    if (res.isSuccess) {
                        syncIsError = false
                        val result = res.getOrDefault(com.fitnessquest.rpg.data.importexport.ImportPersistResult())
                        syncMessage = if (result.totalAdded > 0) "Hevy synced ${result.templatesAdded} training quests and ${result.sessionsAdded} history sessions."
                        else "Hevy is already up to date (0 new items)."
                    } else {
                        syncIsError = true
                        syncMessage = "⚠️ Sync Error: ${res.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isSyncing && keyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.size(8.dp))
                Text("Syncing...")
            } else {
                Text("Sync Now")
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    isRepairing = true
                    val repair = com.fitnessquest.rpg.data.importexport.WorkoutImportService.repairHevyHistoryImport(
                        apiKey = keyInput,
                        userPrefs = userPrefs,
                        gameRepository = repository
                    )
                    isRepairing = false
                    syncMessage = if (repair.isSuccess) {
                        val result = repair.getOrThrow()
                        "Deleted ${result.deletedBadTemplates} bad Training entries and imported ${result.importedHistorySessions} Hevy history sessions."
                    } else {
                        "Hevy repair failed: ${repair.exceptionOrNull()?.message}"
                    }
                }
            },
            enabled = !isRepairing && !isSyncing && keyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRepairing) "Repairing..." else "Repair Hevy history import")
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    isRepairing = true
                    val deleted = repository.cleanUpFragmentedSingleExerciseTemplates()
                    isRepairing = false
                    syncMessage = "Cleaned up $deleted fragmented single-exercise routine templates from Training Grounds!"
                }
            },
            enabled = !isRepairing && !isSyncing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRepairing) "Cleaning..." else "🧹 Clean Up Fragmented CSV Routines")
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
