package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DailyProductivity
import com.example.model.DashboardWidget
import com.example.model.LeaderboardEntry
import com.example.model.StudySchedule
import com.example.model.Subject
import com.example.model.ThemeMode
import com.example.model.UserProfile
import com.example.model.VrEnvironmentMode
import com.example.ui.components.WeeklyProductivityChart
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    weeklyProductivity: List<DailyProductivity>,
    schedules: List<StudySchedule>,
    leaderboard: List<LeaderboardEntry>,
    themeMode: ThemeMode,
    quizDurationSeconds: Int,
    vrEnvironmentMode: VrEnvironmentMode,
    onToggleTheme: (ThemeMode) -> Unit,
    onUpdateQuizSettings: (Int, VrEnvironmentMode) -> Unit,
    onSaveDashboardWidgets: (List<String>) -> Unit,
    onLaunchVrQuiz: (Subject, Boolean) -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenTeacherAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomizeDialog by remember { mutableStateOf(false) }
    val activeWidgets = remember(profile.dashboardWidgets) {
        profile.dashboardWidgets.ifEmpty { DashboardWidget.defaultOrder }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header with Quick Theme & Customize Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Halo, ${profile.displayName}!",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roleLabel = if (profile.role == "teacher") "Guru / Admin" else "Siswa Kelas ${profile.gradeLevel} SD"
                        Text(
                            text = "$roleLabel • Level ${profile.level} (${profile.totalXp} XP)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val next = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                onToggleTheme(next)
                            },
                            modifier = Modifier.testTag("quick_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Ganti Tema Gelap/Terang"
                            )
                        }
                        FilledTonalButton(
                            onClick = { showCustomizeDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("customize_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DashboardCustomize,
                                contentDescription = "Atur Dasbor",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Atur", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Hero VR Banner + Level Progress
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.height(178.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vr_arena_banner),
                            contentDescription = "Arena Kuis 3D VR",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xE60B1120), Color(0x660B1120))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFF59E0B),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Streak ${profile.streakDays} Hari",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = onOpenTeacherAdmin,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("open_teacher_studio_chip")
                                ) {
                                    Icon(
                                        Icons.Rounded.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (profile.role == "teacher") "Studio Guru (Aktif)" else "Studio Guru",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "DuelPintar3D VR",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tebak Blok 3D VR • ${vrEnvironmentMode.label} (${quizDurationSeconds}d)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val xpInLevel = (profile.totalXp % 250) / 250f
                                LinearProgressIndicator(
                                    progress = { xpInLevel },
                                    color = Color(0xFF22D3EE),
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Customizable Widgets
            items(activeWidgets, key = { it }) { widgetId ->
                when (widgetId) {
                    DashboardWidget.VR_ARENA.id -> VrSubjectsWidget(
                        quizDurationSeconds = quizDurationSeconds,
                        vrEnvironmentMode = vrEnvironmentMode,
                        onUpdateQuizSettings = onUpdateQuizSettings,
                        onLaunchVrQuiz = onLaunchVrQuiz
                    )

                    DashboardWidget.MULTIPLAYER.id -> MultiplayerHighlightWidget(
                        onOpenMultiplayer = onOpenMultiplayer,
                        onLaunchLocalDuel = { onLaunchVrQuiz(Subject.MATEMATIKA, true) }
                    )

                    DashboardWidget.WEEKLY_REPORT.id -> WeeklySummaryWidget(
                        days = weeklyProductivity
                    )

                    DashboardWidget.SCHEDULES.id -> UpcomingSchedulesWidget(
                        schedules = schedules,
                        onOpenSchedules = onOpenSchedules
                    )

                    DashboardWidget.LEADERBOARD.id -> MiniLeaderboardWidget(
                        entries = leaderboard,
                        onOpenLeaderboard = onOpenLeaderboard
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showCustomizeDialog) {
        CustomizeDashboardDialog(
            currentOrder = activeWidgets,
            onDismiss = { showCustomizeDialog = false },
            onSave = { updated ->
                onSaveDashboardWidgets(updated)
                showCustomizeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VrSubjectsWidget(
    quizDurationSeconds: Int,
    vrEnvironmentMode: VrEnvironmentMode,
    onUpdateQuizSettings: (Int, VrEnvironmentMode) -> Unit,
    onLaunchVrQuiz: (Subject, Boolean) -> Unit
) {
    var quickSubject by remember { mutableStateOf(Subject.MATEMATIKA) }
    val durations = remember { listOf(60, 120, 180, 300) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Environment & Timer Settings Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pengaturan Dunia VR 3D & Waktu Kuis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih lokasi arena VR 3D (Ruang Kelas atau Halaman Sekolah Luas):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VrEnvironmentMode.entries.forEach { mode ->
                        val selected = vrEnvironmentMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateQuizSettings(quizDurationSeconds, mode) },
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
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("env_chip_${mode.code}")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Batas Waktu Kuis (habis = kuis berakhir):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durations.forEach { sec ->
                        val mins = sec / 60
                        FilterChip(
                            selected = quizDurationSeconds == sec,
                            onClick = { onUpdateQuizSettings(sec, vrEnvironmentMode) },
                            label = { Text("${sec}d (${mins}m)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("duration_chip_$sec")
                        )
                    }
                }

                Button(
                    onClick = { onLaunchVrQuiz(quickSubject, false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_quiz_now_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mulai Game / Kuis ${quickSubject.shortName} (${quizDurationSeconds}d)")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pilih Pelajaran Arena 3D/VR", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "5 Mapel SD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Subject.entries, key = { it.code }) { subject ->
                Card(
                    modifier = Modifier
                        .width(172.dp)
                        .testTag("subject_card_${subject.code}")
                        .clickable {
                            quickSubject = subject
                            onLaunchVrQuiz(subject, false)
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = subject.color.copy(alpha = 0.14f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = subject.color,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.ViewInAr,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Surface(
                                color = subject.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Main ${subject.displayName}",
                                    tint = subject.color,
                                    modifier = Modifier.padding(4.dp).size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = subject.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiplayerHighlightWidget(
    onOpenMultiplayer: () -> Unit,
    onLaunchLocalDuel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.height(118.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_duel_versus_banner),
                    contentDescription = "Duel Multiplayer Siswa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC090F1E)))
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Mode Duel Siswa 1 vs Pemain 2",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Tanding daring real-time atau bergantian di 1 perangkat!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenMultiplayer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_duel_online_button")
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Duel Online")
                }
                FilledTonalButton(
                    onClick = onLaunchLocalDuel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_duel_local_button")
                ) {
                    Text("Duel 2 Pemain")
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryWidget(days: List<DailyProductivity>) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val totalMins = days.sumOf { it.minutesStudied }
            val totalXp = days.sumOf { it.xpEarned }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Produktivitas Mingguan", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "$totalMins menit belajar • +$totalXp XP minggu ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            WeeklyProductivityChart(days = days)
        }
    }
}

@Composable
private fun UpcomingSchedulesWidget(
    schedules: List<StudySchedule>,
    onOpenSchedules: () -> Unit
) {
    val pending = schedules.filter { !it.completed }.take(3)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Jadwal & Tugas Mendatang", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onOpenSchedules) {
                    Text("Kelola")
                }
            }
            if (pending.isEmpty()) {
                Text(
                    text = "Belum ada tugas terjadwal. Tambahkan pengingat belajar agar tersinkron ke Kalender Cloud!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pending.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLeaderboardWidget(
    entries: List<LeaderboardEntry>,
    onOpenLeaderboard: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Top Juara Sekolah", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onOpenLeaderboard) {
                    Text("Lihat Semua")
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = "Mainkan kuis 3D untuk meraih peringkat teratas!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.take(3).forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1} ${entry.displayName} (Kls ${entry.gradeLevel})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.totalXp} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeDashboardDialog(
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val allWidgets = DashboardWidget.entries
    var enabledOrder by remember {
        mutableStateOf(currentOrder.filter { DashboardWidget.fromId(it) != null })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kustomisasi Tampilan Dasbor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Atur urutan dan widget yang tampil di halaman utama Anda:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                allWidgets.forEach { widget ->
                    val isEnabled = enabledOrder.contains(widget.id)
                    val idx = enabledOrder.indexOf(widget.id)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(widget.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(widget.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isEnabled) {
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val mutable = enabledOrder.toMutableList()
                                            val item = mutable.removeAt(idx)
                                            mutable.add(idx - 1, item)
                                            enabledOrder = mutable
                                        }
                                    },
                                    enabled = idx > 0
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naikkan")
                                }
                                IconButton(
                                    onClick = {
                                        if (idx < enabledOrder.lastIndex) {
                                            val mutable = enabledOrder.toMutableList()
                                            val item = mutable.removeAt(idx)
                                            mutable.add(idx + 1, item)
                                            enabledOrder = mutable
                                        }
                                    },
                                    enabled = idx < enabledOrder.lastIndex
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turunkan")
                                }
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    enabledOrder = if (checked) {
                                        enabledOrder + widget.id
                                    } else {
                                        if (enabledOrder.size > 1) enabledOrder - widget.id else enabledOrder
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(enabledOrder) }) {
                Text("Simpan Tata Letak")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
