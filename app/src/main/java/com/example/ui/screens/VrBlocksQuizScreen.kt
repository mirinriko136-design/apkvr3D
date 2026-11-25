package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEffectsManager
import com.example.model.QuizQuestion
import com.example.model.Subject
import com.example.model.VrEnvironmentMode
import com.example.ui.components.VrBlocksStage
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VrBlocksQuizScreen(
    subject: Subject,
    questions: List<QuizQuestion>,
    isLocalTwoPlayer: Boolean,
    soundEnabled: Boolean,
    quizDurationSeconds: Int,
    initialEnvironmentMode: VrEnvironmentMode,
    soundEffects: SoundEffectsManager,
    onToggleSound: (Boolean) -> Unit,
    onEnvironmentChange: (VrEnvironmentMode) -> Unit,
    onSessionFinished: (score: Int, correct: Int, total: Int, durationSec: Int, isDuelWin: Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var isVrSplitMode by remember { mutableStateOf(false) }
    var environmentMode by remember(initialEnvironmentMode) { mutableStateOf(initialEnvironmentMode) }
    val activeSubject = remember(subject) { subject }
    var currentIndex by remember { mutableIntStateOf(0) }
    var player1Score by remember { mutableIntStateOf(0) }
    var player2Score by remember { mutableIntStateOf(0) }
    var player1Correct by remember { mutableIntStateOf(0) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var activePlayerTurn by remember { mutableIntStateOf(1) }
    var comboStreak by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var totalSecondsLeft by remember(quizDurationSeconds) { mutableIntStateOf(quizDurationSeconds) }
    var isFinished by remember { mutableStateOf(false) }
    var endedByTimeout by remember { mutableStateOf(false) }
    var sessionRecorded by remember { mutableStateOf(false) }
    val startedAtMillis = remember { mutableLongStateOf(System.currentTimeMillis()) }

    val safeQuestions = remember(questions) { questions.ifEmpty { emptyList() } }

    // Global countdown timer: when time hits 0, the quiz immediately ends!
    LaunchedEffect(quizDurationSeconds, isFinished, safeQuestions.isEmpty()) {
        if (isFinished || safeQuestions.isEmpty()) return@LaunchedEffect
        while (totalSecondsLeft > 0 && !isFinished) {
            delay(1000L)
            totalSecondsLeft = (totalSecondsLeft - 1).coerceAtLeast(0)
            if (totalSecondsLeft in 1..5) {
                soundEffects.playTick()
            }
        }
        if (totalSecondsLeft == 0 && !isFinished) {
            endedByTimeout = true
            isFinished = true
            soundEffects.playWrong()
        }
    }

    val formattedTime = remember(totalSecondsLeft) {
        val mins = totalSecondsLeft / 60
        val secs = totalSecondsLeft % 60
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Arena Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                    }
                    Column {
                        Text(
                            text = if (isLocalTwoPlayer) "Duel 2 Pemain • ${activeSubject.shortName}"
                            else "Arena VR • ${activeSubject.displayName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isLocalTwoPlayer) "Giliran Siswa Pemain $activePlayerTurn"
                            else "Soal ${minOf(currentIndex + 1, safeQuestions.size)} dari ${safeQuestions.size} • ${environmentMode.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = activeSubject.color
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isVrSplitMode,
                        onClick = {
                            soundEffects.playClick()
                            isVrSplitMode = !isVrSplitMode
                        },
                        label = { Text(if (isVrSplitMode) "VR Split" else "3D VR", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("toggle_vr_split_chip")
                    )
                    IconButton(onClick = { onToggleSound(!soundEnabled) }) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = "Suara Efek Edukatif"
                        )
                    }
                }
            }

            if (safeQuestions.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Soal belum tersedia untuk mata pelajaran ini.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBack) { Text("Kembali ke Dasbor") }
                    }
                }
                return@Column
            }

            if (isFinished) {
                val elapsedDuration = if (endedByTimeout) {
                    quizDurationSeconds
                } else {
                    (((System.currentTimeMillis() - startedAtMillis.longValue) / 1000L).toInt()).coerceAtLeast(1)
                }
                val p1Won = !isLocalTwoPlayer || player1Score >= player2Score
                LaunchedEffect(Unit) {
                    if (!sessionRecorded) {
                        sessionRecorded = true
                        if (!endedByTimeout) soundEffects.playVictory()
                        onSessionFinished(
                            player1Score,
                            player1Correct,
                            maxOf(answeredCount, 1),
                            elapsedDuration,
                            isLocalTwoPlayer && p1Won
                        )
                    }
                }
                QuizResultSummaryCard(
                    subject = activeSubject,
                    isLocalTwoPlayer = isLocalTwoPlayer,
                    endedByTimeout = endedByTimeout,
                    player1Score = player1Score,
                    player2Score = player2Score,
                    correctCount = player1Correct,
                    answeredCount = answeredCount,
                    totalQuestions = safeQuestions.size,
                    durationSeconds = elapsedDuration,
                    onReplay = {
                        currentIndex = 0
                        player1Score = 0
                        player2Score = 0
                        player1Correct = 0
                        answeredCount = 0
                        activePlayerTurn = 1
                        comboStreak = 0
                        selectedOption = null
                        isAnswerCorrect = null
                        totalSecondsLeft = quizDurationSeconds
                        endedByTimeout = false
                        isFinished = false
                        sessionRecorded = false
                        startedAtMillis.longValue = System.currentTimeMillis()
                    },
                    onBack = onBack
                )
                return@Column
            }

            // Live 3D Scene Switcher (Ruang Kelas 3D vs Halaman Sekolah Luas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VrEnvironmentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = environmentMode == mode,
                        onClick = {
                            soundEffects.playClick()
                            environmentMode = mode
                            onEnvironmentChange(mode)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (mode == VrEnvironmentMode.RUANG_KELAS) {
                                    Icons.Rounded.MeetingRoom
                                } else {
                                    Icons.Rounded.Park
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arena_env_chip_${mode.code}")
                    )
                }
            }

            val currentQuestion = safeQuestions[currentIndex]

            // Score / Combo / Global Countdown Timer Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLocalTwoPlayer) "P1: $player1Score  |  P2: $player2Score"
                            else "Skor: $player1Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (comboStreak >= 2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${comboStreak}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
                val timerCritical = totalSecondsLeft <= 15
                Surface(
                    color = if (timerCritical) CoralError.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = "Sisa Waktu Kuis",
                            tint = if (timerCritical) CoralError else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (timerCritical) CoralError else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (totalSecondsLeft.toFloat() / quizDurationSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                color = if (totalSecondsLeft <= 15) CoralError else activeSubject.color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Interactive Unity-style OpenGL ES 3D / VR XR Blocks Stage
            VrBlocksStage(
                accentColor = activeSubject.color,
                isVrSplitMode = isVrSplitMode,
                selectedBlockIndex = selectedOption?.takeIf { it >= 0 },
                isAnswerCorrect = isAnswerCorrect,
                environmentMode = environmentMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
            )

            // Question Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kelas ${currentQuestion.gradeLevel} • ${currentQuestion.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeSubject.color
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentQuestion.prompt,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4 Interactive Answer Blocks (A, B, C, D)
            val optionPrefixes = listOf("A", "B", "C", "D")
            val optionBlockColors = listOf(
                Color(0xFF3B82F6),
                Color(0xFF10B981),
                Color(0xFFF59E0B),
                Color(0xFFEC4899)
            )

            currentQuestion.options.forEachIndexed { idx, optionText ->
                val isSelected = selectedOption == idx
                val isCorrectOption = idx == currentQuestion.correctIndex
                val cardBg = when {
                    selectedOption == null -> MaterialTheme.colorScheme.surface
                    isCorrectOption -> EmeraldSuccess.copy(alpha = 0.16f)
                    isSelected -> CoralError.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                }

                Card(
                    onClick = {
                        if (selectedOption != null || isFinished) return@Card
                        selectedOption = idx
                        answeredCount += 1
                        val correct = idx == currentQuestion.correctIndex
                        isAnswerCorrect = correct
                        if (correct) {
                            comboStreak += 1
                            val timeBonus = (totalSecondsLeft / 10).coerceIn(0, 30)
                            val points = 100 + timeBonus + (comboStreak * 15)
                            if (isLocalTwoPlayer && activePlayerTurn == 2) {
                                player2Score += points
                            } else {
                                player1Score += points
                                player1Correct += 1
                            }
                            if (comboStreak >= 2) soundEffects.playCombo() else soundEffects.playCorrect()
                        } else {
                            comboStreak = 0
                            soundEffects.playWrong()
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("answer_block_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = optionBlockColors[idx % 4],
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = optionPrefixes[idx],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedOption != null && isCorrectOption) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Benar", tint = EmeraldSuccess)
                        } else if (isSelected && !isCorrectOption) {
                            Icon(Icons.Rounded.Close, contentDescription = "Salah", tint = CoralError)
                        }
                    }
                }
            }

            // Explanation & Next Button
            if (selectedOption != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnswerCorrect == true) EmeraldSuccess.copy(alpha = 0.12f)
                        else CoralError.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isAnswerCorrect == true) "Hebat! Jawaban Benar!" else "Hampir tepat! Yuk pelajari:",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isAnswerCorrect == true) EmeraldSuccess else CoralError
                        )
                        if (currentQuestion.explanation.isNotBlank()) {
                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = {
                                soundEffects.playClick()
                                if (currentIndex < safeQuestions.lastIndex) {
                                    currentIndex += 1
                                    selectedOption = null
                                    isAnswerCorrect = null
                                    if (isLocalTwoPlayer) {
                                        activePlayerTurn = if (activePlayerTurn == 1) 2 else 1
                                    }
                                } else {
                                    isFinished = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("next_question_button")
                        ) {
                            Text(if (currentIndex < safeQuestions.lastIndex) "Blok Soal Berikutnya" else "Lihat Hasil Kuis")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizResultSummaryCard(
    subject: Subject,
    isLocalTwoPlayer: Boolean,
    endedByTimeout: Boolean,
    player1Score: Int,
    player2Score: Int,
    correctCount: Int,
    answeredCount: Int,
    totalQuestions: Int,
    durationSeconds: Int,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_result_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = (if (endedByTimeout) CoralError else subject.color).copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (endedByTimeout) Icons.Rounded.TimerOff else Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (endedByTimeout) CoralError else subject.color,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = when {
                    endedByTimeout -> "Waktu Habis! Kuis Berakhir"
                    isLocalTwoPlayer -> when {
                        player1Score > player2Score -> "Pemain 1 Menang!"
                        player2Score > player1Score -> "Pemain 2 Menang!"
                        else -> "Skor Seri Hebat!"
                    }
                    else -> "Misi 3D/VR Selesai!"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (endedByTimeout) CoralError else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isLocalTwoPlayer) {
                    "Pemain 1: $player1Score pts  •  Pemain 2: $player2Score pts"
                } else {
                    "Skor Akhir: $player1Score • Benar $correctCount/$totalQuestions (Dijawab $answeredCount • $durationSeconds detik)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ke Dasbor")
                }
                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(containerColor = subject.color),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Main Lagi")
                }
            }
        }
    }
}
