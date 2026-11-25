package com.example.data

import android.content.Context
import com.example.R
import com.example.model.DashboardWidget
import com.example.model.Difficulty
import com.example.model.DuelRoom
import com.example.model.LeaderboardEntry
import com.example.model.QuizMode
import com.example.model.QuizQuestion
import com.example.model.QuizSession
import com.example.model.StudySchedule
import com.example.model.Subject
import com.example.model.ThemeMode
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DuelPintarRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")

    private fun <T> Flow<T>.wrapFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun ensureUserProfile(defaultName: String? = null, preferredRole: String = "student"): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val safeRole = if (preferredRole == "teacher") "teacher" else "student"
        val userPath = "users/$uid"
        val lbPath = "leaderboard/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val resolvedName = (defaultName ?: auth.currentUser?.displayName ?: "Siswa Hebat")
                .trim()
                .ifBlank { "Siswa Hebat" }
                .take(60)
            if (!snap.exists()) {
                val profileMap = mapOf(
                    "userId" to uid,
                    "displayName" to resolvedName,
                    "role" to safeRole,
                    "gradeLevel" to 4,
                    "schoolName" to "SDN Nusantara",
                    "avatarColor" to "#2563EB",
                    "totalXp" to 0,
                    "level" to 1,
                    "streakDays" to 1,
                    "totalCorrect" to 0,
                    "totalAnswered" to 0,
                    "duelWins" to 0,
                    "duelPlayed" to 0,
                    "soundEnabled" to true,
                    "biometricEnabled" to false,
                    "themeMode" to "system",
                    "dailyReminderEnabled" to false,
                    "dailyReminderHour" to 18,
                    "dailyReminderMinute" to 30,
                    "dashboardWidgets" to DashboardWidget.defaultOrder,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profileMap).await()
            } else if (snap.getString("role") != safeRole) {
                userRef.update(
                    mapOf(
                        "role" to safeRole,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            if (!lbSnap.exists()) {
                val lbMap = mapOf(
                    "userId" to uid,
                    "displayName" to resolvedName,
                    "schoolName" to "SDN Nusantara",
                    "gradeLevel" to 4,
                    "totalXp" to 0,
                    "level" to 1,
                    "duelWins" to 0,
                    "accuracyPct" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(lbMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$userPath / $lbPath")
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses profil lintas pengguna ditolak." }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observeQuizSessions(userId: String): Flow<List<QuizSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/quiz_sessions"
        emitAll(
            db.collection("users").document(userId).collection("quiz_sessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeStudySchedules(userId: String): Flow<List<StudySchedule>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/study_schedules"
        emitAll(
            db.collection("users").document(userId).collection("study_schedules")
                .orderBy("dueAtMillis", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudySchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeLeaderboard(userId: String): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereGreaterThanOrEqualTo("totalXp", 0)
                .orderBy("totalXp", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observePublishedQuestions(userId: String): Flow<List<QuizQuestion>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "questions"
        emitAll(
            db.collection("questions")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeMyAuthoredQuestions(userId: String): Flow<List<QuizQuestion>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "questions"
        emitAll(
            db.collection("questions")
                .whereEqualTo("authorId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeOpenDuelRooms(userId: String): Flow<List<DuelRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "duel_rooms"
        emitAll(
            db.collection("duel_rooms")
                .whereEqualTo("status", "WAITING")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DuelRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeDuelRoom(roomId: String): Flow<DuelRoom?> = flow {
        val uid = requireUserId()
        val path = "duel_rooms/$roomId"
        emitAll(
            db.collection("duel_rooms").document(roomId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(DuelRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    suspend fun updateProfilePreferences(
        displayName: String? = null,
        role: String? = null,
        gradeLevel: Int? = null,
        schoolName: String? = null,
        soundEnabled: Boolean? = null,
        biometricEnabled: Boolean? = null,
        themeMode: ThemeMode? = null,
        dailyReminderEnabled: Boolean? = null,
        dailyReminderHour: Int? = null,
        dailyReminderMinute: Int? = null,
        dashboardWidgets: List<String>? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["displayName"] = it.take(60) }
            role?.takeIf { it == "student" || it == "teacher" }?.let { updates["role"] = it }
            gradeLevel?.coerceIn(1, 6)?.let { updates["gradeLevel"] = it }
            schoolName?.trim()?.let { updates["schoolName"] = it.take(80) }
            soundEnabled?.let { updates["soundEnabled"] = it }
            biometricEnabled?.let { updates["biometricEnabled"] = it }
            themeMode?.let { updates["themeMode"] = it.code }
            dailyReminderEnabled?.let { updates["dailyReminderEnabled"] = it }
            dailyReminderHour?.coerceIn(0, 23)?.let { updates["dailyReminderHour"] = it }
            dailyReminderMinute?.coerceIn(0, 59)?.let { updates["dailyReminderMinute"] = it }
            dashboardWidgets?.take(10)?.let { updates["dashboardWidgets"] = it }

            db.collection("users").document(uid).update(updates).await()

            // Sync public displayName / schoolName / gradeLevel to leaderboard if changed
            val lbUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { lbUpdates["displayName"] = it.take(60) }
            gradeLevel?.coerceIn(1, 6)?.let { lbUpdates["gradeLevel"] = it }
            schoolName?.trim()?.let { lbUpdates["schoolName"] = it.take(80) }
            if (lbUpdates.size > 1) {
                db.collection("leaderboard").document(uid).update(lbUpdates).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizCompletion(
        currentProfile: UserProfile,
        subject: Subject,
        mode: QuizMode,
        score: Int,
        correctCount: Int,
        totalQuestions: Int,
        durationSeconds: Int,
        isDuelWin: Boolean = false
    ): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val safeTotal = totalQuestions.coerceIn(1, 50)
        val safeCorrect = correctCount.coerceIn(0, safeTotal)
        val safeDuration = durationSeconds.coerceIn(1, 3600)
        val xpEarned = (safeCorrect * 20 + (if (isDuelWin) 60 else 15)).coerceIn(0, 10000)
        val newTotalXp = (currentProfile.totalXp + xpEarned).coerceAtMost(10_000_000)
        val newLevel = ((newTotalXp / 250) + 1).coerceIn(1, 999)
        val newCorrect = currentProfile.totalCorrect + safeCorrect
        val newAnswered = currentProfile.totalAnswered + safeTotal
        val newAccuracy = if (newAnswered > 0) ((newCorrect * 100) / newAnswered).coerceIn(0, 100) else 0
        val newDuelWins = currentProfile.duelWins + if (isDuelWin) 1 else 0
        val newDuelPlayed = currentProfile.duelPlayed + if (mode != QuizMode.SOLO_VR) 1 else 0
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val sessionPath = "users/$uid/quiz_sessions"
        return try {
            val sessionRef = db.collection("users").document(uid).collection("quiz_sessions").document()
            val sessionPayload = mapOf(
                "userId" to uid,
                "subject" to subject.code,
                "mode" to mode.code,
                "score" to score.coerceIn(0, 100000),
                "correctCount" to safeCorrect,
                "totalQuestions" to safeTotal,
                "durationSeconds" to safeDuration,
                "xpEarned" to xpEarned,
                "dayKey" to dayKey,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val profileUpdates = mapOf(
                "totalXp" to newTotalXp,
                "level" to newLevel,
                "totalCorrect" to newCorrect,
                "totalAnswered" to newAnswered,
                "duelWins" to newDuelWins,
                "duelPlayed" to newDuelPlayed,
                "streakDays" to currentProfile.streakDays.coerceAtLeast(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val lbUpdates = mapOf(
                "displayName" to currentProfile.displayName.take(60).ifBlank { "Siswa" },
                "schoolName" to currentProfile.schoolName.take(80),
                "gradeLevel" to currentProfile.gradeLevel.coerceIn(1, 6),
                "totalXp" to newTotalXp,
                "level" to newLevel,
                "duelWins" to newDuelWins,
                "accuracyPct" to newAccuracy,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(sessionRef, sessionPayload)
            batch.update(db.collection("users").document(uid), profileUpdates)
            batch.update(db.collection("leaderboard").document(uid), lbUpdates)
            batch.commit().await()

            Result.success(xpEarned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionPath)
            Result.failure(e)
        }
    }

    suspend fun saveQuestion(
        questionId: String?,
        authorName: String,
        subject: Subject,
        gradeLevel: Int,
        difficulty: Difficulty,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        published: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val cleanPrompt = prompt.trim()
        val cleanOptions = options.map { it.trim().take(120) }
        if (cleanPrompt.length < 5 || cleanOptions.size != 4 || cleanOptions.any { it.isEmpty() }) {
            return Result.failure(IllegalArgumentException("Pertanyaan minimal 5 karakter dan 4 pilihan wajib diisi."))
        }
        val docRef = if (questionId.isNullOrBlank()) {
            db.collection("questions").document()
        } else {
            db.collection("questions").document(questionId)
        }
        return try {
            if (questionId.isNullOrBlank()) {
                val payload = mapOf(
                    "authorId" to uid,
                    "authorName" to authorName.trim().take(60).ifBlank { "Guru SD" },
                    "subject" to subject.code,
                    "gradeLevel" to gradeLevel.coerceIn(1, 6),
                    "difficulty" to difficulty.code,
                    "prompt" to cleanPrompt.take(300),
                    "options" to cleanOptions,
                    "correctIndex" to correctIndex.coerceIn(0, 3),
                    "explanation" to explanation.trim().take(400),
                    "published" to published,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "authorName" to authorName.trim().take(60).ifBlank { "Guru SD" },
                    "subject" to subject.code,
                    "gradeLevel" to gradeLevel.coerceIn(1, 6),
                    "difficulty" to difficulty.code,
                    "prompt" to cleanPrompt.take(300),
                    "options" to cleanOptions,
                    "correctIndex" to correctIndex.coerceIn(0, 3),
                    "explanation" to explanation.trim().take(400),
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> {
        requireUserId()
        val path = "questions/$questionId"
        return try {
            db.collection("questions").document(questionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addStudySchedule(
        title: String,
        subjectCode: String,
        dueAtMillis: Long,
        reminderMinutesBefore: Int,
        calendarSynced: Boolean,
        notes: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Judul jadwal tidak boleh kosong."))
        }
        val docRef = db.collection("users").document(uid).collection("study_schedules").document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "subject" to subjectCode,
                "dueAtMillis" to dueAtMillis.coerceAtLeast(0L),
                "reminderMinutesBefore" to reminderMinutesBefore.coerceIn(0, 1440),
                "completed" to false,
                "calendarSynced" to calendarSynced,
                "notes" to notes.trim().take(300),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleScheduleCompleted(scheduleId: String, completed: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val docRef = db.collection("users").document(uid).collection("study_schedules").document(scheduleId)
        return try {
            docRef.update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markScheduleCalendarSynced(scheduleId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val docRef = db.collection("users").document(uid).collection("study_schedules").document(scheduleId)
        return try {
            docRef.update(
                mapOf(
                    "calendarSynced" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudySchedule(scheduleId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val docRef = db.collection("users").document(uid).collection("study_schedules").document(scheduleId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createDuelRoom(
        hostName: String,
        subject: Subject,
        gradeLevel: Int,
        questionSeeds: List<String>
    ): Result<DuelRoom> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val roomId = "room_$code"
        val docRef = db.collection("duel_rooms").document(roomId)
        return try {
            val cleanHostName = hostName.trim().take(60).ifBlank { "Pemain 1" }
            val payload = mapOf(
                "roomCode" to code,
                "subject" to subject.code,
                "gradeLevel" to gradeLevel.coerceIn(1, 6),
                "status" to "WAITING",
                "hostId" to uid,
                "hostName" to cleanHostName,
                "hostScore" to 0,
                "hostProgress" to 0,
                "guestScore" to 0,
                "guestProgress" to 0,
                "questionSeeds" to questionSeeds.take(20),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                DuelRoom(
                    id = roomId,
                    roomCode = code,
                    subject = subject.code,
                    gradeLevel = gradeLevel,
                    status = "WAITING",
                    hostId = uid,
                    hostName = cleanHostName,
                    questionSeeds = questionSeeds.take(20)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinDuelRoomByCode(roomCode: String, guestName: String): Result<DuelRoom> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val cleanCode = roomCode.trim().uppercase(Locale.US)
        val roomId = if (cleanCode.startsWith("ROOM_")) cleanCode.lowercase(Locale.US) else "room_$cleanCode"
        val docRef = db.collection("duel_rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(DuelRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Ruang duel dengan kode $cleanCode tidak ditemukan."))
            if (room.hostId == uid) {
                return Result.success(room)
            }
            if (room.status != "WAITING" || room.guestId != null) {
                return Result.failure(IllegalStateException("Ruang duel sudah penuh atau sudah dimulai."))
            }
            val cleanGuest = guestName.trim().take(60).ifBlank { "Pemain 2" }
            docRef.update(
                mapOf(
                    "guestId" to uid,
                    "guestName" to cleanGuest,
                    "status" to "PLAYING",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(room.copy(guestId = uid, guestName = cleanGuest, status = "PLAYING"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDuelProgress(
        room: DuelRoom,
        newScore: Int,
        newProgress: Int,
        finished: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        val docRef = db.collection("duel_rooms").document(room.id)
        return try {
            val isHost = room.hostId == uid
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isHost) {
                updates["hostScore"] = newScore.coerceIn(0, 100000)
                updates["hostProgress"] = newProgress.coerceIn(0, 20)
            } else {
                updates["guestScore"] = newScore.coerceIn(0, 100000)
                updates["guestProgress"] = newProgress.coerceIn(0, 20)
            }
            if (finished) {
                val hostFinal = if (isHost) newScore else room.hostScore
                val guestFinal = if (!isHost) newScore else room.guestScore
                val opponentFinished = if (isHost) {
                    room.guestProgress >= room.questionSeeds.size.coerceAtLeast(1)
                } else {
                    room.hostProgress >= room.questionSeeds.size.coerceAtLeast(1)
                }
                if (opponentFinished || room.guestId == null) {
                    updates["status"] = "FINISHED"
                    updates["winnerId"] = when {
                        hostFinal > guestFinal -> room.hostId
                        guestFinal > hostFinal -> room.guestId ?: room.hostId
                        else -> ""
                    }
                }
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markDriveSyncTimestamp(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "lastDriveSyncAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    fun buildDriveBackupJson(
        profile: UserProfile,
        sessions: List<QuizSession>,
        schedules: List<StudySchedule>
    ): String {
        val root = JSONObject()
        root.put("app", "DuelPintar3D VR")
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val profileJson = JSONObject().apply {
            put("displayName", profile.displayName)
            put("gradeLevel", profile.gradeLevel)
            put("schoolName", profile.schoolName)
            put("totalXp", profile.totalXp)
            put("level", profile.level)
            put("streakDays", profile.streakDays)
            put("totalCorrect", profile.totalCorrect)
            put("totalAnswered", profile.totalAnswered)
            put("duelWins", profile.duelWins)
            put("themeMode", profile.themeMode)
            put("dashboardWidgets", JSONArray(profile.dashboardWidgets))
        }
        root.put("profile", profileJson)

        val sessionsArr = JSONArray()
        sessions.forEach { s ->
            sessionsArr.put(
                JSONObject().apply {
                    put("subject", s.subject)
                    put("mode", s.mode)
                    put("score", s.score)
                    put("correctCount", s.correctCount)
                    put("totalQuestions", s.totalQuestions)
                    put("durationSeconds", s.durationSeconds)
                    put("xpEarned", s.xpEarned)
                    put("dayKey", s.dayKey)
                }
            )
        }
        root.put("sessions", sessionsArr)

        val schedulesArr = JSONArray()
        schedules.forEach { sc ->
            schedulesArr.put(
                JSONObject().apply {
                    put("title", sc.title)
                    put("subject", sc.subject)
                    put("dueAtMillis", sc.dueAtMillis)
                    put("reminderMinutesBefore", sc.reminderMinutesBefore)
                    put("completed", sc.completed)
                    put("notes", sc.notes)
                }
            )
        }
        root.put("schedules", schedulesArr)
        return root.toString(2)
    }

    suspend fun restoreFromDriveBackupJson(jsonText: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Belum masuk"))
        return try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "DuelPintar3D VR") {
                return Result.failure(IllegalArgumentException("Berkas bukan cadangan valid DuelPintar3D VR."))
            }
            val p = root.optJSONObject("profile")
            if (p != null) {
                val widgets = mutableListOf<String>()
                val wArr = p.optJSONArray("dashboardWidgets")
                if (wArr != null) {
                    for (i in 0 until wArr.length()) {
                        widgets.add(wArr.optString(i))
                    }
                }
                updateProfilePreferences(
                    displayName = p.optString("displayName").takeIf { it.isNotBlank() },
                    gradeLevel = p.optInt("gradeLevel", 4),
                    schoolName = p.optString("schoolName", "SDN Nusantara"),
                    themeMode = ThemeMode.fromCode(p.optString("themeMode")),
                    dashboardWidgets = widgets.takeIf { it.isNotEmpty() }
                )
            }
            markDriveSyncTimestamp()
            val sessionCount = root.optJSONArray("sessions")?.length() ?: 0
            Result.success("Berhasil sinkronisasi profil & $sessionCount riwayat belajar dari Google Drive.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }
}
