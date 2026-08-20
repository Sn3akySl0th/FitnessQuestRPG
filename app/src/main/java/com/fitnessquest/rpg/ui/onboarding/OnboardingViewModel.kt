package com.fitnessquest.rpg.ui.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitnessquest.rpg.AppContainer
import com.fitnessquest.rpg.data.auth.AccountState
import com.fitnessquest.rpg.domain.CardioPlacement
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.CharacterRace
import com.fitnessquest.rpg.domain.Equipment
import com.fitnessquest.rpg.domain.FitnessGoal
import com.fitnessquest.rpg.domain.FitnessLevel
import com.fitnessquest.rpg.domain.MuscleFocus
import com.fitnessquest.rpg.domain.OnboardingStarterQuests
import com.fitnessquest.rpg.domain.Units
import com.fitnessquest.rpg.domain.WorkoutDurationMins
import com.fitnessquest.rpg.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val returning: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val programSummary: List<String> = emptyList(),
    /** In-memory demo moves — never written as a real workout. */
    val demoExercises: List<com.fitnessquest.rpg.ui.screens.SessionExercise> = emptyList(),
    val demoTitle: String = "Demo Quest",
    val demoSessionKey: Long = -2L,
    val inSession: Boolean = false,
    val usernameBusy: Boolean = false,
    val usernameError: String? = null
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui.asStateFlow()

    val account: StateFlow<AccountState> = container.auth.state
    val imperial = container.prefs.imperial
    val profile = container.prefs.profile
    val bodyProfile = container.prefs.bodyProfile
    val fitnessGoal = container.prefs.fitnessGoal
    val muscleFocus = container.prefs.muscleFocus
    val cardioPlacement = container.prefs.cardioPlacement
    val workoutDuration = container.prefs.workoutDuration
    val isPremium = container.prefs.isPremium
    val wear = container.wearPresence.state
    val usernameSet = container.prefs.usernameSet
    val healthConnect = container.healthConnect

    val progressFraction: Float
        get() {
            val ordered = progressSteps(_ui.value.returning)
            val idx = ordered.indexOf(_ui.value.step).coerceAtLeast(0)
            return if (ordered.isEmpty()) 0f else (idx + 1f) / ordered.size
        }

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        container.auth.ensureSignedIn()
        val character = container.repository.character.first()
        val hasClass = character.characterClass != null
        val hasUsername = container.prefs.usernameSet.value
        val hasAccount = container.auth.state.value.hasAccount
        val firstWorkoutDone = container.prefs.firstWorkoutDone.value
        val savedStep = OnboardingStep.fromName(container.prefs.onboardingStep.value)

        when {
            hasAccount && hasClass && hasUsername -> {
                container.prefs.setOnboardingComplete(true)
                return
            }
            // Existing guest with identity: jump to account wall.
            !hasAccount && hasClass && hasUsername -> {
                goTo(OnboardingStep.ACCOUNT_PITCH)
            }
            savedStep != null && savedStep != OnboardingStep.WELCOME -> {
                // Rebuild an in-memory demo if they left mid-flow after terms.
                if (savedStep == OnboardingStep.PROGRAM ||
                    savedStep == OnboardingStep.FIRST_WORKOUT ||
                    savedStep == OnboardingStep.REWARDS
                ) {
                    val plan = OnboardingStarterQuests.build(
                        profile = container.prefs.profile.value,
                        goal = container.prefs.fitnessGoal.value,
                        duration = container.prefs.workoutDuration.value,
                        focus = container.prefs.muscleFocus.value,
                        cardio = container.prefs.cardioPlacement.value
                    )
                    _ui.value = _ui.value.copy(
                        step = if (savedStep == OnboardingStep.FIRST_WORKOUT) {
                            OnboardingStep.PROGRAM
                        } else {
                            savedStep
                        },
                        demoTitle = plan.workoutName,
                        demoExercises = plan.exercises.map { e ->
                            com.fitnessquest.rpg.ui.screens.SessionExercise(
                                name = e.exerciseName,
                                category = e.category,
                                targetSets = e.targetSets,
                                targetReps = e.targetReps
                            )
                        },
                        programSummary = plan.summaryLines,
                        inSession = false
                    )
                } else {
                    _ui.value = _ui.value.copy(step = savedStep)
                }
            }
            else -> goTo(OnboardingStep.WELCOME)
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null, usernameError = null)
    }

    fun startNewHero() {
        _ui.value = _ui.value.copy(returning = false)
        goTo(OnboardingStep.INTRO)
    }

    fun startReturning() {
        _ui.value = _ui.value.copy(returning = true)
        goTo(OnboardingStep.RETURNING_SIGN_IN)
    }

    fun continueFromIntro() = goTo(OnboardingStep.UNITS)

    fun setImperial(imperial: Boolean) {
        container.prefs.setImperial(imperial)
    }

    fun continueUnits() = goTo(OnboardingStep.HEALTH_CONNECT)

    fun skipHealthConnect() = goTo(OnboardingStep.GENDER)

    fun applyHealthConnectAndContinue() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            val profile = runCatching {
                container.healthConnect.readLatest()
            }.getOrElse {
                _ui.value = _ui.value.copy(
                    busy = false,
                    error = it.message ?: "Could not read Health Connect"
                )
                return@launch
            }
            container.prefs.saveBodyProfileFromHealthConnect(profile)
            _ui.value = _ui.value.copy(
                busy = false,
                error = if (profile.heightM == null && profile.bodyWeightKg == null) {
                    "Connected, but no height or weight was found. You can enter them next, or check Height + history access in Health Connect."
                } else {
                    null
                }
            )
            goTo(OnboardingStep.GENDER)
        }
    }

    fun setGender(gender: String) {
        viewModelScope.launch {
            val c = container.repository.character.first()
            container.repository.updateAppearance(
                skin = c.skinColor,
                hair = c.hairColor,
                underwear = c.underwearColor,
                eye = c.eyeColor,
                style = c.hairStyle,
                gender = gender,
                braColor = c.braColor,
                race = c.race.ifBlank { CharacterRace.HUMAN.name }
            )
            goTo(OnboardingStep.DOB)
        }
    }

    fun setDob(epochMilli: Long) {
        val body = container.prefs.bodyProfile.value
        container.prefs.saveBodyProfileManual(
            bodyWeightKg = body.bodyWeightKg,
            heightM = body.heightM,
            dateOfBirthEpoch = epochMilli
        )
        goTo(stepAfterDob())
    }

    fun setHeightMeters(meters: Double) {
        val body = container.prefs.bodyProfile.value
        container.prefs.saveBodyProfileManual(
            bodyWeightKg = body.bodyWeightKg,
            heightM = meters.coerceIn(0.5, 3.0),
            dateOfBirthEpoch = body.dateOfBirthEpoch
        )
        goTo(stepAfterHeight())
    }

    fun setWeightDisplay(display: Double) {
        val kg = Units.toKg(display, container.prefs.imperial.value)
        val body = container.prefs.bodyProfile.value
        container.prefs.saveBodyProfileManual(
            bodyWeightKg = kg,
            heightM = body.heightM,
            dateOfBirthEpoch = body.dateOfBirthEpoch
        )
        goTo(OnboardingStep.GOAL)
    }

    fun skipDob() = goTo(stepAfterDob())

    fun skipHeight() = goTo(stepAfterHeight())

    fun skipWeight() = goTo(OnboardingStep.GOAL)

    fun skipBodyMetric(next: OnboardingStep) = goTo(next)

    /** Skip height/weight when Health Connect (or a prior entry) already filled them. */
    private fun stepAfterDob(): OnboardingStep {
        val body = container.prefs.bodyProfile.value
        return when {
            body.heightM == null -> OnboardingStep.HEIGHT
            body.bodyWeightKg == null -> OnboardingStep.WEIGHT
            else -> OnboardingStep.GOAL
        }
    }

    private fun stepAfterHeight(): OnboardingStep {
        val body = container.prefs.bodyProfile.value
        return if (body.bodyWeightKg == null) OnboardingStep.WEIGHT else OnboardingStep.GOAL
    }

    fun setGoal(goal: FitnessGoal) {
        container.prefs.setFitnessGoal(goal)
        goTo(OnboardingStep.EXPERIENCE)
    }

    fun setExperience(level: FitnessLevel) {
        val p = container.prefs.profile.value
        container.prefs.saveProfile(p.copy(level = level))
        goTo(OnboardingStep.FREQUENCY)
    }

    fun setFrequency(days: Int) {
        val p = container.prefs.profile.value
        val updated = p.copy(daysPerWeek = days.coerceIn(1, 7))
        container.prefs.saveProfile(updated.copy(split = updated.recommendedSplit))
        goTo(OnboardingStep.CARDIO)
    }

    fun setCardio(placement: CardioPlacement) {
        container.prefs.setCardioPlacement(placement)
        val p = container.prefs.profile.value
        val gear = p.equipment.toMutableSet()
        if (placement != CardioPlacement.NONE) gear += Equipment.CARDIO_GEAR
        container.prefs.saveProfile(p.copy(equipment = gear))
        goTo(OnboardingStep.DURATION)
    }

    fun setDuration(duration: WorkoutDurationMins) {
        container.prefs.setWorkoutDuration(duration)
        goTo(OnboardingStep.FOCUS)
    }

    fun setFocus(focus: MuscleFocus) {
        container.prefs.setMuscleFocus(focus)
        goTo(OnboardingStep.GYM)
    }

    fun setGymPreset(equipment: Set<Equipment>) {
        val p = container.prefs.profile.value
        val cardio = container.prefs.cardioPlacement.value
        val merged = equipment.toMutableSet()
        if (cardio != CardioPlacement.NONE) merged += Equipment.CARDIO_GEAR
        if (merged.isEmpty()) merged += Equipment.BODYWEIGHT
        container.prefs.saveProfile(p.copy(equipment = merged))
        goTo(OnboardingStep.EQUIPMENT)
    }

    fun toggleEquipment(item: Equipment) {
        val p = container.prefs.profile.value
        val next = p.equipment.toMutableSet()
        if (item in next) {
            if (next.size > 1) next.remove(item)
        } else {
            next.add(item)
        }
        container.prefs.saveProfile(p.copy(equipment = next))
    }

    fun continueEquipment() = goTo(OnboardingStep.TERMS)

    fun acceptTerms() {
        viewModelScope.launch { buildProgram() }
    }

    private suspend fun buildProgram() {
        _ui.value = _ui.value.copy(busy = true, error = null)
        val plan = OnboardingStarterQuests.build(
            profile = container.prefs.profile.value,
            goal = container.prefs.fitnessGoal.value,
            duration = container.prefs.workoutDuration.value,
            focus = container.prefs.muscleFocus.value,
            cardio = container.prefs.cardioPlacement.value
        )
        // In-memory only — demo must not pollute the workout library or hero stats.
        _ui.value = _ui.value.copy(
            busy = false,
            demoTitle = plan.workoutName,
            demoSessionKey = -2L,
            demoExercises = plan.exercises.map { e ->
                com.fitnessquest.rpg.ui.screens.SessionExercise(
                    name = e.exerciseName,
                    category = e.category,
                    targetSets = e.targetSets,
                    targetReps = e.targetReps
                )
            },
            programSummary = plan.summaryLines
        )
        goTo(OnboardingStep.PROGRAM)
    }

    fun startFirstWorkout() {
        if (_ui.value.demoExercises.isEmpty()) {
            viewModelScope.launch { buildProgram() }
            return
        }
        _ui.value = _ui.value.copy(
            inSession = true,
            demoSessionKey = System.currentTimeMillis()
        )
        goTo(OnboardingStep.FIRST_WORKOUT)
    }

    fun onFirstWorkoutFinished() {
        container.prefs.setFirstWorkoutDone(true)
        _ui.value = _ui.value.copy(inSession = false)
        goTo(OnboardingStep.REWARDS)
    }

    /** Skip the practice session and continue onboarding (no stats saved either way). */
    fun skipFirstWorkout() {
        container.prefs.setFirstWorkoutDone(true)
        _ui.value = _ui.value.copy(inSession = false)
        goTo(OnboardingStep.REWARDS)
    }

    fun continueAfterRewards() = goTo(OnboardingStep.NOTIFICATIONS)

    fun skipNotifications() = goTo(OnboardingStep.STEPS)

    fun continueAfterNotifications() = goTo(OnboardingStep.STEPS)

    fun skipSteps() = goTo(OnboardingStep.WEAR)

    fun continueAfterSteps() = goTo(OnboardingStep.WEAR)

    fun skipWear() = goTo(OnboardingStep.CLASS)

    fun continueAfterWear() = goTo(OnboardingStep.CLASS)

    fun refreshWear() = container.wearPresence.refreshNow()

    fun chooseClass(cls: CharacterClass) {
        if (cls.requiresPremium && !container.prefs.isPremium.value) return
        viewModelScope.launch {
            container.repository.chooseClass(cls)
            goTo(OnboardingStep.USERNAME)
        }
    }

    fun clearUsernameError() {
        _ui.value = _ui.value.copy(usernameError = null)
    }

    fun claimUsername(name: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(usernameBusy = true, usernameError = null)
            try {
                container.usernames.claim(name)
                    .onSuccess {
                        _ui.value = _ui.value.copy(usernameBusy = false)
                        goTo(OnboardingStep.ACCOUNT_PITCH)
                    }
                    .onFailure {
                        _ui.value = _ui.value.copy(
                            usernameBusy = false,
                            usernameError = it.message ?: "Could not claim that username"
                        )
                    }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    usernameBusy = false,
                    usernameError = t.message ?: "Could not claim that username"
                )
            }
        }
    }

    fun continueAccountPitch() = goTo(OnboardingStep.ACCOUNT)

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            container.auth.signInWithGoogle(activity)
                .onSuccess {
                    _ui.value = _ui.value.copy(busy = false)
                    afterAccountLinked()
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        error = it.message ?: "Google sign-in failed"
                    )
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            container.auth.signUpWithEmail(email.trim(), password)
                .onSuccess {
                    _ui.value = _ui.value.copy(busy = false)
                    afterAccountLinked()
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        error = it.message ?: "Could not create account"
                    )
                }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            container.auth.signInWithEmail(email.trim(), password)
                .onSuccess {
                    _ui.value = _ui.value.copy(busy = false)
                    afterAccountLinked()
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        busy = false,
                        error = it.message ?: "Could not sign in"
                    )
                }
        }
    }

    private suspend fun afterAccountLinked() {
        if (!_ui.value.returning) {
            completeOnboarding()
            return
        }

        _ui.value = _ui.value.copy(busy = true)
        
        // Explicitly trigger a sync/reconcile and wait for it.
        container.sync.forceReconcile()

        val character = container.repository.character.first()
        when {
            character.characterClass == null -> {
                _ui.value = _ui.value.copy(busy = false)
                goTo(OnboardingStep.CLASS)
            }
            character.name.isNotBlank() && !character.name.equals("Hero", ignoreCase = true) -> {
                container.prefs.setUsernameClaim(character.name)
                container.usernames.syncPublicPointer()
                completeOnboarding()
            }
            !container.prefs.usernameSet.value -> {
                container.usernames.ensureClaimedForExistingName()
                if (!container.prefs.usernameSet.value) {
                    _ui.value = _ui.value.copy(busy = false)
                    goTo(OnboardingStep.USERNAME)
                } else {
                    completeOnboarding()
                }
            }
            else -> completeOnboarding()
        }
    }

    fun completeOnboarding() {
        container.prefs.setOnboardingStep(null)
        container.prefs.setOnboardingComplete(true)
    }

    fun goBack() {
        val ordered = progressSteps(_ui.value.returning)
        val currentIdx = ordered.indexOf(_ui.value.step)
        if (currentIdx > 0) {
            goTo(ordered[currentIdx - 1])
        }
    }

    private fun goTo(step: OnboardingStep) {
        container.prefs.setOnboardingStep(step.name)
        _ui.value = _ui.value.copy(step = step, error = null)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(appContainer) }
        }

        fun progressSteps(returning: Boolean): List<OnboardingStep> =
            if (returning) {
                listOf(
                    OnboardingStep.WELCOME,
                    OnboardingStep.RETURNING_SIGN_IN,
                    OnboardingStep.CLASS,
                    OnboardingStep.USERNAME
                )
            } else {
                listOf(
                    OnboardingStep.WELCOME,
                    OnboardingStep.INTRO,
                    OnboardingStep.UNITS,
                    OnboardingStep.HEALTH_CONNECT,
                    OnboardingStep.GENDER,
                    OnboardingStep.DOB,
                    OnboardingStep.HEIGHT,
                    OnboardingStep.WEIGHT,
                    OnboardingStep.GOAL,
                    OnboardingStep.EXPERIENCE,
                    OnboardingStep.FREQUENCY,
                    OnboardingStep.CARDIO,
                    OnboardingStep.DURATION,
                    OnboardingStep.FOCUS,
                    OnboardingStep.GYM,
                    OnboardingStep.EQUIPMENT,
                    OnboardingStep.TERMS,
                    OnboardingStep.PROGRAM,
                    OnboardingStep.FIRST_WORKOUT,
                    OnboardingStep.REWARDS,
                    OnboardingStep.NOTIFICATIONS,
                    OnboardingStep.STEPS,
                    OnboardingStep.WEAR,
                    OnboardingStep.CLASS,
                    OnboardingStep.USERNAME,
                    OnboardingStep.ACCOUNT_PITCH,
                    OnboardingStep.ACCOUNT
                )
            }
    }
}
