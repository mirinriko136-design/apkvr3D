package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.SoundEffectsManager
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.AppPreferencesDataStore
import com.example.data.DuelPintarRepository
import com.example.model.QuizMode
import com.example.model.Subject
import com.example.model.ThemeMode
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.notifications.StudyReminderScheduler
import com.example.security.BiometricAuthHelper
import com.example.ui.screens.AnalyticsProfileScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MultiplayerDuelScreen
import com.example.ui.screens.ScheduleCalendarScreen
import com.example.ui.screens.TeacherAdminScreen
import com.example.ui.screens.VrBlocksQuizScreen
import com.example.ui.theme.DuelPintarTheme
import com.example.viewmodel.AppViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_DUEL = "duel"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_SCHEDULE = "schedule"
const val ROUTE_PROFILE = "profile"
const val ROUTE_QUIZ = "quiz"
const val ROUTE_TEACHER = "teacher"

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StudyReminderScheduler.ensureNotificationChannel(this)
        setContent {
            val context = LocalContext.current
            val prefsStore = remember { AppPreferencesDataStore(context.applicationContext) }
            val localPrefs by prefsStore.prefsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.LocalAppPrefs()
            )
            DuelPintarTheme(themeMode = localPrefs.themeMode) {
                AppNavigation(activity = this, prefsStore = prefsStore)
            }
        }
    }
}

@Composable
fun AppNavigation(
    activity: FragmentActivity,
    prefsStore: AppPreferencesDataStore
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(prefsStore = prefsStore, onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedAppRoot(
                activity = activity,
                currentUserId = user.uid,
                prefsStore = prefsStore
            )
        }
    }
}

@Composable
private fun AuthenticatedAppRoot(
    activity: FragmentActivity,
    currentUserId: String,
    prefsStore: AppPreferencesDataStore
) {
    val viewModel: AppViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AppViewModel(
                    repository = DuelPintarRepository(db),
                    prefsStore = prefsStore,
                    soundEffects = SoundEffectsManager(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val themeMode by viewModel.effectiveThemeMode.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val schedulesState by viewModel.studySchedulesState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val openRoomsState by viewModel.openDuelRoomsState.collectAsStateWithLifecycle()
    val activeDuelRoom by viewModel.activeDuelRoomState.collectAsStateWithLifecycle()
    val allQuestions by viewModel.allQuestionsState.collectAsStateWithLifecycle()
    val myQuestionsState by viewModel.myAuthoredQuestionsState.collectAsStateWithLifecycle()
    val subjectAnalytics by viewModel.subjectAnalytics.collectAsStateWithLifecycle()
    val weeklyProductivity by viewModel.weeklyProductivity.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
    val biometricRequired = profile.biometricEnabled || localPrefs.biometricEnabled
    var biometricUnlocked by remember(currentUserId) { mutableStateOf(false) }

    LaunchedEffect(biometricRequired) {
        if (!biometricRequired) {
            biometricUnlocked = true
        }
    }

    DuelPintarTheme(themeMode = themeMode) {
        if (biometricRequired && !biometricUnlocked) {
            BiometricGateScreen(
                activity = activity,
                displayName = profile.displayName,
                onUnlocked = { biometricUnlocked = true },
                onDisableFallback = {
                    viewModel.setBiometricEnabled(false)
                    biometricUnlocked = true
                }
            )
            return@DuelPintarTheme
        }

        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember { CredentialManager.create(context) }

        var pendingQuizSubject by remember { mutableStateOf(Subject.MATEMATIKA) }
        var pendingQuizLocalTwoPlayer by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.toastEvents.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        val tabs = remember {
            listOf(
                BottomTab(ROUTE_HOME, "Beranda", Icons.Rounded.Home),
                BottomTab(ROUTE_DUEL, "Duel", Icons.Rounded.Groups),
                BottomTab(ROUTE_LEADERBOARD, "Juara", Icons.Rounded.EmojiEvents),
                BottomTab(ROUTE_SCHEDULE, "Jadwal", Icons.Rounded.CalendarMonth),
                BottomTab(ROUTE_PROFILE, "Profil", Icons.Rounded.Analytics)
            )
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
        val showPrimaryNav = currentRoute in tabs.map { it.route }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    if (showPrimaryNav && !useRail) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showPrimaryNav && useRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(16.dp))
                            tabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        composable(ROUTE_HOME) {
                            if (profileState is UiState.Loading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                HomeDashboardScreen(
                                    profile = profile,
                                    weeklyProductivity = weeklyProductivity,
                                    schedules = (schedulesState as? UiState.Success)?.data ?: emptyList(),
                                    leaderboard = (leaderboardState as? UiState.Success)?.data ?: emptyList(),
                                    themeMode = themeMode,
                                    quizDurationSeconds = localPrefs.quizDurationSeconds,
                                    vrEnvironmentMode = localPrefs.vrEnvironmentMode,
                                    onToggleTheme = viewModel::setThemeMode,
                                    onUpdateQuizSettings = viewModel::setQuizSettings,
                                    onSaveDashboardWidgets = viewModel::saveDashboardWidgets,
                                    onLaunchVrQuiz = { subj, local2p ->
                                        pendingQuizSubject = subj
                                        pendingQuizLocalTwoPlayer = local2p
                                        navController.navigate(ROUTE_QUIZ)
                                    },
                                    onOpenMultiplayer = { navController.navigate(ROUTE_DUEL) },
                                    onOpenLeaderboard = { navController.navigate(ROUTE_LEADERBOARD) },
                                    onOpenSchedules = { navController.navigate(ROUTE_SCHEDULE) },
                                    onOpenTeacherAdmin = { navController.navigate(ROUTE_TEACHER) }
                                )
                            }
                        }

                        composable(ROUTE_DUEL) {
                            MultiplayerDuelScreen(
                                currentUserId = currentUserId,
                                profile = profile,
                                openRoomsState = openRoomsState,
                                activeRoom = activeDuelRoom,
                                allQuestions = allQuestions,
                                soundEffects = viewModel.soundEffects,
                                onCreateRoom = viewModel::createOnlineDuelRoom,
                                onJoinRoom = viewModel::joinOnlineDuelRoom,
                                onUpdateDuelProgress = viewModel::updateActiveDuelProgress,
                                onLeaveRoom = viewModel::leaveActiveDuelRoom,
                                onRecordDuelResult = { subj, score, correct, total, dur, won ->
                                    viewModel.recordQuizResult(
                                        subject = subj,
                                        mode = QuizMode.MULTIPLAYER_ONLINE,
                                        score = score,
                                        correctCount = correct,
                                        totalQuestions = total,
                                        durationSeconds = dur,
                                        isDuelWin = won
                                    )
                                },
                                onLaunchLocalTwoPlayer = { subj ->
                                    pendingQuizSubject = subj
                                    pendingQuizLocalTwoPlayer = true
                                    navController.navigate(ROUTE_QUIZ)
                                }
                            )
                        }

                        composable(ROUTE_LEADERBOARD) {
                            LeaderboardScreen(
                                currentUserId = currentUserId,
                                leaderboardState = leaderboardState
                            )
                        }

                        composable(ROUTE_SCHEDULE) {
                            ScheduleCalendarScreen(
                                profile = profile,
                                schedulesState = schedulesState,
                                onSetDailyReminder = viewModel::setDailyReminder,
                                onAddSchedule = viewModel::addStudySchedule,
                                onToggleCompleted = viewModel::toggleScheduleCompleted,
                                onMarkCalendarSynced = viewModel::markScheduleCalendarSynced,
                                onDeleteSchedule = viewModel::deleteStudySchedule
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            AnalyticsProfileScreen(
                                profile = profile,
                                subjectAnalytics = subjectAnalytics,
                                weeklyProductivity = weeklyProductivity,
                                themeMode = themeMode,
                                soundEnabled = profile.soundEnabled,
                                onUpdateProfile = { name, role, grade, school ->
                                    viewModel.updateProfile(name, role, grade, school)
                                },
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetSoundEnabled = viewModel::setSoundEnabled,
                                onSetBiometricEnabled = viewModel::setBiometricEnabled,
                                onBuildDriveBackupJson = viewModel::buildDriveBackupJson,
                                onDriveBackupExported = viewModel::onDriveBackupExported,
                                onRestoreFromDriveBackupJson = viewModel::restoreFromDriveBackupJson,
                                onOpenTeacherAdmin = { navController.navigate(ROUTE_TEACHER) },
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }

                        composable(ROUTE_QUIZ) {
                            val quizQuestions = remember(pendingQuizSubject, profile.gradeLevel, allQuestions) {
                                viewModel.questionsForSubject(pendingQuizSubject, profile.gradeLevel, count = 5)
                            }
                            VrBlocksQuizScreen(
                                subject = pendingQuizSubject,
                                questions = quizQuestions,
                                isLocalTwoPlayer = pendingQuizLocalTwoPlayer,
                                soundEnabled = profile.soundEnabled,
                                quizDurationSeconds = localPrefs.quizDurationSeconds,
                                initialEnvironmentMode = localPrefs.vrEnvironmentMode,
                                soundEffects = viewModel.soundEffects,
                                onToggleSound = viewModel::setSoundEnabled,
                                onEnvironmentChange = { env ->
                                    viewModel.setQuizSettings(localPrefs.quizDurationSeconds, env)
                                },
                                onSessionFinished = { score, correct, total, dur, won ->
                                    viewModel.recordQuizResult(
                                        subject = pendingQuizSubject,
                                        mode = if (pendingQuizLocalTwoPlayer) QuizMode.MULTIPLAYER_LOCAL else QuizMode.SOLO_VR,
                                        score = score,
                                        correctCount = correct,
                                        totalQuestions = total,
                                        durationSeconds = dur,
                                        isDuelWin = won
                                    )
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_TEACHER) {
                            TeacherAdminScreen(
                                myQuestionsState = myQuestionsState,
                                allQuestions = allQuestions,
                                onSaveQuestion = viewModel::saveTeacherQuestion,
                                onDeleteQuestion = viewModel::deleteTeacherQuestion,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricGateScreen(
    activity: FragmentActivity,
    displayName: String,
    onUnlocked: () -> Unit,
    onDisableFallback: () -> Unit
) {
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (BiometricAuthHelper.isBiometricAvailable(activity)) {
            BiometricAuthHelper.promptBiometric(
                activity = activity,
                onSuccess = onUnlocked,
                onError = { err -> errorMsg = err }
            )
        } else {
            onDisableFallback()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Fingerprint,
                        contentDescription = "Kunci Biometrik",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Akun Terkunci Aman", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Halo $displayName, verifikasi sidik jari atau kunci perangkat untuk masuk ke DuelPintar3D VR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    BiometricAuthHelper.promptBiometric(
                        activity = activity,
                        onSuccess = onUnlocked,
                        onError = { err -> errorMsg = err }
                    )
                },
                modifier = Modifier.testTag("unlock_biometric_button")
            ) {
                Text("Buka dengan Biometrik")
            }
        }
    }
}
