package com.fitnessquest.rpg

import android.app.Application
import com.fitnessquest.rpg.data.GameRepository
import com.fitnessquest.rpg.data.UserPrefs
import com.fitnessquest.rpg.data.ai.GeminiService
import com.fitnessquest.rpg.data.ai.LocalModelDownloader
import com.fitnessquest.rpg.data.ai.PlayAssetModelProvider
import com.fitnessquest.rpg.data.auth.AuthService
import com.fitnessquest.rpg.data.auth.UsernameService
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.exercises.ExerciseInfoService
import com.fitnessquest.rpg.data.guild.GuildService
import com.fitnessquest.rpg.data.health.HealthConnectProfile
import com.fitnessquest.rpg.data.media.MediaControllerManager
import com.fitnessquest.rpg.data.party.PartyService
import com.fitnessquest.rpg.data.steps.StepTracker
import com.fitnessquest.rpg.data.sync.SyncService
import com.fitnessquest.rpg.data.update.InAppUpdateService
import com.fitnessquest.rpg.data.update.PlayAppUpdateClient
import com.fitnessquest.rpg.data.wear.WearPresence
import com.fitnessquest.rpg.ui.effects.AudioEffects
import com.fitnessquest.rpg.ui.effects.HapticEffects
import com.fitnessquest.rpg.widget.HeroStatusWidgetProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val app: Application) {
    val database: AppDatabase = AppDatabase.get(app)
    val exerciseInfo: ExerciseInfoService = ExerciseInfoService(app)
    val prefs: UserPrefs = UserPrefs(app)
    val repository: GameRepository = GameRepository(database, exerciseInfo, prefs)
    val gemini: GeminiService = GeminiService(app)
    val auth: AuthService = AuthService(app)
    val usernames: UsernameService = UsernameService(database, auth, prefs)
    val sync: SyncService = SyncService(app, database, auth, prefs, usernames)
    val steps: StepTracker = StepTracker(app, repository)
    val party: PartyService = PartyService(app, repository, auth)
    val guild: GuildService = GuildService(app, repository, auth)
    val wearPresence: WearPresence = WearPresence(app)
    val healthConnect: HealthConnectProfile = HealthConnectProfile(app)
    val localAiModel: LocalModelDownloader = LocalModelDownloader(app)
    val playAssetModel: PlayAssetModelProvider = PlayAssetModelProvider(app)
    val music: MediaControllerManager = MediaControllerManager(app)
    val inAppUpdate: InAppUpdateService = InAppUpdateService(PlayAppUpdateClient(app))




    init {
        repository.setSyncService(sync)
        prefs.setRepository(repository)
        // Free guest username before Firebase abandons that uid on Google collision.
        auth.beforeAbandonAnonymous = { usernames.releaseClaim() }

        // Migrate any pre-existing Play Asset Delivery model to local filesDir storage
        val padFile = playAssetModel.getModelFile()
        if ((padFile != null) && padFile.exists()) {
            localAiModel.migrateFromPlayAsset(padFile)
        }
    }

    /** Set by FightScreen when an ambush battle ends; consumed by ActiveSession. */
    @Volatile
    var lastAmbushVictory: Boolean? = null
}

class FitQuestApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val crashlytics = FirebaseCrashlytics.getInstance().apply {
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            log("FitQuest startup begin")
        }
        container = AppContainer(this)
        com.fitnessquest.rpg.data.sync.OutboxWorker.enqueue(this)
        AudioEffects.soundEnabled = container.prefs.sound.value
        HapticEffects.hapticsEnabled = container.prefs.haptics.value
        appScope.launchStartup("repository.ensureSeeded", crashlytics) { container.repository.ensureSeeded() }
        appScope.launchStartup("auth.ensureSignedIn", crashlytics) { container.auth.ensureSignedIn() }
        appScope.launchStartup("usernames.syncPublicPointer", crashlytics) { container.usernames.syncPublicPointer() }
        appScope.launchStartup("heroWidget.refresh.collect", crashlytics) {
            container.repository.character.collect {
                HeroStatusWidgetProvider.refresh(this@FitQuestApp)
            }
        }
        startSafely("prefs.start", crashlytics) { container.prefs.start() }
        startSafely("sync.start", crashlytics) { container.sync.start() }
        startSafely("steps.start", crashlytics) { container.steps.start() }
        startSafely("party.start", crashlytics) { container.party.start() }
        startSafely("guild.start", crashlytics) { container.guild.start() }
        startSafely("wearPresence.start", crashlytics) { container.wearPresence.start() }
    }

    private fun startSafely(
        name: String,
        crashlytics: FirebaseCrashlytics,
        block: () -> Unit,
    ) {
        runCatching(block).onFailure { error ->
            crashlytics.log("Startup task failed: $name")
            crashlytics.recordException(error)
        }
    }

    private fun CoroutineScope.launchStartup(
        name: String,
        crashlytics: FirebaseCrashlytics,
        block: suspend () -> Unit
    ) {
        launch {
            runCatching { block() }.onFailure { error ->
                crashlytics.log("Startup coroutine failed: $name")
                crashlytics.recordException(error)
            }
        }
    }
}
