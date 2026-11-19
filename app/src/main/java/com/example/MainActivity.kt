package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.findActivity
import com.example.auth.signOutUser
import com.example.core.FirestoreClientProvider
import com.example.core.LocalRpgAudioEngine
import com.example.core.RpgAudioEngine
import com.example.core.configureAppCheck
import com.example.core.describeActiveSoundscape
import com.example.core.diagnoseAppCheckFailure
import com.example.core.getLocalAppCheckDebugToken
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.PlayerFactsRepository
import com.example.data.repository.ThemePreferencesRepository
import com.example.data.repository.UserAppSettings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.GameSessionScreen
import com.example.ui.screens.PresentationScreen
import com.example.ui.screens.SettingsBottomSheet
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.AppVisualLocation
import com.example.ui.theme.ChroniclesTheme
import com.example.ui.theme.DynamicThemeContext
import com.example.ui.viewmodel.CampaignsViewModel
import com.example.ui.viewmodel.GameSessionViewModel
import com.example.ui.viewmodel.SetupWizardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val ROUTE_CAMPAIGNS = "campaigns"
private const val ROUTE_SETUP = "setup?templateId={templateId}"
private const val ROUTE_GAME = "game/{campaignId}"
private const val ROUTE_PRESENTATION = "presentation"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val themeRepo = remember(context) {
                ThemePreferencesRepository(context.applicationContext)
            }
            val appSettings by themeRepo.settings.collectAsStateWithLifecycle(
                initialValue = UserAppSettings()
            )
            val coroutineScope = rememberCoroutineScope()
            var dynamicContext by remember {
                mutableStateOf(DynamicThemeContext(location = AppVisualLocation.AUTH))
            }
            val audioEngine = remember { RpgAudioEngine() }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner, audioEngine) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> audioEngine.setAppForeground(true)
                        Lifecycle.Event.ON_STOP -> audioEngine.setAppForeground(false)
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    audioEngine.release()
                }
            }

            LaunchedEffect(appSettings.soundEnabled, appSettings.ostVolume, appSettings.sfxVolume) {
                audioEngine.updateSettings(
                    enabled = appSettings.soundEnabled,
                    ostVol = appSettings.ostVolume,
                    sfxVol = appSettings.sfxVolume
                )
            }

            LaunchedEffect(dynamicContext) {
                audioEngine.updateContext(dynamicContext)
            }

            val cycleTheme: () -> Unit = {
                val modes = PaperThemeMode.entries
                val nextIndex = (modes.indexOf(appSettings.themeMode) + 1) % modes.size
                val next = modes[nextIndex]
                coroutineScope.launch { themeRepo.setThemeMode(next) }
            }

            val toggleSound: () -> Unit = {
                val next = !appSettings.soundEnabled
                coroutineScope.launch { themeRepo.setSoundEnabled(next) }
            }

            CompositionLocalProvider(LocalRpgAudioEngine provides audioEngine) {
                ChroniclesTheme(
                    themeMode = appSettings.themeMode,
                    dynamicContext = dynamicContext,
                    fontStyle = appSettings.fontStyle,
                    fontScale = appSettings.fontScale
                ) {
                    AppNavigation(
                        appSettings = appSettings,
                        dynamicContext = dynamicContext,
                        themeRepo = themeRepo,
                        onDynamicContextChange = { dynamicContext = it },
                        onCycleTheme = cycleTheme,
                        onToggleSound = toggleSound
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    appSettings: UserAppSettings,
    dynamicContext: DynamicThemeContext,
    themeRepo: ThemePreferencesRepository,
    onDynamicContextChange: (DynamicThemeContext) -> Unit,
    onCycleTheme: () -> Unit,
    onToggleSound: () -> Unit
) {
    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val auth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
    var autoSignInJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            autoSignInJob = attemptAutoSignIn(
                context = activityContext,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        LaunchedEffect(Unit) {
            onDynamicContextChange(DynamicThemeContext(location = AppVisualLocation.AUTH))
        }
        AuthScreen(
            themeMode = appSettings.themeMode,
            onCycleTheme = onCycleTheme,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onBeforeInteractiveSignIn = {
                autoSignInJob?.cancel()
                autoSignInJob = null
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedNavHost(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email.orEmpty(),
                appSettings = appSettings,
                dynamicContext = dynamicContext,
                themeRepo = themeRepo,
                onDynamicContextChange = onDynamicContextChange,
                onCycleTheme = onCycleTheme,
                onToggleSound = onToggleSound,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = coroutineScope
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedNavHost(
    currentUserId: String,
    userDisplayName: String,
    appSettings: UserAppSettings,
    dynamicContext: DynamicThemeContext,
    themeRepo: ThemePreferencesRepository,
    onDynamicContextChange: (DynamicThemeContext) -> Unit,
    onCycleTheme: () -> Unit,
    onToggleSound: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    var showSettingsSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            ROUTE_CAMPAIGNS -> onDynamicContextChange(
                DynamicThemeContext(location = AppVisualLocation.SHELF)
            )
            ROUTE_SETUP -> onDynamicContextChange(
                DynamicThemeContext(location = AppVisualLocation.WIZARD)
            )
            ROUTE_PRESENTATION -> onDynamicContextChange(
                DynamicThemeContext(location = AppVisualLocation.PRESENTATION)
            )
        }
    }

    val aiRepository = remember(appContext) {
        GameMasterAiRepository(
            debugTokenProvider = { getLocalAppCheckDebugToken(appContext) },
            appCheckDiagnosticProvider = { diagnoseAppCheckFailure(appContext) }
        )
    }
    val factsRepository = remember(appContext) {
        PlayerFactsRepository(appContext)
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_CAMPAIGNS
    ) {
        composable(ROUTE_CAMPAIGNS) {
            val campaignsVm: CampaignsViewModel = viewModel(
                key = "campaigns_$currentUserId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirestoreClientProvider.getDatabase(app)
                        CampaignsViewModel(
                            repository = CampaignRepository(db, Firebase.auth),
                            currentUserId = currentUserId
                        )
                    }
                }
            )
            CampaignsScreen(
                viewModel = campaignsVm,
                userDisplayName = userDisplayName,
                themeMode = appSettings.themeMode,
                hapticsEnabled = appSettings.hapticsEnabled,
                soundEnabled = appSettings.soundEnabled,
                onToggleSound = onToggleSound,
                onCycleTheme = onCycleTheme,
                onOpenSettings = { showSettingsSheet = true },
                onOpenPresentation = {
                    navController.navigate(ROUTE_PRESENTATION) {
                        launchSingleTop = true
                    }
                },
                onSignOut = onSignOut,
                onCreateNewCampaign = { templateId ->
                    val route = if (templateId.isNullOrBlank()) {
                        "setup"
                    } else {
                        "setup?templateId=$templateId"
                    }
                    navController.navigate(route)
                },
                onOpenCampaign = { campaignId ->
                    navController.navigate("game/$campaignId")
                }
            )
        }

        composable(
            route = ROUTE_SETUP,
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            val setupVm: SetupWizardViewModel = viewModel(
                key = "setup_$currentUserId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirestoreClientProvider.getDatabase(app)
                        SetupWizardViewModel(
                            campaignRepository = CampaignRepository(db, Firebase.auth),
                            aiRepository = aiRepository,
                            themePreferencesRepository = themeRepo
                        )
                    }
                }
            )
            SetupWizardScreen(
                viewModel = setupVm,
                initialTemplateId = templateId,
                onBack = { navController.popBackStack() },
                onCampaignCreated = { campaignId ->
                    navController.navigate("game/$campaignId") {
                        popUpTo(ROUTE_CAMPAIGNS)
                    }
                }
            )
        }

        composable(
            route = ROUTE_GAME,
            arguments = listOf(
                navArgument("campaignId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId").orEmpty()
            val sessionVm: GameSessionViewModel = viewModel(
                key = "game_${currentUserId}_$campaignId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirestoreClientProvider.getDatabase(app)
                        GameSessionViewModel(
                            campaignRepository = CampaignRepository(db, Firebase.auth),
                            aiRepository = aiRepository,
                            factsRepository = factsRepository,
                            themePreferencesRepository = themeRepo,
                            currentUserId = currentUserId,
                            campaignId = campaignId
                        )
                    }
                }
            )
            GameSessionScreen(
                viewModel = sessionVm,
                hapticsEnabled = appSettings.hapticsEnabled,
                soundEnabled = appSettings.soundEnabled,
                autoReadTtsEnabled = appSettings.autoReadTtsEnabled,
                onToggleSound = onToggleSound,
                onDynamicContextChange = onDynamicContextChange,
                onOpenSettings = { showSettingsSheet = true },
                onOpenForkedCampaign = { forkedId ->
                    navController.navigate("game/$forkedId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_PRESENTATION) {
            PresentationScreen(
                hapticsEnabled = appSettings.hapticsEnabled,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = appSettings,
            onSelectTheme = { mode ->
                coroutineScope.launch { themeRepo.setThemeMode(mode) }
            },
            onSelectFontStyle = { style ->
                coroutineScope.launch { themeRepo.setFontStyle(style) }
            },
            onSelectFontScale = { scale ->
                coroutineScope.launch { themeRepo.setFontScale(scale) }
            },
            onSelectAiModel = { model ->
                coroutineScope.launch { themeRepo.setAiModel(model) }
            },
            onSelectNarrativePace = { pace ->
                coroutineScope.launch { themeRepo.setNarrativePace(pace) }
            },
            onToggleLiveStreaming = { enabled ->
                coroutineScope.launch { themeRepo.setLiveStreamingEnabled(enabled) }
            },
            onToggleAutoReadTts = { enabled ->
                coroutineScope.launch { themeRepo.setAutoReadTtsEnabled(enabled) }
            },
            onToggleHaptics = { enabled ->
                coroutineScope.launch { themeRepo.setHapticsEnabled(enabled) }
            },
            onToggleSound = { enabled ->
                coroutineScope.launch { themeRepo.setSoundEnabled(enabled) }
            },
            onOstVolumeChange = { vol ->
                coroutineScope.launch { themeRepo.setOstVolume(vol) }
            },
            onSfxVolumeChange = { vol ->
                coroutineScope.launch { themeRepo.setSfxVolume(vol) }
            },
            activeSoundscapeLabel = describeActiveSoundscape(dynamicContext),
            onOpenPresentation = {
                showSettingsSheet = false
                navController.navigate(ROUTE_PRESENTATION) {
                    launchSingleTop = true
                }
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
