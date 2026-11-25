package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddToDrive
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.model.DailyProductivity
import com.example.model.SubjectAnalytics
import com.example.model.ThemeMode
import com.example.model.UserProfile
import com.example.security.BiometricAuthHelper
import com.example.ui.components.SubjectMasteryChart
import com.example.ui.components.WeeklyProductivityChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsProfileScreen(
    profile: UserProfile,
    subjectAnalytics: List<SubjectAnalytics>,
    weeklyProductivity: List<DailyProductivity>,
    themeMode: ThemeMode,
    soundEnabled: Boolean,
    onUpdateProfile: (displayName: String, role: String, gradeLevel: Int, schoolName: String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onBuildDriveBackupJson: () -> String,
    onDriveBackupExported: () -> Unit,
    onRestoreFromDriveBackupJson: (String) -> Unit,
    onOpenTeacherAdmin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    val driveExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = onBuildDriveBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onDriveBackupExported()
            }
        }
    }

    val driveImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onRestoreFromDriveBackupJson(text)
                }
            }
        }
    }

    val unityScriptExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bundle = com.example.data.UnityVrExportHelper.buildUnityVrExportBundle()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bundle.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val accuracyOverall = if (profile.totalAnswered > 0) {
        (profile.totalCorrect * 100) / profile.totalAnswered
    } else 0

    val bestSubject = subjectAnalytics.filter { it.attempts > 0 }.maxByOrNull { it.accuracyPct }
    val weakestSubject = subjectAnalytics.filter { it.attempts > 0 }.minByOrNull { it.accuracyPct }
    val weeklyMinutes = weeklyProductivity.sumOf { it.minutesStudied }
    val weeklyQuestions = weeklyProductivity.sumOf { it.questionsAnswered }
    val weeklyXp = weeklyProductivity.sumOf { it.xpEarned }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = profile.displayName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "${profile.schoolName} • Kelas ${profile.gradeLevel} SD",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Peran: ${if (profile.role == "teacher") "Guru / Admin" else "Siswa"} • Level ${profile.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Ubah Profil")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill("Total XP", "${profile.totalXp}", Modifier.weight(1f))
                            StatPill("Akurasi", "$accuracyOverall%", Modifier.weight(1f))
                            StatPill("Duel Menang", "${profile.duelWins}", Modifier.weight(1f))
                        }

                        OutlinedButton(
                            onClick = onOpenTeacherAdmin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buka Dasbor Admin Guru (Kelola Soal)")
                        }
                    }
                }
            }

            // Deep Performance Analytics
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Analisis Performa Mendalam", style = MaterialTheme.typography.titleLarge)
                        }
                        SubjectMasteryChart(items = subjectAnalytics)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val insight = when {
                                bestSubject == null -> "Belum ada sesi selesai. Mainkan kuis 3D di setiap mata pelajaran untuk melihat evaluasi lengkap!"
                                bestSubject == weakestSubject -> "Bagus! Kamu baru mencoba ${bestSubject.subject.displayName} (${bestSubject.accuracyPct}%). Coba 4 mapel lainnya!"
                                else -> "Kekuatan terbaik: ${bestSubject.subject.displayName} (${bestSubject.accuracyPct}%). Rekomendasi latihan: ${weakestSubject?.subject?.displayName} (${weakestSubject?.accuracyPct}%)."
                            }
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Weekly Productivity Report
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
                            Column {
                                Text("Laporan Produktivitas Mingguan", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "$weeklyMinutes menit • $weeklyQuestions soal • +$weeklyXp XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareText = "Laporan Belajar Mingguan ${profile.displayName} (DuelPintar3D VR):\n" +
                                        "- Total Waktu: $weeklyMinutes menit\n" +
                                        "- Soal Dijawab: $weeklyQuestions soal\n" +
                                        "- XP Minggu Ini: +$weeklyXp XP\n" +
                                        "- Akurasi Keseluruhan: $accuracyOverall%"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Mingguan"))
                                }
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Bagikan Laporan")
                            }
                        }
                        WeeklyProductivityChart(days = weeklyProductivity)
                    }
                }
            }

            // Google Drive Sync Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AddToDrive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Sinkronisasi Google Drive", style = MaterialTheme.typography.titleLarge)
                        }
                        val lastSyncLabel = profile.lastDriveSyncAt?.toDate()?.let { date ->
                            "Sinkron terakhir: " + SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(date)
                        } ?: "Simpan atau pulihkan progres belajar langsung ke folder Google Drive Anda."
                        Text(
                            text = lastSyncLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val fileName = "DuelPintar3D_Progres_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json"
                                    driveExportLauncher.launch(fileName)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drive_backup_button")
                            ) {
                                Icon(Icons.Rounded.AddToDrive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan Drive")
                            }
                            OutlinedButton(
                                onClick = { driveImportLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("drive_restore_button")
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pulihkan")
                            }
                        }
                        OutlinedButton(
                            onClick = { unityScriptExportLauncher.launch("DuelPintar3D_UnityVR_Scripts.txt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_unity_vr_scripts_button")
                        ) {
                            Text("Ekspor Skrip Unity VR 3D (C# + Firebase Bridge)")
                        }
                    }
                }
            }

            // Security & App Settings (Biometric, Theme, Sound, Sign Out)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Keamanan & Kenyamanan Mata", style = MaterialTheme.typography.titleLarge)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Kunci Login Biometrik", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Verifikasi sidik jari/wajah saat membuka aplikasi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = profile.biometricEnabled,
                                onCheckedChange = { wantEnable ->
                                    if (!wantEnable) {
                                        onSetBiometricEnabled(false)
                                    } else {
                                        val activity = context as? FragmentActivity
                                        if (activity != null && BiometricAuthHelper.isBiometricAvailable(activity)) {
                                            BiometricAuthHelper.promptBiometric(
                                                activity = activity,
                                                onSuccess = { onSetBiometricEnabled(true) },
                                                onError = { }
                                            )
                                        } else {
                                            onSetBiometricEnabled(true)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("biometric_toggle_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(10.dp))
                                Text("Efek Suara Edukatif", style = MaterialTheme.typography.titleMedium)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = onSetSoundEnabled,
                                modifier = Modifier.testTag("sound_toggle_switch")
                            )
                        }

                        Text("Tema Tampilan", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar Akun Google")
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { name, role, grade, school ->
                onUpdateProfile(name, role, grade, school)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, grade: Int, school: String) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var school by remember { mutableStateOf(profile.schoolName) }
    var role by remember { mutableStateOf(profile.role) }
    var grade by remember { mutableIntStateOf(profile.gradeLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Profil Belajar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text("Nama Sekolah (SD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Peran Akun", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == "student",
                        onClick = { role = "student" },
                        label = { Text("Siswa") }
                    )
                    FilterChip(
                        selected = role == "teacher",
                        onClick = { role = "teacher" },
                        label = { Text("Guru") }
                    )
                }
                Text("Kelas SD", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { g ->
                        FilterChip(
                            selected = grade == g,
                            onClick = { grade = g },
                            label = { Text("$g") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, role, grade, school) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
