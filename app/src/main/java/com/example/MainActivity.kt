package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.findActivity
import com.example.auth.signOutUser
import com.example.core.configureAppCheck
import com.example.core.getLocalAppCheckDebugToken
import com.example.data.repository.CampaignRepository
import com.example.data.repository.GameMasterAiRepository
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.ThemePreferencesRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.GameSessionScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.ChroniclesTheme
import com.example.ui.viewmodel.CampaignsViewModel
import com.example.ui.viewmodel.GameSessionViewModel
import com.example.ui.viewmodel.SetupWizardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val ROUTE_CAMPAIGNS = "campaigns"
private const val ROUTE_SETUP = "setup?templateId={templateId}"
private const val ROUTE_GAME = "game/{campaignId}"

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
            val themeMode by themeRepo.themeMode.collectAsStateWithLifecycle(
                initialValue = PaperThemeMode.SYSTEM
            )
            val coroutineScope = rememberCoroutineScope()

            val isDark = when (themeMode) {
                PaperThemeMode.LIGHT -> false
                PaperThemeMode.DARK -> true
                PaperThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val cycleTheme: () -> Unit = {
                val next = when (themeMode) {
                    PaperThemeMode.SYSTEM -> PaperThemeMode.LIGHT
                    PaperThemeMode.LIGHT -> PaperThemeMode.DARK
                    PaperThemeMode.DARK -> PaperThemeMode.SYSTEM
                }
                coroutineScope.launch { themeRepo.setThemeMode(next) }
            }

            ChroniclesTheme(darkTheme = isDark) {
                AppNavigation(
                    themeMode = themeMode,
                    onCycleTheme = cycleTheme
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    themeMode: PaperThemeMode,
    onCycleTheme: () -> Unit
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
        AuthScreen(
            themeMode = themeMode,
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
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
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
    themeMode: PaperThemeMode,
    onCycleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val aiRepository = remember(appContext) {
        GameMasterAiRepository(
            debugTokenProvider = { getLocalAppCheckDebugToken(appContext) }
        )
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
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
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
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
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
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        SetupWizardViewModel(
                            campaignRepository = CampaignRepository(db, Firebase.auth),
                            aiRepository = aiRepository
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
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        GameSessionViewModel(
                            campaignRepository = CampaignRepository(db, Firebase.auth),
                            aiRepository = aiRepository,
                            currentUserId = currentUserId,
                            campaignId = campaignId
                        )
                    }
                }
            )
            GameSessionScreen(
                viewModel = sessionVm,
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
