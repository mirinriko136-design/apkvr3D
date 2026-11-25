package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.StudySchedule
import com.example.model.Subject
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.notifications.StudyReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleCalendarScreen(
    profile: UserProfile,
    schedulesState: UiState<List<StudySchedule>>,
    onSetDailyReminder: (Boolean, Int, Int) -> Unit,
    onAddSchedule: (title: String, subjectCode: String, dueAtMillis: Long, reminderMins: Int, notes: String, onSaved: (String) -> Unit) -> Unit,
    onToggleCompleted: (String, Boolean) -> Unit,
    onMarkCalendarSynced: (String) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderHour by remember(profile.dailyReminderHour) { mutableIntStateOf(profile.dailyReminderHour) }
    var reminderMinute by remember(profile.dailyReminderMinute) { mutableIntStateOf(profile.dailyReminderMinute) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetDailyReminder(true, reminderHour, reminderMinute)
            StudyReminderScheduler.scheduleDailyReminder(context, true, reminderHour, reminderMinute)
            StudyReminderScheduler.showNow(
                context,
                "Pengingat Belajar Aktif!",
                "Jadwal belajar harian pukul %02d:%02d telah diaktifkan.".format(reminderHour, reminderMinute)
            )
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kalender Cloud & Jadwal", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Notifikasi belajar harian & pengingat tenggat tugas otomatis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_schedule_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tugas")
                    }
                }
            }

            // Daily Study Reminder Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Pengingat Belajar Harian", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Setiap hari pukul %02d:%02d".format(reminderHour, reminderMinute),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = profile.dailyReminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !StudyReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSetDailyReminder(enabled, reminderHour, reminderMinute)
                                        StudyReminderScheduler.scheduleDailyReminder(context, enabled, reminderHour, reminderMinute)
                                    }
                                },
                                modifier = Modifier.testTag("daily_reminder_switch")
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(16 to 0, 18 to 30, 19 to 30).forEach { (h, m) ->
                                FilterChip(
                                    selected = reminderHour == h && reminderMinute == m,
                                    onClick = {
                                        reminderHour = h
                                        reminderMinute = m
                                        if (profile.dailyReminderEnabled) {
                                            onSetDailyReminder(true, h, m)
                                            StudyReminderScheduler.scheduleDailyReminder(context, true, h, m)
                                        }
                                    },
                                    label = { Text("%02d:%02d".format(h, m), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !StudyReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        StudyReminderScheduler.showNow(
                                            context,
                                            "Pengingat Belajar DuelPintar3D VR",
                                            "Ayo selesaikan 1 kuis blok 3D hari ini untuk menjaga streak ${profile.streakDays} hari!"
                                        )
                                    }
                                }
                            ) {
                                Text("Tes Notif", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Schedules List
            when (schedulesState) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(schedulesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val itemsList = schedulesState.data
                    if (itemsList.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Belum ada jadwal belajar atau tugas.")
                                    Text(
                                        text = "Tambahkan tugas baru untuk sinkronisasi ke Kalender Cloud & pengingat tenggat otomatis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(itemsList, key = { it.id }) { schedule ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = schedule.completed,
                                            onCheckedChange = { onToggleCompleted(schedule.id, it) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = schedule.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                textDecoration = if (schedule.completed) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            Text(
                                                text = "${schedule.subject} • Tenggat ${dateFormatter.format(Date(schedule.dueAtMillis))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onDeleteSchedule(schedule.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus Jadwal")
                                        }
                                    }

                                    if (schedule.notes.isNotBlank()) {
                                        Text(
                                            text = schedule.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Notif ${schedule.reminderMinutesBefore}m sebelum tenggat",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                                    data = CalendarContract.Events.CONTENT_URI
                                                    putExtra(CalendarContract.Events.TITLE, "DuelPintar3D: ${schedule.title}")
                                                    putExtra(
                                                        CalendarContract.Events.DESCRIPTION,
                                                        "Mapel: ${schedule.subject}. ${schedule.notes}"
                                                    )
                                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, schedule.dueAtMillis)
                                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, schedule.dueAtMillis + 3600_000L)
                                                }
                                                runCatching {
                                                    context.startActivity(calIntent)
                                                    onMarkCalendarSynced(schedule.id)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.EventAvailable,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (schedule.calendarSynced) "Tersinkron Kalender" else "Ke Google Kalender",
                                                style = MaterialTheme.typography.labelSmall
                                            )
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

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectCode, dueMillis, remMins, notes ->
                onAddSchedule(title, subjectCode, dueMillis, remMins, notes) { newId ->
                    StudyReminderScheduler.scheduleTaskDeadlineReminder(
                        context = context,
                        scheduleId = newId,
                        title = title,
                        dueAtMillis = dueMillis,
                        minutesBefore = remMins
                    )
                }
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectCode: String, dueAtMillis: Long, reminderMins: Int, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("MATEMATIKA") }
    var hoursFromNow by remember { mutableIntStateOf(2) }
    var reminderMinutes by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal & Tugas Cloud") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tugas / Jadwal Belajar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Mata Pelajaran", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Subject.entries.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj.code,
                            onClick = { selectedSubject = subj.code },
                            label = { Text(subj.shortName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Tenggat Waktu", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "1 Jam lagi", 3 to "3 Jam lagi", 24 to "Besok", 48 to "2 Hari").forEach { (hrs, lbl) ->
                        FilterChip(
                            selected = hoursFromNow == hrs,
                            onClick = { hoursFromNow = hrs },
                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Pengingat Otomatis Sebelum Tenggat", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = reminderMinutes == mins,
                            onClick = { reminderMinutes = mins },
                            label = { Text("${mins}m", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dueAt = System.currentTimeMillis() + (hoursFromNow * 3600_000L)
                    onConfirm(title, selectedSubject, dueAt, reminderMinutes, notes)
                },
                enabled = title.trim().isNotEmpty()
            ) {
                Text("Simpan Jadwal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
