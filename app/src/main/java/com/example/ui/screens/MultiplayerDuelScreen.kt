package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.SoundEffectsManager
import com.example.model.DuelRoom
import com.example.model.QuizMode
import com.example.model.QuizQuestion
import com.example.model.StarterQuestions
import com.example.model.Subject
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.VrBlocksStage
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiplayerDuelScreen(
    currentUserId: String,
    profile: UserProfile,
    openRoomsState: UiState<List<DuelRoom>>,
    activeRoom: DuelRoom?,
    allQuestions: List<QuizQuestion>,
    soundEffects: SoundEffectsManager,
    onCreateRoom: (Subject, Int) -> Unit,
    onJoinRoom: (String) -> Unit,
    onUpdateDuelProgress: (DuelRoom, Int, Int, Boolean) -> Unit,
    onLeaveRoom: () -> Unit,
    onRecordDuelResult: (Subject, Int, Int, Int, Int, Boolean) -> Unit,
    onLaunchLocalTwoPlayer: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf(Subject.MATEMATIKA) }
    var joinCodeInput by remember { mutableStateOf("") }

    if (activeRoom != null) {
        ActiveOnlineDuelArena(
            currentUserId = currentUserId,
            room = activeRoom,
            allQuestions = allQuestions,
            soundEffects = soundEffects,
            onUpdateProgress = { score, progress, finished ->
                onUpdateDuelProgress(activeRoom, score, progress, finished)
            },
            onRecordFinished = { score, correct, total, won ->
                onRecordDuelResult(
                    Subject.fromCode(activeRoom.subject),
                    score,
                    correct,
                    total,
                    60,
                    won
                )
            },
            onLeave = onLeaveRoom,
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.height(150.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_duel_versus_banner),
                            contentDescription = "Arena Duel Online",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9090F1E)))
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Arena Duel Multiplayer",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Kompetisi real-time antar siswa di sekolah melalui Firebase",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Create Duel Room Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Buat Ruang Duel Baru (Pemain 1)", style = MaterialTheme.typography.titleLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Subject.entries.forEach { subj ->
                                FilterChip(
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj.shortName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onCreateRoom(selectedSubject, profile.gradeLevel) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_duel_room_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buat Duel Online")
                            }
                            OutlinedButton(
                                onClick = { onLaunchLocalTwoPlayer(selectedSubject) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("launch_local_duel_button")
                            ) {
                                Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Duel 1 HP")
                            }
                        }
                    }
                }
            }

            // Join by Code
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Gabung Ruang Duel (Pemain 2)", style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(8) },
                                label = { Text("Kode Ruang (mis. AB12CD)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_code_input")
                            )
                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) onJoinRoom(joinCodeInput)
                                },
                                enabled = joinCodeInput.length >= 4,
                                modifier = Modifier.testTag("join_room_code_button")
                            ) {
                                Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Masuk")
                            }
                        }
                    }
                }
            }

            // Open Rooms Lobby
            item {
                Text("Lobi Ruang Terbuka", style = MaterialTheme.typography.titleLarge)
            }

            when (openRoomsState) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(openRoomsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val rooms = openRoomsState.data
                    if (rooms.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada ruang terbuka saat ini. Klik 'Buat Duel Online' untuk menantang teman sekolah!",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(rooms, key = { it.id }) { room ->
                            val subj = Subject.fromCode(room.subject)
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${subj.displayName} • Kode ${room.roomCode}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Pembuat: ${room.hostName} (Kelas ${room.gradeLevel})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(onClick = { onJoinRoom(room.roomCode) }) {
                                        Text(if (room.hostId == currentUserId) "Buka" else "Lawan!")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOnlineDuelArena(
    currentUserId: String,
    room: DuelRoom,
    allQuestions: List<QuizQuestion>,
    soundEffects: SoundEffectsManager,
    onUpdateProgress: (score: Int, progress: Int, finished: Boolean) -> Unit,
    onRecordFinished: (score: Int, correct: Int, total: Int, won: Boolean) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subj = Subject.fromCode(room.subject)
    val isHost = room.hostId == currentUserId
    val duelQuestions = remember(room.id, room.questionSeeds, allQuestions) {
        val byId = allQuestions.associateBy { it.id }
        val resolved = room.questionSeeds.mapNotNull { byId[it] }
        if (resolved.isNotEmpty()) resolved
        else StarterQuestions.bundled.filter { it.subject == subj.code }.take(5)
    }

    var localIndex by remember(room.id) { mutableIntStateOf(0) }
    var localScore by remember(room.id) { mutableIntStateOf(0) }
    var localCorrect by remember(room.id) { mutableIntStateOf(0) }
    var selectedOption by remember(room.id, localIndex) { mutableStateOf<Int?>(null) }
    var localCompleted by remember(room.id) { mutableStateOf(false) }
    var recorded by remember(room.id) { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ruang Duel #${room.roomCode}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${subj.displayName} • Status: ${room.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subj.color
                    )
                }
                OutlinedButton(onClick = onLeave) {
                    Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Keluar")
                }
            }

            // Live Scoreboard for Player 1 vs Player 2
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayerLiveBar(
                        label = "Pemain 1: ${room.hostName}",
                        score = room.hostScore,
                        progress = room.hostProgress,
                        total = duelQuestions.size.coerceAtLeast(1),
                        color = MaterialTheme.colorScheme.primary
                    )
                    PlayerLiveBar(
                        label = "Pemain 2: ${room.guestName ?: "Menunggu lawan bergabung..."}",
                        score = room.guestScore,
                        progress = room.guestProgress,
                        total = duelQuestions.size.coerceAtLeast(1),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (room.status == "WAITING" && room.guestId == null) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Bagikan Kode Ruang: ${room.roomCode}", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Menunggu Siswa Pemain 2 masuk agar duel dimulai serentak!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            if (localCompleted || room.status == "FINISHED") {
                val myFinal = if (isHost) room.hostScore else room.guestScore
                val oppFinal = if (isHost) room.guestScore else room.hostScore
                val won = myFinal > oppFinal
                LaunchedEffect(room.status, localCompleted) {
                    if (!recorded) {
                        recorded = true
                        soundEffects.playVictory()
                        onRecordFinished(localScore, localCorrect, duelQuestions.size, won)
                    }
                }
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when {
                                room.status != "FINISHED" -> "Selesai! Menunggu lawan menyelesaikan soal..."
                                won -> "Selamat, Kamu Menang Duel!"
                                myFinal == oppFinal -> "Hasil Duel Seri!"
                                else -> "Duel Selesai! Terus Berlatih!"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text("Skor Kamu: $myFinal  •  Skor Lawan: $oppFinal", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onLeave) { Text("Kembali ke Lobi Duel") }
                    }
                }
                return@Column
            }

            val q = duelQuestions[localIndex]
            VrBlocksStage(
                accentColor = subj.color,
                isVrSplitMode = false,
                selectedBlockIndex = selectedOption,
                isAnswerCorrect = selectedOption?.let { it == q.correctIndex },
                environmentMode = com.example.model.VrEnvironmentMode.HALAMAN_SEKOLAH,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Soal ${localIndex + 1}/${duelQuestions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = subj.color
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(q.prompt, style = MaterialTheme.typography.titleMedium)
                }
            }
            q.options.forEachIndexed { idx, opt ->
                val isChosen = selectedOption == idx
                val isCorrect = idx == q.correctIndex
                Card(
                    onClick = {
                        if (selectedOption != null) return@Card
                        selectedOption = idx
                        val correct = idx == q.correctIndex
                        if (correct) {
                            localScore += 120
                            localCorrect += 1
                            soundEffects.playCorrect()
                        } else {
                            soundEffects.playWrong()
                        }
                        val nextProgress = localIndex + 1
                        val finishedNow = nextProgress >= duelQuestions.size
                        onUpdateProgress(localScore, nextProgress, finishedNow)
                        if (finishedNow) {
                            localCompleted = true
                        } else {
                            localIndex = nextProgress
                            selectedOption = null
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            selectedOption == null -> MaterialTheme.colorScheme.surface
                            isCorrect -> EmeraldSuccess.copy(alpha = 0.16f)
                            isChosen -> CoralError.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (selectedOption != null && isCorrect) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                        } else if (isChosen && !isCorrect) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = CoralError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerLiveBar(
    label: String,
    score: Int,
    progress: Int,
    total: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("$score pts ($progress/$total)", style = MaterialTheme.typography.labelMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}
