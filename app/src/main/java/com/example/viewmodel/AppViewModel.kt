package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsManager
import com.example.data.AppPreferencesDataStore
import com.example.data.DuelPintarRepository
import com.example.data.LocalAppPrefs
import com.example.model.DailyProductivity
import com.example.model.Difficulty
import com.example.model.DuelRoom
import com.example.model.LeaderboardEntry
import com.example.model.QuizMode
import com.example.model.QuizQuestion
import com.example.model.QuizSession
import com.example.model.StarterQuestions
import com.example.model.StudySchedule
import com.example.model.Subject
import com.example.model.SubjectAnalytics
import com.example.model.ThemeMode
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.VrEnvironmentMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppViewModel(
    private val repository: DuelPintarRepository,
    private val prefsStore: AppPreferencesDataStore,
    val soundEffects: SoundEffectsManager,
    val currentUserId: String
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _activeDuelRoomId = MutableStateFlow<String?>(null)
    val activeDuelRoomId: StateFlow<String?> = _activeDuelRoomId.asStateFlow()

    val localPrefs: StateFlow<LocalAppPrefs> = prefsStore.prefsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LocalAppPrefs()
    )

    val userProfileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            val resolved = profile ?: UserProfile(userId = currentUserId)
            soundEffects.enabled = resolved.soundEnabled
            UiState.Success(resolved)
        }
        .unwrapFirestoreErrors("profil pengguna")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val quizSessionsState: StateFlow<UiState<List<QuizSession>>> = repository.observeQuizSessions(currentUserId)
        .map<List<QuizSession>, UiState<List<QuizSession>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("riwayat kuis")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val studySchedulesState: StateFlow<UiState<List<StudySchedule>>> = repository.observeStudySchedules(currentUserId)
        .map<List<StudySchedule>, UiState<List<StudySchedule>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("jadwal belajar")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard(currentUserId)
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("papan peringkat")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val allQuestionsState: StateFlow<List<QuizQuestion>> = repository.observePublishedQuestions(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing published questions", firestoreError ?: error)
            emit(emptyList())
        }
        .map { remote ->
            val combined = remote + StarterQuestions.bundled.filter { starter ->
                remote.none { it.prompt.equals(starter.prompt, ignoreCase = true) }
            }
            combined
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StarterQuestions.bundled
        )

    val myAuthoredQuestionsState: StateFlow<UiState<List<QuizQuestion>>> = repository.observeMyAuthoredQuestions(currentUserId)
        .map<List<QuizQuestion>, UiState<List<QuizQuestion>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("soal buatan saya")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val openDuelRoomsState: StateFlow<UiState<List<DuelRoom>>> = repository.observeOpenDuelRooms(currentUserId)
        .map<List<DuelRoom>, UiState<List<DuelRoom>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("ruang duel")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDuelRoomState: StateFlow<DuelRoom?> = _activeDuelRoomId
        .flatMapLatest { roomId ->
            if (roomId.isNullOrBlank()) flowOf(null)
            else repository.observeDuelRoom(roomId).catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing active duel room", firestoreError ?: error)
                emit(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val subjectAnalytics: StateFlow<List<SubjectAnalytics>> = quizSessionsState
        .map { state ->
            val sessions = (state as? UiState.Success)?.data ?: emptyList()
            Subject.entries.map { subject ->
                val list = sessions.filter { it.subject == subject.code }
                val attempts = list.size
                val totalQ = list.sumOf { it.totalQuestions }
                val totalCorrect = list.sumOf { it.correctCount }
                val accuracy = if (totalQ > 0) (totalCorrect * 100) / totalQ else 0
                val avgDur = if (attempts > 0) list.sumOf { it.durationSeconds } / attempts else 0
                val xp = list.sumOf { it.xpEarned }
                SubjectAnalytics(
                    subject = subject,
                    attempts = attempts,
                    accuracyPct = accuracy,
                    avgDurationSec = avgDur,
                    totalXp = xp
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = Subject.entries.map { SubjectAnalytics(it, 0, 0, 0, 0) }
        )

    val weeklyProductivity: StateFlow<List<DailyProductivity>> = quizSessionsState
        .map { state ->
            val sessions = (state as? UiState.Success)?.data ?: emptyList()
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val labelFmt = SimpleDateFormat("EEE", Locale("id", "ID"))
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            (0..6).map { offset ->
                val dayDate = cal.time
                val key = keyFmt.format(dayDate)
                val label = labelFmt.format(dayDate).take(3)
                val daySessions = sessions.filter { it.dayKey == key }
                val mins = (daySessions.sumOf { it.durationSeconds } / 60).coerceAtLeast(
                    if (daySessions.isNotEmpty()) 1 else 0
                )
                val qCount = daySessions.sumOf { it.totalQuestions }
                val xp = daySessions.sumOf { it.xpEarned }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                DailyProductivity(
                    dayKey = key,
                    label = label,
                    minutesStudied = mins,
                    questionsAnswered = qCount,
                    xpEarned = xp
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val effectiveThemeMode: StateFlow<ThemeMode> = combine(localPrefs, userProfileState) { prefs, profileState ->
        val profile = (profileState as? UiState.Success)?.data
        if (profile != null) ThemeMode.fromCode(profile.themeMode) else prefs.themeMode
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ThemeMode.SYSTEM
    )

    init {
        viewModelScope.launch {
            val chosenRole = prefsStore.prefsFlow.first().pendingLoginRole
            repository.ensureUserProfile(preferredRole = chosenRole).onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal memuat profil cloud")
            }
        }
    }

    fun setQuizSettings(durationSeconds: Int, environmentMode: VrEnvironmentMode) {
        viewModelScope.launch {
            prefsStore.setQuizSettings(durationSeconds, environmentMode)
        }
    }

    fun questionsForSubject(subject: Subject, gradeFilter: Int? = null, count: Int = 5): List<QuizQuestion> {
        val pool = allQuestionsState.value.filter { it.subject == subject.code }
        val gradeMatched = if (gradeFilter != null) {
            pool.filter { it.gradeLevel == gradeFilter }
        } else {
            emptyList()
        }
        val finalPool = (if (gradeMatched.size >= count) gradeMatched else pool).ifEmpty {
            StarterQuestions.bundled.filter { it.subject == subject.code }
        }
        return finalPool.shuffled().take(count)
    }

    fun updateProfile(
        displayName: String? = null,
        role: String? = null,
        gradeLevel: Int? = null,
        schoolName: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfilePreferences(
                displayName = displayName,
                role = role,
                gradeLevel = gradeLevel,
                schoolName = schoolName
            ).onSuccess {
                _toastEvents.tryEmit("Profil berhasil diperbarui dan disinkronkan.")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal memperbarui profil")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsStore.setThemeMode(mode)
            repository.updateProfilePreferences(themeMode = mode)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEffects.enabled = enabled
        viewModelScope.launch {
            prefsStore.setSoundEnabled(enabled)
            repository.updateProfilePreferences(soundEnabled = enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setBiometricEnabled(enabled)
            repository.updateProfilePreferences(biometricEnabled = enabled).onSuccess {
                _toastEvents.tryEmit(
                    if (enabled) "Kunci Biometrik diaktifkan untuk keamanan akun."
                    else "Kunci Biometrik dinonaktifkan."
                )
            }
        }
    }

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsStore.setDailyReminder(enabled, hour, minute)
            repository.updateProfilePreferences(
                dailyReminderEnabled = enabled,
                dailyReminderHour = hour,
                dailyReminderMinute = minute
            )
        }
    }

    fun saveDashboardWidgets(widgets: List<String>) {
        viewModelScope.launch {
            repository.updateProfilePreferences(dashboardWidgets = widgets).onSuccess {
                _toastEvents.tryEmit("Tampilan dasbor diperbarui!")
            }
        }
    }

    fun recordQuizResult(
        subject: Subject,
        mode: QuizMode,
        score: Int,
        correctCount: Int,
        totalQuestions: Int,
        durationSeconds: Int,
        isDuelWin: Boolean = false
    ) {
        val currentProfile = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        viewModelScope.launch {
            repository.recordQuizCompletion(
                currentProfile = currentProfile,
                subject = subject,
                mode = mode,
                score = score,
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                durationSeconds = durationSeconds,
                isDuelWin = isDuelWin
            ).onSuccess { xp ->
                _toastEvents.tryEmit("+$xp XP tercatat ke cloud!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal menyimpan progres kuis")
            }
        }
    }

    fun saveTeacherQuestion(
        questionId: String?,
        subject: Subject,
        gradeLevel: Int,
        difficulty: Difficulty,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        published: Boolean
    ) {
        val authorName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: "Guru SD"
        viewModelScope.launch {
            repository.saveQuestion(
                questionId = questionId,
                authorName = authorName,
                subject = subject,
                gradeLevel = gradeLevel,
                difficulty = difficulty,
                prompt = prompt,
                options = options,
                correctIndex = correctIndex,
                explanation = explanation,
                published = published
            ).onSuccess {
                _toastEvents.tryEmit("Soal berhasil disimpan ke Bank Soal Cloud!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal menyimpan soal")
            }
        }
    }

    fun deleteTeacherQuestion(questionId: String) {
        viewModelScope.launch {
            repository.deleteQuestion(questionId).onSuccess {
                _toastEvents.tryEmit("Soal dihapus.")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal menghapus soal")
            }
        }
    }

    fun addStudySchedule(
        title: String,
        subjectCode: String,
        dueAtMillis: Long,
        reminderMinutesBefore: Int,
        notes: String,
        onScheduled: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.addStudySchedule(
                title = title,
                subjectCode = subjectCode,
                dueAtMillis = dueAtMillis,
                reminderMinutesBefore = reminderMinutesBefore,
                calendarSynced = false,
                notes = notes
            ).onSuccess { id ->
                _toastEvents.tryEmit("Jadwal belajar & pengingat tugas disimpan!")
                onScheduled(id)
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal menambah jadwal")
            }
        }
    }

    fun toggleScheduleCompleted(scheduleId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleScheduleCompleted(scheduleId, completed)
        }
    }

    fun markScheduleCalendarSynced(scheduleId: String) {
        viewModelScope.launch {
            repository.markScheduleCalendarSynced(scheduleId)
        }
    }

    fun deleteStudySchedule(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteStudySchedule(scheduleId)
        }
    }

    fun createOnlineDuelRoom(subject: Subject, gradeLevel: Int) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val seeds = questionsForSubject(subject, gradeLevel, count = 5).map { it.id }
        viewModelScope.launch {
            repository.createDuelRoom(
                hostName = profile.displayName,
                subject = subject,
                gradeLevel = gradeLevel,
                questionSeeds = seeds
            ).onSuccess { room ->
                _activeDuelRoomId.value = room.id
                _toastEvents.tryEmit("Ruang duel ${room.roomCode} dibuat!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal membuat ruang duel")
            }
        }
    }

    fun joinOnlineDuelRoom(roomCode: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        viewModelScope.launch {
            repository.joinDuelRoomByCode(roomCode, profile.displayName).onSuccess { room ->
                _activeDuelRoomId.value = room.id
                _toastEvents.tryEmit("Bergabung ke duel ${room.roomCode}!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Gagal bergabung ke ruang duel")
            }
        }
    }

    fun updateActiveDuelProgress(room: DuelRoom, score: Int, progress: Int, finished: Boolean) {
        viewModelScope.launch {
            repository.updateDuelProgress(room, score, progress, finished)
        }
    }

    fun leaveActiveDuelRoom() {
        _activeDuelRoomId.value = null
    }

    fun buildDriveBackupJson(): String {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: UserProfile(userId = currentUserId)
        val sessions = (quizSessionsState.value as? UiState.Success)?.data ?: emptyList()
        val schedules = (studySchedulesState.value as? UiState.Success)?.data ?: emptyList()
        return repository.buildDriveBackupJson(profile, sessions, schedules)
    }

    fun onDriveBackupExported() {
        viewModelScope.launch {
            repository.markDriveSyncTimestamp()
            _toastEvents.tryEmit("Progres belajar berhasil dicadangkan ke Google Drive!")
        }
    }

    fun restoreFromDriveBackupJson(jsonText: String) {
        viewModelScope.launch {
            repository.restoreFromDriveBackupJson(jsonText)
                .onSuccess { msg -> _toastEvents.tryEmit(msg) }
                .onFailure { err -> _toastEvents.tryEmit(err.localizedMessage ?: "Gagal memulihkan cadangan") }
        }
    }

    private fun <T> Flow<UiState<T>>.unwrapFirestoreErrors(label: String): Flow<UiState<T>> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $label", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat $label"))
        }

    private companion object {
        const val TAG = "AppViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
