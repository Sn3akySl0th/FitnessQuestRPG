package com.fitnessquest.rpg.data

import android.content.Context
import androidx.core.content.edit
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.health.BodyProfile
import com.fitnessquest.rpg.data.health.BodyProfileSource
import com.fitnessquest.rpg.domain.CardioPlacement
import com.fitnessquest.rpg.domain.EffortMethod
import com.fitnessquest.rpg.domain.Equipment
import com.fitnessquest.rpg.domain.FitnessGoal
import com.fitnessquest.rpg.domain.FitnessLevel
import com.fitnessquest.rpg.domain.MuscleFocus
import com.fitnessquest.rpg.domain.Split
import com.fitnessquest.rpg.domain.TrainingProfile
import com.fitnessquest.rpg.domain.WorkoutDurationMins
import com.fitnessquest.shared.wear.HrZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** User preferences that live outside the game database: units and training profile. */
class UserPrefs(context: Context) {

    private lateinit var repository: GameRepository
    fun setRepository(repo: GameRepository) { repository = repo }

    private val prefs = context.getSharedPreferences("fitquest_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- Play onboarding (UI/Local only) ----

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, value) }
        _onboardingComplete.value = value
    }

    private val _onboardingStep = MutableStateFlow(prefs.getString(KEY_ONBOARDING_STEP, null))
    val onboardingStep: StateFlow<String?> = _onboardingStep

    fun setOnboardingStep(step: String?) {
        prefs.edit {
            step?.let { putString(KEY_ONBOARDING_STEP, it) } ?: remove(KEY_ONBOARDING_STEP)
        }
        _onboardingStep.value = step
    }

    // ---- Profile & Progress (Synced to CharacterEntity) ----

    private val _fitnessGoal = MutableStateFlow(FitnessGoal.BUILD_MUSCLE)
    val fitnessGoal: StateFlow<FitnessGoal> = _fitnessGoal

    fun setFitnessGoal(goal: FitnessGoal) {
        updateCharacter { it.copy(fitnessGoal = goal.name) }
    }

    private val _muscleFocus = MutableStateFlow(MuscleFocus.BALANCED)
    val muscleFocus: StateFlow<MuscleFocus> = _muscleFocus

    fun setMuscleFocus(focus: MuscleFocus) {
        updateCharacter { it.copy(muscleFocus = focus.name) }
    }

    private val _cardioPlacement = MutableStateFlow(CardioPlacement.NONE)
    val cardioPlacement: StateFlow<CardioPlacement> = _cardioPlacement

    fun setCardioPlacement(placement: CardioPlacement) {
        updateCharacter { it.copy(cardioPlacement = placement.name) }
    }

    private val _workoutDuration = MutableStateFlow(WorkoutDurationMins.STANDARD)
    val workoutDuration: StateFlow<WorkoutDurationMins> = _workoutDuration

    fun setWorkoutDuration(duration: WorkoutDurationMins) {
        updateCharacter { it.copy(workoutDuration = duration.name) }
    }

    private val _firstWorkoutDone = MutableStateFlow(value = false)
    val firstWorkoutDone: StateFlow<Boolean> = _firstWorkoutDone

    private val _encounterClaimedThisTravel = MutableStateFlow(value = false)
    val encounterClaimedThisTravel: StateFlow<Boolean> = _encounterClaimedThisTravel

    private val _pendingEncounterStrBoost = MutableStateFlow(value = false)
    val pendingEncounterStrBoost: StateFlow<Boolean> = _pendingEncounterStrBoost

    fun resetTravelEncounter() {
        updateCharacter { it.copy(encounterClaimedThisTravel = false) }
    }

    fun claimTravelEncounter(): Boolean {
        if (_encounterClaimedThisTravel.value) return false
        updateCharacter { it.copy(encounterClaimedThisTravel = true, pendingEncounterStrBoost = true) }
        return true
    }

    fun consumeEncounterStrBoost(): Boolean {
        if (!_pendingEncounterStrBoost.value) return false
        updateCharacter { it.copy(pendingEncounterStrBoost = false) }
        return true
    }

    fun setFirstWorkoutDone(value: Boolean) {
        updateCharacter { it.copy(firstWorkoutDone = value) }
    }

    // ---- Units & Feedback (Local Settings) ----

    private val _imperial = MutableStateFlow(prefs.getBoolean(KEY_IMPERIAL, false))
    val imperial: StateFlow<Boolean> = _imperial

    fun setImperial(value: Boolean) {
        prefs.edit { putBoolean(KEY_IMPERIAL, value) }
        _imperial.value = value
    }

    private val _sound = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val sound: StateFlow<Boolean> = _sound

    fun setSound(value: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, value) }
        _sound.value = value
    }

    private val _haptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val haptics: StateFlow<Boolean> = _haptics

    fun setHaptics(value: Boolean) {
        prefs.edit { putBoolean(KEY_HAPTICS, value) }
        _haptics.value = value
    }

    private val _lowPowerUi = MutableStateFlow(prefs.getBoolean(KEY_LOW_POWER_UI, false))
    val lowPowerUi: StateFlow<Boolean> = _lowPowerUi

    fun setLowPowerUi(value: Boolean) {
        prefs.edit { putBoolean(KEY_LOW_POWER_UI, value) }
        _lowPowerUi.value = value
    }

    private val _showCardioIntensity = MutableStateFlow(prefs.getBoolean(KEY_CARDIO_INTENSITY, false))
    val showCardioIntensity: StateFlow<Boolean> = _showCardioIntensity

    fun setShowCardioIntensity(value: Boolean) {
        prefs.edit { putBoolean(KEY_CARDIO_INTENSITY, value) }
        _showCardioIntensity.value = value
    }

    // ---- Developer sandbox ----

    private val _developerSandbox = MutableStateFlow(prefs.getBoolean(KEY_DEV_SANDBOX, false))
    val developerSandbox: StateFlow<Boolean> = _developerSandbox

    fun setDeveloperSandbox(value: Boolean) {
        prefs.edit { putBoolean(KEY_DEV_SANDBOX, value) }
        _developerSandbox.value = value
    }

    // ---- Trophies ----

    private val _claimedTrophies = MutableStateFlow(emptySet<String>())
    val claimedTrophies: StateFlow<Set<String>> = _claimedTrophies

    // ---- Premium ----

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium

    fun setPremium(value: Boolean) {
        prefs.edit { putBoolean(KEY_PREMIUM, value) }
        _isPremium.value = value
    }

    // ---- Soreness & Rest Day Buff State ----

    private val _soreMuscles = MutableStateFlow(prefs.getStringSet("sore_muscles", emptySet()) ?: emptySet())
    val soreMuscles: StateFlow<Set<String>> = _soreMuscles

    fun setSoreMuscles(muscles: Set<String>) {
        prefs.edit { putStringSet("sore_muscles", muscles) }
        _soreMuscles.value = muscles
    }

    private val _wellRestedBuff = MutableStateFlow(prefs.getBoolean("well_rested_buff", false))
    val wellRestedBuff: StateFlow<Boolean> = _wellRestedBuff

    fun setWellRestedBuff(active: Boolean) {
        prefs.edit { putBoolean("well_rested_buff", active) }
        _wellRestedBuff.value = active
    }

    fun consumeWellRestedBuff(): Boolean {
        if (!_wellRestedBuff.value) return false
        setWellRestedBuff(false)
        return true
    }

    fun redeemPremiumCode(raw: String): Boolean {
        val code = raw.trim().uppercase().replace(' ', '-')
        if (code !in PREMIUM_REDEEM_CODES) return false
        setPremium(true)
        return true
    }

    // ---- Username claim ----

    private val _usernameSet = MutableStateFlow(prefs.getBoolean(KEY_USERNAME_SET, false))
    val usernameSet: StateFlow<Boolean> = _usernameSet

    private val _usernameKey = MutableStateFlow(prefs.getString(KEY_USERNAME_KEY, null))
    val usernameKey: StateFlow<String?> = _usernameKey

    fun setUsernameClaim(normalizedKey: String) {
        prefs.edit {
            putBoolean(KEY_USERNAME_SET, true)
            putString(KEY_USERNAME_KEY, normalizedKey)
        }
        _usernameSet.value = true
        _usernameKey.value = normalizedKey
    }

    fun clearUsernameClaim() {
        prefs.edit {
            putBoolean(KEY_USERNAME_SET, false)
            remove(KEY_USERNAME_KEY)
        }
        _usernameSet.value = false
        _usernameKey.value = null
    }

    // ---- Bounties & Campaigns ----

    private val _claimedBounties = MutableStateFlow(emptySet<String>())
    val claimedBounties: StateFlow<Set<String>> = _claimedBounties

    private val _bountyDay = MutableStateFlow(-1L)
    private val _bountyBattlesStart = MutableStateFlow(0)
    private val _campaignBattlesStart = MutableStateFlow(0)

    private val _waterGlasses = MutableStateFlow(0)
    val waterGlasses: StateFlow<Int> = _waterGlasses

    private val _stretchDone = MutableStateFlow(false)
    val stretchDone: StateFlow<Boolean> = _stretchDone

    fun ensureBountyDay(todayEpochDay: Long, currentBattlesWon: Int): Boolean {
        if (_bountyDay.value == todayEpochDay) return false
        updateCharacter {
            it.copy(
                bountyDay = todayEpochDay,
                bountyBattlesStart = currentBattlesWon,
                waterGlasses = 0,
                stretchDone = false,
                claimedBounties = "",
            )
        }
        return true
    }

    fun bountyBattlesStart(todayEpochDay: Long, currentBattlesWon: Int): Int {
        return if (_bountyDay.value == todayEpochDay) _bountyBattlesStart.value else currentBattlesWon
    }

    fun logWaterGlass() {
        val next = (_waterGlasses.value + 1).coerceAtMost(20)
        updateCharacter { it.copy(waterGlasses = next) }
    }

    fun markStretchDone() {
        updateCharacter { it.copy(stretchDone = true) }
    }

    private val _claimedCampaigns = MutableStateFlow(emptySet<String>())
    val claimedCampaigns: StateFlow<Set<String>> = _claimedCampaigns

    private val _campaignWeek = MutableStateFlow(-1L)
    val campaignWeek: StateFlow<Long> = _campaignWeek

    fun ensureCampaignWeek(weekStartEpochDay: Long, currentBattlesWon: Int): Int {
        if (_campaignWeek.value != weekStartEpochDay) {
            updateCharacter {
                it.copy(
                    campaignWeek = weekStartEpochDay,
                    campaignBattlesStart = currentBattlesWon,
                    claimedCampaigns = ""
                )
            }
        }
        return currentBattlesWon
    }

    fun campaignBattlesStart(weekStartEpochDay: Long, currentBattlesWon: Int): Int {
        return if (_campaignWeek.value == weekStartEpochDay) _campaignBattlesStart.value else currentBattlesWon
    }

    // ---- Effort & Plate Calculator ----

    private val _effortMethod = MutableStateFlow(EffortMethod.OFF)
    val effortMethod: StateFlow<EffortMethod> = _effortMethod

    fun setEffortMethod(method: EffortMethod) {
        updateCharacter { it.copy(effortMethod = method.name) }
    }

    private val _plateBarKg = MutableStateFlow<Double?>(null)
    val plateBarKg: StateFlow<Double?> = _plateBarKg

    fun setPlateBarKg(kg: Double) {
        updateCharacter { it.copy(plateBarKg = kg) }
    }

    // ---- Body metrics ----

    private val _bodyProfile = MutableStateFlow(BodyProfile())
    val bodyProfile: StateFlow<BodyProfile> = _bodyProfile

    fun bodyWeightKg(): Double? = _bodyProfile.value.bodyWeightKg
    fun ageYears(): Int = _bodyProfile.value.ageYears ?: 30
    fun maxHr(): Int = HrZone.estimatedMaxHr(ageYears())

    fun saveBodyProfileFromHealthConnect(profile: BodyProfile) {
        updateCharacter {
            it.copy(
                bodyWeightKg = profile.bodyWeightKg ?: it.bodyWeightKg,
                heightM = profile.heightM ?: it.heightM,
                dateOfBirthEpoch = profile.dateOfBirthEpoch ?: it.dateOfBirthEpoch
            )
        }
    }

    fun saveBodyProfileManual(
        bodyWeightKg: Double?,
        heightM: Double?,
        dateOfBirthEpoch: Long?,
        clearMissing: Boolean = false
    ) {
        updateCharacter {
            it.copy(
                bodyWeightKg = bodyWeightKg ?: if (clearMissing) null else it.bodyWeightKg,
                heightM = heightM ?: if (clearMissing) null else it.heightM,
                dateOfBirthEpoch = dateOfBirthEpoch ?: if (clearMissing) null else it.dateOfBirthEpoch
            )
        }
    }

    // ---- Training profile ----

    private val _profile = MutableStateFlow(TrainingProfile())
    val profile: StateFlow<TrainingProfile> = _profile

    fun saveProfile(profile: TrainingProfile) {
        updateCharacter {
            it.copy(
                trainingEquipment = profile.equipment.joinToString(",") { e -> e.name },
                trainingDaysPerWeek = profile.daysPerWeek,
                trainingSplit = profile.split.name,
                trainingLevel = profile.level.name
            )
        }
    }

    // ---- Reactivity ----

    fun start() {
        scope.launch {
            repository.character.collectLatest { c ->
                _encounterClaimedThisTravel.value = c.encounterClaimedThisTravel
                _pendingEncounterStrBoost.value = c.pendingEncounterStrBoost
                _firstWorkoutDone.value = c.firstWorkoutDone
                _claimedTrophies.value = c.claimedTrophies.split(",").filter { it.isNotBlank() }.toSet()
                
                _bountyDay.value = c.bountyDay
                _bountyBattlesStart.value = c.bountyBattlesStart
                _waterGlasses.value = c.waterGlasses
                _stretchDone.value = c.stretchDone
                _claimedBounties.value = c.claimedBounties.split(",").filter { it.isNotBlank() }.toSet()
                
                _campaignWeek.value = c.campaignWeek
                _campaignBattlesStart.value = c.campaignBattlesStart
                _claimedCampaigns.value = c.claimedCampaigns.split(",").filter { it.isNotBlank() }.toSet()
                
                _effortMethod.value = runCatching { EffortMethod.valueOf(c.effortMethod) }.getOrDefault(EffortMethod.OFF)
                _plateBarKg.value = c.plateBarKg
                
                _fitnessGoal.value = runCatching { FitnessGoal.valueOf(c.fitnessGoal) }.getOrDefault(FitnessGoal.BUILD_MUSCLE)
                _muscleFocus.value = runCatching { MuscleFocus.valueOf(c.muscleFocus) }.getOrDefault(MuscleFocus.BALANCED)
                _cardioPlacement.value = runCatching { CardioPlacement.valueOf(c.cardioPlacement) }.getOrDefault(CardioPlacement.NONE)
                _workoutDuration.value = runCatching { WorkoutDurationMins.valueOf(c.workoutDuration) }.getOrDefault(WorkoutDurationMins.STANDARD)

                _bodyProfile.value = BodyProfile(
                    bodyWeightKg = c.bodyWeightKg,
                    heightM = c.heightM,
                    dateOfBirthEpoch = c.dateOfBirthEpoch,
                    source = BodyProfileSource.MANUAL // Simplified
                )
                
                val equipment = c.trainingEquipment.split(",")
                    .mapNotNull { name -> runCatching { Equipment.valueOf(name) }.getOrNull() }
                    .toSet()
                    .ifEmpty { setOf(Equipment.BODYWEIGHT) }
                
                _profile.value = TrainingProfile(
                    equipment = equipment,
                    daysPerWeek = c.trainingDaysPerWeek,
                    split = runCatching { Split.valueOf(c.trainingSplit) }.getOrDefault(Split.FULL_BODY),
                    level = runCatching { FitnessLevel.valueOf(c.trainingLevel) }.getOrDefault(FitnessLevel.BEGINNER)
                )
            }
        }
    }

    private fun updateCharacter(block: (CharacterEntity) -> CharacterEntity) {
        scope.launch {
            val c = repository.getCharacter()
            repository.updateCharacter(block(c))
        }
    }

    private companion object {
        const val KEY_IMPERIAL = "units_imperial"
        const val KEY_SOUND = "feedback_sound"
        const val KEY_HAPTICS = "feedback_haptics"
        const val KEY_LOW_POWER_UI = "graphics_low_power"
        const val KEY_CARDIO_INTENSITY = "cardio_show_intensity"
        const val KEY_PREMIUM = "is_premium"
        const val KEY_DEV_SANDBOX = "developer_sandbox"
        val PREMIUM_REDEEM_CODES = setOf(
            "FITQUEST-PREMIUM",
            "ALLY-PASS",
            "GUILDMASTER"
        )
        const val KEY_USERNAME_SET = "username_set"
        const val KEY_USERNAME_KEY = "username_key"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_ONBOARDING_STEP = "onboarding_step"
        const val KEY_HEVY_API_KEY = "hevy_api_key"
        const val KEY_HEVY_AUTO_SYNC = "hevy_auto_sync_enabled"
        const val KEY_HEVY_LAST_SYNC = "hevy_last_sync_timestamp"
        const val KEY_PERMISSIONS_REPAIR_SHOWN = "permissions_repair_shown"
        const val KEY_USE_LOCAL_AI = "use_local_ai_by_default"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"
    }

    // ---- Local AI ----
    private val _useLocalAi = MutableStateFlow(prefs.getBoolean(KEY_USE_LOCAL_AI, false))
    val useLocalAi: StateFlow<Boolean> = _useLocalAi

    fun setUseLocalAi(value: Boolean) {
        prefs.edit { putBoolean(KEY_USE_LOCAL_AI, value) }
        _useLocalAi.value = value
    }


    // ---- Permissions Repair ----
    private val _permissionsRepairShown = MutableStateFlow(prefs.getBoolean(KEY_PERMISSIONS_REPAIR_SHOWN, false))
    val permissionsRepairShown: StateFlow<Boolean> = _permissionsRepairShown

    fun setPermissionsRepairShown(shown: Boolean) {
        prefs.edit { putBoolean(KEY_PERMISSIONS_REPAIR_SHOWN, shown) }
        _permissionsRepairShown.value = shown
    }


    // ---- Hevy API Preferences ----
    private val _hevyApiKey = MutableStateFlow(prefs.getString(KEY_HEVY_API_KEY, "").orEmpty())
    val hevyApiKey: StateFlow<String> = _hevyApiKey

    fun setHevyApiKey(key: String) {
        prefs.edit { putString(KEY_HEVY_API_KEY, key.trim()) }
        _hevyApiKey.value = key.trim()
    }

    private val _hevyAutoSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_HEVY_AUTO_SYNC, false))
    val hevyAutoSyncEnabled: StateFlow<Boolean> = _hevyAutoSyncEnabled

    fun setHevyAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HEVY_AUTO_SYNC, enabled) }
        _hevyAutoSyncEnabled.value = enabled
    }

    private val _hevyLastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_HEVY_LAST_SYNC, 0L))
    val hevyLastSyncTimestamp: StateFlow<Long> = _hevyLastSyncTimestamp

    fun setHevyLastSyncTimestamp(timestamp: Long) {
        prefs.edit { putLong(KEY_HEVY_LAST_SYNC, timestamp) }
        _hevyLastSyncTimestamp.value = timestamp
    }

    // ---- Version Tracking ----
    fun getLastSeenVersion(): Int = prefs.getInt(KEY_LAST_SEEN_VERSION, 0)

    fun setLastSeenVersion(versionCode: Int) {
        prefs.edit { putInt(KEY_LAST_SEEN_VERSION, versionCode) }
    }
}
