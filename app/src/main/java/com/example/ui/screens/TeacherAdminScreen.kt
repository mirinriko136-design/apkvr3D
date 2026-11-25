package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Difficulty
import com.example.model.QuizQuestion
import com.example.model.Subject
import com.example.model.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherAdminScreen(
    myQuestionsState: UiState<List<QuizQuestion>>,
    allQuestions: List<QuizQuestion>,
    onSaveQuestion: (
        questionId: String?,
        subject: Subject,
        gradeLevel: Int,
        difficulty: Difficulty,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        published: Boolean
    ) -> Unit,
    onDeleteQuestion: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedSubjectFilter by remember { mutableStateOf<Subject?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali")
                        }
                        Text("Dasbor Admin Guru", style = MaterialTheme.typography.headlineSmall)
                    }
                    Button(
                        onClick = {
                            editingQuestion = null
                            showCreateDialog = true
                        },
                        modifier = Modifier.testTag("add_question_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Soal")
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.height(136.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_teacher_studio_banner),
                            contentDescription = "Studio Guru",
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
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Studio Kurikulum & Bank Soal",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${allQuestions.size} soal aktif tersedia di Arena 3D/VR",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Cari pertanyaan soal...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedSubjectFilter == null,
                        onClick = { selectedSubjectFilter = null },
                        label = { Text("Semua") }
                    )
                    Subject.entries.forEach { subj ->
                        FilterChip(
                            selected = selectedSubjectFilter == subj,
                            onClick = { selectedSubjectFilter = subj },
                            label = { Text(subj.shortName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            item {
                Text("Soal Buatan Saya (Cloud)", style = MaterialTheme.typography.titleMedium)
            }

            when (myQuestionsState) {
                is UiState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(myQuestionsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val filtered = myQuestionsState.data.filter { q ->
                        (selectedSubjectFilter == null || q.subject == selectedSubjectFilter?.code) &&
                            (searchQuery.isBlank() || q.prompt.contains(searchQuery, ignoreCase = true))
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Anda belum menambahkan soal kustom. Klik 'Tambah Soal' untuk membuat soal pelajaran baru!",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { q ->
                            val subj = Subject.fromCode(q.subject)
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${subj.shortName} • Kls ${q.gradeLevel} • ${q.difficulty}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = subj.color
                                        )
                                        Row {
                                            IconButton(onClick = {
                                                editingQuestion = q
                                                showCreateDialog = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Soal")
                                            }
                                            IconButton(onClick = { onDeleteQuestion(q.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Hapus Soal")
                                            }
                                        }
                                    }
                                    Text(q.prompt, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Jawaban: ${q.options.getOrNull(q.correctIndex) ?: "-"} • ${if (q.published) "Dipublikasi" else "Draf"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        QuestionEditorDialog(
            initial = editingQuestion,
            onDismiss = { showCreateDialog = false },
            onSave = { subject, grade, difficulty, prompt, options, correctIdx, explanation, published ->
                onSaveQuestion(
                    editingQuestion?.id,
                    subject,
                    grade,
                    difficulty,
                    prompt,
                    options,
                    correctIdx,
                    explanation,
                    published
                )
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionEditorDialog(
    initial: QuizQuestion?,
    onDismiss: () -> Unit,
    onSave: (
        subject: Subject,
        gradeLevel: Int,
        difficulty: Difficulty,
        prompt: String,
        options: List<String>,
        correctIndex: Int,
        explanation: String,
        published: Boolean
    ) -> Unit
) {
    var subject by remember { mutableStateOf(Subject.fromCode(initial?.subject)) }
    var grade by remember { mutableIntStateOf(initial?.gradeLevel ?: 4) }
    var difficulty by remember { mutableStateOf(Difficulty.fromCode(initial?.difficulty)) }
    var prompt by remember { mutableStateOf(initial?.prompt ?: "") }
    var optA by remember { mutableStateOf(initial?.options?.getOrNull(0) ?: "") }
    var optB by remember { mutableStateOf(initial?.options?.getOrNull(1) ?: "") }
    var optC by remember { mutableStateOf(initial?.options?.getOrNull(2) ?: "") }
    var optD by remember { mutableStateOf(initial?.options?.getOrNull(3) ?: "") }
    var correctIdx by remember { mutableIntStateOf(initial?.correctIndex ?: 0) }
    var explanation by remember { mutableStateOf(initial?.explanation ?: "") }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    val canSave = prompt.trim().length >= 5 &&
        optA.isNotBlank() && optB.isNotBlank() && optC.isNotBlank() && optD.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Buat Soal Baru" else "Edit Soal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Subject.entries.forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj.shortName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { g ->
                        FilterChip(
                            selected = grade == g,
                            onClick = { grade = g },
                            label = { Text("K$g", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = difficulty == d,
                            onClick = { difficulty = d },
                            label = { Text(d.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Pertanyaan (min 5 huruf)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Pilihan A") }, singleLine = true)
                OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Pilihan B") }, singleLine = true)
                OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Pilihan C") }, singleLine = true)
                OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Pilihan D") }, singleLine = true)
                Text("Kunci Jawaban Benar", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                        FilterChip(
                            selected = correctIdx == idx,
                            onClick = { correctIdx = idx },
                            label = { Text(lbl) }
                        )
                    }
                }
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Pembahasan Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publikasikan ke Arena Kuis")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        subject,
                        grade,
                        difficulty,
                        prompt,
                        listOf(optA, optB, optC, optD),
                        correctIdx,
                        explanation,
                        published
                    )
                },
                enabled = canSave
            ) {
                Text("Simpan Soal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
