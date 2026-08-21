package com.fitnessquest.rpg.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Column
import com.fitnessquest.rpg.data.db.ActiveSessionWithDetails
import kotlinx.coroutines.launch

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.ui.components.DockTab
import com.fitnessquest.rpg.ui.components.FloatingGameDock
import com.fitnessquest.rpg.ui.components.LocalLowPowerUi
import com.fitnessquest.rpg.ui.onboarding.OnboardingScreen
import com.fitnessquest.rpg.ui.screens.ActiveSessionScreen
import com.fitnessquest.rpg.ui.screens.AiGeneratorScreen
import com.fitnessquest.rpg.ui.screens.BattleScreen
import com.fitnessquest.rpg.ui.screens.ExerciseLibraryScreen
import com.fitnessquest.rpg.ui.screens.FightScreen
import com.fitnessquest.rpg.ui.screens.HeroScreen
import com.fitnessquest.rpg.ui.screens.HeroViewModel
import com.fitnessquest.rpg.ui.screens.QuestHubScreen
import com.fitnessquest.rpg.ui.screens.HistoryScreen
import com.fitnessquest.rpg.ui.screens.RivalsScreen
import com.fitnessquest.rpg.ui.screens.ShopScreen
import com.fitnessquest.rpg.ui.screens.WorkoutDetailScreen
import com.fitnessquest.rpg.ui.screens.WorkoutEditorScreen
import com.fitnessquest.rpg.ui.screens.WorkoutsScreen
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import com.fitnessquest.rpg.data.update.InAppUpdateStatus
import com.fitnessquest.rpg.ui.components.BetaWelcomeDialog
import com.fitnessquest.rpg.ui.components.PREF_BETA_WELCOME_SHOWN
import com.fitnessquest.rpg.ui.components.InAppUpdateBanner
import com.fitnessquest.rpg.ui.components.WhatNewDialog
import com.fitnessquest.rpg.ui.onboarding.PermissionsConsolidator
import com.fitnessquest.rpg.BuildConfig
import com.fitnessquest.rpg.ui.screens.SessionDetailScreen

object Routes {
    const val HERO = "hero"
    const val HERO_DETAILS = "hero/details"
    const val HERO_GEAR = "hero/details/gear"
    const val HERO_SAGA = "hero/details/saga"
    const val TRAIN = "train"
    const val BATTLE = "battle"
    const val ALLIES = "allies"
    const val SHOP = "shop"
    const val EDITOR = "train/editor"
    const val EDITOR_EXISTING = "train/editor/{workoutId}"
    const val DETAIL = "train/detail/{workoutId}"
    const val HISTORY = "train/history"
    const val RECORDS = "train/records"
    const val EXERCISES = "train/exercises"
    const val AI = "train/ai"
    const val SESSION = "train/session/{workoutId}"
    const val SESSION_DETAIL = "train/session-detail/{sessionId}"
    const val FIGHT = "battle/fight/{monsterId}?ambush={ambush}"

    fun session(workoutId: Long) = "train/session/$workoutId"
    fun sessionDetail(sessionId: Long) = "train/session-detail/$sessionId"
    fun detail(workoutId: Long) = "train/detail/$workoutId"
    fun editor(workoutId: Long = -1L) =
        if (workoutId > 0) "train/editor/$workoutId" else EDITOR
    fun fight(monsterId: Int, ambush: Boolean = false) = "battle/fight/$monsterId?ambush=$ambush"
}

/** Bottom inset so scroll content clears the floating dock (dock overlays content). */
val LocalDockBottomInset = staticCompositionLocalOf { 0.dp }

/** Global snackbar host so messages show above the dock. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun rememberDockContentPadding(
    horizontal: Dp = 16.dp,
    top: Dp = 16.dp,
    extraBottom: Dp = 16.dp,
): PaddingValues {
    val inset = LocalDockBottomInset.current
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = top,
        bottom = extraBottom + inset
    )
}

private val tabs = listOf(
    DockTab(Routes.HERO, "Home", Icons.Filled.Home),
    DockTab(Routes.TRAIN, "Train", Icons.Filled.FitnessCenter),
    DockTab(Routes.BATTLE, "Battle", Icons.Filled.SportsMma),
    DockTab(Routes.ALLIES, "Allies", Icons.Filled.Groups),
    DockTab(Routes.SHOP, "Shop", Icons.Filled.Storefront)
)

/** Approximate height of floating pill + margin for list clearance. */
private val DockClearance = 100.dp

@Composable
fun FitQuestNav() {
    val context = LocalContext.current
    val container = (context.applicationContext as FitQuestApp).container
    val onboardingComplete by container.prefs.onboardingComplete.collectAsState()

    val account by container.auth.state.collectAsState()
    val character by container.repository.character.collectAsState(null)
    val usernameSet by container.prefs.usernameSet.collectAsState()
    val syncStatus by container.sync.status.collectAsState()
    val lowPowerUi by container.prefs.lowPowerUi.collectAsState()
    val leftHanded by container.prefs.leftHanded.collectAsState()

    LaunchedEffect(account, character, usernameSet, syncStatus.isReconciling) {
        if (!onboardingComplete && !syncStatus.isReconciling) {
            val c = character ?: return@LaunchedEffect
            val linked = account.hasAccount
            if (linked && usernameSet && (c.characterClass != null)) {
                container.prefs.setOnboardingComplete(value = true)
            }
        }
    }

    val hevyApiKey by container.prefs.hevyApiKey.collectAsState()
    val hevyAutoSync by container.prefs.hevyAutoSyncEnabled.collectAsState()

    LaunchedEffect(hevyApiKey, hevyAutoSync) {
        if (hevyAutoSync && hevyApiKey.isNotBlank()) {
            com.fitnessquest.rpg.data.importexport.WorkoutImportService.performBackgroundSync(
                apiKey = hevyApiKey,
                userPrefs = container.prefs,
                gameRepository = container.repository,
                gemini = container.gemini,
                renameTemplates = true
            )
        }
    }

    if (!onboardingComplete) {
        OnboardingScreen()
        return
    }

    var showWhatNew by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val lastSeen = container.prefs.getLastSeenVersion()
        if (lastSeen < BuildConfig.VERSION_CODE) {
            showWhatNew = true
            container.prefs.setLastSeenVersion(BuildConfig.VERSION_CODE)
        }
    }

    var showBetaWelcome by remember {
        mutableStateOf(!context.getSharedPreferences("fitnessrpg_user_prefs", android.content.Context.MODE_PRIVATE).getBoolean(PREF_BETA_WELCOME_SHOWN, false))
    }

    if (showBetaWelcome) {
        BetaWelcomeDialog(onDismiss = { showBetaWelcome = false })
    }

    if (showWhatNew && !showBetaWelcome) {
        WhatNewDialog(onDismiss = { showWhatNew = false })
    }

    val permissionsRepairShown by container.prefs.permissionsRepairShown.collectAsState()
    var showPermissionsConsolidator by remember { mutableStateOf(false) }

    LaunchedEffect(permissionsRepairShown) {
        if (!permissionsRepairShown) {
            val ctx = context
            val activityGranted = if (Build.VERSION.SDK_INT >= 29) {
                ContextCompat.checkSelfPermission(ctx, "android.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_GRANTED
            } else true
            val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(ctx, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
            } else true
            val healthGranted = container.healthConnect.hasCoreReadPermissions()
            
            if (!activityGranted || !notificationGranted || !healthGranted) {
                showPermissionsConsolidator = true
            } else {
                // If all are already granted, don't show it ever.
                container.prefs.setPermissionsRepairShown(true)
            }
        }
    }


    if (showPermissionsConsolidator) {
        PermissionsConsolidator(
            onDismiss = {
                showPermissionsConsolidator = false
                container.prefs.setPermissionsRepairShown(true)
            }
        )
        return
    }


    val navController = rememberNavController()
    val heroViewModel: HeroViewModel = viewModel(factory = HeroViewModel.Factory)
    val hasClaimableDailyBounty by heroViewModel.hasClaimableDailyBounty.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val heroDetailRoutes = setOf(Routes.HERO_DETAILS, Routes.HERO_SAGA, Routes.HERO_GEAR)
    val showBottomBar = currentRoute in tabs.map { it.route } || currentRoute in heroDetailRoutes
    val selectedDockRoute = if (currentRoute in heroDetailRoutes) Routes.HERO else currentRoute
    val dockInset = if (showBottomBar && !isLandscape) DockClearance else 0.dp
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        LocalDockBottomInset provides dockInset,
        LocalLowPowerUi provides lowPowerUi,
        LocalSnackbarHostState provides snackbarHostState
    ) {
        // Overlay dock (not Scaffold bottomBar) so page content shows in the gaps around the pill.
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HERO,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (showBottomBar && isLandscape && leftHanded) 74.dp else 0.dp,
                        end = if (showBottomBar && isLandscape && !leftHanded) 74.dp else 0.dp
                    )
            ) {
                composable(Routes.HERO) {
                    QuestHubScreen(
                        viewModel = heroViewModel,
                        onOpenHero = { navController.navigate(Routes.HERO_DETAILS) },
                        onOpenHeroGear = { navController.navigate(Routes.HERO_GEAR) },
                        onOpenSaga = { navController.navigate(Routes.HERO_SAGA) },
                        onOpenTraining = {
                            navController.navigate(Routes.TRAIN) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenBattle = {
                            navController.navigate(Routes.BATTLE) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onStartWorkout = { workoutId ->
                            navController.navigate(Routes.session(workoutId))
                        }
                    )
                }
                composable(Routes.HERO_DETAILS) {
                    HeroScreen(
                        viewModel = heroViewModel,
                        onOpenHome = {
                            navController.navigate(Routes.HERO) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HERO) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable(Routes.HERO_GEAR) {
                    HeroScreen(
                        viewModel = heroViewModel,
                        initialTab = 1,
                        onOpenHome = {
                            navController.navigate(Routes.HERO) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HERO) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable(Routes.HERO_SAGA) {
                    HeroScreen(
                        viewModel = heroViewModel,
                        initialTab = 2,
                        onOpenHome = {
                            navController.navigate(Routes.HERO) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HERO) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable(Routes.TRAIN) {
                    WorkoutsScreen(
                        onNewWorkout = { navController.navigate(Routes.EDITOR) },
                        onAiWorkout = { navController.navigate(Routes.AI) },
                        onExerciseLibrary = { navController.navigate(Routes.EXERCISES) },
                        onHistory = { navController.navigate(Routes.HISTORY) },
                        onRecords = { navController.navigate(Routes.RECORDS) },
                        onOpenWorkout = { id -> navController.navigate(Routes.detail(id)) },
                        onStartWorkout = { id -> navController.navigate(Routes.session(id)) },
                        onFreestyle = { navController.navigate(Routes.session(-1L)) }
                    )
                }
                composable(Routes.BATTLE) {
                    BattleScreen(onFight = { id -> navController.navigate(Routes.fight(id)) })
                }
                composable(Routes.ALLIES) {
                    RivalsScreen(onStartWorkout = { id -> navController.navigate(Routes.session(id)) })
                }
                composable(Routes.SHOP) { ShopScreen() }
                composable(Routes.EDITOR) {
                    WorkoutEditorScreen(onDone = { navController.popBackStack() })
                }
                composable(
                    Routes.EDITOR_EXISTING,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("workoutId") ?: -1L
                    WorkoutEditorScreen(
                        workoutId = id,
                        onDone = { navController.popBackStack() }
                    )
                }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("workoutId") ?: -1L
                    WorkoutDetailScreen(
                        workoutId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { editId -> navController.navigate(Routes.editor(editId)) },
                        onStart = { startId -> navController.navigate(Routes.session(startId)) }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSession = { id -> navController.navigate(Routes.sessionDetail(id)) }
                    )
                }
                composable(Routes.RECORDS) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSession = { id -> navController.navigate(Routes.sessionDetail(id)) },
                        initialTab = 1,
                        title = "Personal Records"
                    )
                }
                composable(
                    Routes.SESSION_DETAIL,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("sessionId") ?: -1L
                    SessionDetailScreen(
                        sessionId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.EXERCISES) {
                    ExerciseLibraryScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.AI) {
                    AiGeneratorScreen(onDone = { navController.popBackStack() })
                }
                composable(
                    Routes.SESSION,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
                ) { entry ->
                    val workoutId = entry.arguments?.getLong("workoutId") ?: -1L
                    ActiveSessionScreen(
                        workoutId = workoutId,
                        onDone = { navController.popBackStack() },
                        onAmbushFight = { monsterId ->
                            navController.navigate(Routes.fight(monsterId, ambush = true))
                        }
                    )
                }
                composable(
                    Routes.FIGHT,
                    arguments = listOf(
                        navArgument("monsterId") { type = NavType.IntType },
                        navArgument("ambush") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entry ->
                    val monsterId = entry.arguments?.getInt("monsterId") ?: 1
                    val ambush = entry.arguments?.getBoolean("ambush") ?: false
                    FightScreen(
                        monsterId = monsterId,
                        ambush = ambush,
                        onDone = { navController.popBackStack() }
                    )
                }
            }

            val activeSessionDetails by container.repository.activeSession.collectAsState(initial = null)

            val updateStatus by container.inAppUpdate.updateStatus.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                val hideBanners = currentRoute == Routes.HERO ||
                    currentRoute == Routes.SESSION ||
                    currentRoute?.startsWith("battle/fight") == true
                
                if (!hideBanners) {
                    if (updateStatus is InAppUpdateStatus.Downloaded) {
                        InAppUpdateBanner(
                            onRestartToUpdate = {
                                container.inAppUpdate.completeUpdate()
                            }
                        )
                    }

                    activeSessionDetails?.let { details ->
                        val imperialPref by container.prefs.imperial.collectAsState()
                        com.fitnessquest.rpg.ui.components.ActiveQuestBanner(
                            title = details.session.title,
                            startedAt = details.session.startedAt,
                            pausedAt = details.session.pausedAt,
                            accumulatedPausedMs = details.session.accumulatedPausedMs,
                            setCount = details.exercises.sumOf { it.sets.size },
                            provisionalXp = details.exercises.sumOf { ex -> ex.sets.sumOf { it.xp } },
                            provisionalVolumeKg = details.exercises.sumOf { ex -> ex.sets.sumOf { it.weightKg * it.reps } },
                            provisionalDistanceKm = details.exercises.sumOf { ex -> ex.sets.sumOf { it.distanceKm } },
                            imperial = imperialPref,
                            onResume = {
                                navController.navigate(Routes.session(details.session.workoutId ?: -1L))
                            },
                            onDiscard = {
                                container.repository.discardActiveSession()
                            }
                        )
                    }
                }
            }

            if (showBottomBar) {
                FloatingGameDock(
                    tabs = tabs.map { tab ->
                        if (tab.route == Routes.HERO) tab.copy(hasBadge = hasClaimableDailyBounty) else tab
                    },
                    currentRoute = selectedDockRoute,
                    dockOnLeft = leftHanded,
                    onTabClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(
                        when {
                            !isLandscape && leftHanded -> Alignment.BottomStart
                            !isLandscape -> Alignment.BottomEnd
                            leftHanded -> Alignment.CenterStart
                            else -> Alignment.CenterEnd
                        }
                    )
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(
                        when {
                            !isLandscape -> Alignment.BottomCenter
                            leftHanded -> Alignment.BottomEnd
                            else -> Alignment.BottomStart
                        }
                    )
                    .navigationBarsPadding()
                    .padding(
                        bottom = if (showBottomBar && !isLandscape) 100.dp else 16.dp,
                        start = if (isLandscape && !leftHanded) 16.dp else 0.dp,
                        end = if (isLandscape && leftHanded) 16.dp else 0.dp
                    )
            )
        }
    }
}
