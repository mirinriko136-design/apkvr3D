package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SubjectEnglishColor
import com.example.ui.theme.SubjectIndoColor
import com.example.ui.theme.SubjectIpaColor
import com.example.ui.theme.SubjectIpsColor
import com.example.ui.theme.SubjectMathColor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class Subject(
    val code: String,
    val displayName: String,
    val shortName: String,
    val color: Color,
    val description: String
) {
    BAHASA_INDONESIA(
        code = "BAHASA_INDONESIA",
        displayName = "Bahasa Indonesia",
        shortName = "B. Indo",
        color = SubjectIndoColor,
        description = "Sinonim, peribahasa, ide pokok & ejaan"
    ),
    MATEMATIKA(
        code = "MATEMATIKA",
        displayName = "Matematika",
        shortName = "MTK",
        color = SubjectMathColor,
        description = "Hitung cepat, pecahan & bangun ruang 3D"
    ),
    BAHASA_INGGRIS(
        code = "BAHASA_INGGRIS",
        displayName = "Bahasa Inggris",
        shortName = "B. Inggris",
        color = SubjectEnglishColor,
        description = "Kosakata, kalimat sehari-hari & grammar"
    ),
    IPA(
        code = "IPA",
        displayName = "IPA (Sains)",
        shortName = "IPA",
        color = SubjectIpaColor,
        description = "Tata surya, energi, ekosistem & makhluk hidup"
    ),
    IPS(
        code = "IPS",
        displayName = "IPS (Sosial)",
        shortName = "IPS",
        color = SubjectIpsColor,
        description = "Geografi Nusantara, budaya & sejarah bangsa"
    );

    companion object {
        fun fromCode(raw: String?): Subject =
            entries.firstOrNull { it.code == raw || it.name == raw } ?: MATEMATIKA
    }
}

enum class QuizMode(val code: String, val label: String) {
    SOLO_VR("SOLO_VR", "Arena 3D/VR Solo"),
    MULTIPLAYER_ONLINE("MULTIPLAYER_ONLINE", "Duel Online"),
    MULTIPLAYER_LOCAL("MULTIPLAYER_LOCAL", "Duel 2 Pemain");

    companion object {
        fun fromCode(raw: String?): QuizMode =
            entries.firstOrNull { it.code == raw } ?: SOLO_VR
    }
}

enum class Difficulty(val code: String, val label: String) {
    MUDAH("MUDAH", "Mudah"),
    SEDANG("SEDANG", "Sedang"),
    SULIT("SULIT", "Sulit");

    companion object {
        fun fromCode(raw: String?): Difficulty =
            entries.firstOrNull { it.code == raw } ?: MUDAH
    }
}

enum class ThemeMode(val code: String, val label: String) {
    SYSTEM("system", "Ikuti Sistem"),
    LIGHT("light", "Mode Terang"),
    DARK("dark", "Mode Gelap");

    companion object {
        fun fromCode(raw: String?): ThemeMode =
            entries.firstOrNull { it.code == raw } ?: SYSTEM
    }
}

enum class DashboardWidget(val id: String, val title: String, val subtitle: String) {
    VR_ARENA("vr_arena", "Arena Blok 3D/VR", "Pilih 5 mata pelajaran SD"),
    MULTIPLAYER("multiplayer", "Duel Multiplayer", "Tanding cepat 2 pemain"),
    WEEKLY_REPORT("weekly_report", "Laporan Mingguan", "Ringkasan target & XP 7 hari"),
    SCHEDULES("schedules", "Jadwal & Tugas Cloud", "Pengingat belajar hari ini"),
    LEADERBOARD("leaderboard", "Peringkat Sekolah", "Top juara DuelPintar3D VR");

    companion object {
        val defaultOrder: List<String> = entries.map { it.id }
        fun fromId(id: String): DashboardWidget? = entries.firstOrNull { it.id == id }
    }
}

enum class VrEnvironmentMode(val code: String, val label: String, val subtitle: String) {
    RUANG_KELAS("RUANG_KELAS", "Ruang Kelas 3D", "Meja belajar, papan tulis & lampu kelas"),
    HALAMAN_SEKOLAH("HALAMAN_SEKOLAH", "Halaman Sekolah Luas", "Lapangan hijau, gedung sekolah & tiang bendera");

    companion object {
        fun fromCode(raw: String?): VrEnvironmentMode =
            entries.firstOrNull { it.code == raw } ?: RUANG_KELAS
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Siswa Hebat",
    val role: String = "student",
    val gradeLevel: Int = 4,
    val schoolName: String = "SDN Nusantara",
    val avatarColor: String = "#2563EB",
    val totalXp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 1,
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val duelWins: Int = 0,
    val duelPlayed: Int = 0,
    val soundEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val themeMode: String = "system",
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 18,
    val dailyReminderMinute: Int = 30,
    val dashboardWidgets: List<String> = DashboardWidget.defaultOrder,
    val lastDriveSyncAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = Subject.MATEMATIKA.code,
    val mode: String = QuizMode.SOLO_VR.code,
    val score: Int = 0,
    val correctCount: Int = 0,
    val totalQuestions: Int = 5,
    val durationSeconds: Int = 60,
    val xpEarned: Int = 0,
    val dayKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudySchedule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "UMUM",
    val dueAtMillis: Long = 0L,
    val reminderMinutesBefore: Int = 30,
    val completed: Boolean = false,
    val calendarSynced: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Siswa",
    val schoolName: String = "SDN Nusantara",
    val gradeLevel: Int = 4,
    val totalXp: Int = 0,
    val level: Int = 1,
    val duelWins: Int = 0,
    val accuracyPct: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "Tim Kurikulum SD",
    val subject: String = Subject.MATEMATIKA.code,
    val gradeLevel: Int = 4,
    val difficulty: String = Difficulty.MUDAH.code,
    val prompt: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DuelRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val subject: String = Subject.MATEMATIKA.code,
    val gradeLevel: Int = 4,
    val status: String = "WAITING",
    val hostId: String = "",
    val hostName: String = "",
    val hostScore: Int = 0,
    val hostProgress: Int = 0,
    val guestId: String? = null,
    val guestName: String? = null,
    val guestScore: Int = 0,
    val guestProgress: Int = 0,
    val winnerId: String? = null,
    val questionSeeds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SubjectAnalytics(
    val subject: Subject,
    val attempts: Int,
    val accuracyPct: Int,
    val avgDurationSec: Int,
    val totalXp: Int
)

data class DailyProductivity(
    val dayKey: String,
    val label: String,
    val minutesStudied: Int,
    val questionsAnswered: Int,
    val xpEarned: Int
)

object StarterQuestions {
    val bundled: List<QuizQuestion> = listOf(
        // Bahasa Indonesia (6 soal)
        QuizQuestion(
            id = "starter_bi_1",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Sinonim dari kata 'rajin' adalah...",
            options = listOf("Giat", "Malas", "Lambat", "Lelah"),
            correctIndex = 0,
            explanation = "Giat memiliki arti yang sama dengan rajin atau tekun bekerja."
        ),
        QuizQuestion(
            id = "starter_bi_2",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Antonim (lawan kata) dari 'hemat' adalah...",
            options = listOf("Cermat", "Boros", "Untung", "Kaya"),
            correctIndex = 1,
            explanation = "Boros adalah lawan kata dari hemat."
        ),
        QuizQuestion(
            id = "starter_bi_3",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Peribahasa 'Besar pasak daripada tiang' artinya...",
            options = listOf("Rumah sangat kokoh", "Pengeluaran lebih besar daripada pendapatan", "Rajin menabung setiap hari", "Suka menolong sesama"),
            correctIndex = 1,
            explanation = "Artinya belanja atau pengeluaran lebih besar daripada penghasilan."
        ),
        QuizQuestion(
            id = "starter_bi_4",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 3,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Kalimat yang diakhiri tanda tanya (?) disebut kalimat...",
            options = listOf("Berita", "Perintah", "Tanya", "Seru"),
            correctIndex = 2,
            explanation = "Kalimat tanya digunakan untuk menanyakan sesuatu dan diakhiri tanda tanya."
        ),
        QuizQuestion(
            id = "starter_bi_5",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Kata baku yang benar sesuai KBBI adalah...",
            options = listOf("Apotik", "Praktek", "Apotek", "Nafas"),
            correctIndex = 2,
            explanation = "Bentuk baku yang tepat menurut KBBI adalah 'apotek', 'praktik', dan 'napas'."
        ),
        QuizQuestion(
            id = "starter_bi_6",
            subject = Subject.BAHASA_INDONESIA.code,
            gradeLevel = 6,
            difficulty = Difficulty.SULIT.code,
            prompt = "Gagasan utama yang menjadi dasar pengembangan paragraf disebut...",
            options = listOf("Kalimat penjelas", "Ide pokok", "Kata kunci", "Judul bacaan"),
            correctIndex = 1,
            explanation = "Ide pokok adalah gagasan inti yang mendasari terbentuknya sebuah paragraf."
        ),

        // Matematika (6 soal)
        QuizQuestion(
            id = "starter_mtk_1",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 3,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Hasil dari 24 x 5 adalah...",
            options = listOf("100", "110", "120", "140"),
            correctIndex = 2,
            explanation = "24 x 5 = (20 x 5) + (4 x 5) = 100 + 20 = 120."
        ),
        QuizQuestion(
            id = "starter_mtk_2",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Sebuah kubus memiliki jumlah sisi sebanyak...",
            options = listOf("4 sisi", "6 sisi", "8 sisi", "12 sisi"),
            correctIndex = 1,
            explanation = "Bangun ruang 3D kubus memiliki 6 sisi berbentuk persegi yang sama luas."
        ),
        QuizQuestion(
            id = "starter_mtk_3",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Bentuk pecahan paling sederhana dari 12/18 adalah...",
            options = listOf("2/3", "3/4", "4/6", "1/2"),
            correctIndex = 0,
            explanation = "Pembilang dan penyebut dibagi 6: 12:6 = 2 dan 18:6 = 3, hasilnya 2/3."
        ),
        QuizQuestion(
            id = "starter_mtk_4",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 4,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Keliling persegi dengan panjang sisi 15 cm adalah...",
            options = listOf("30 cm", "45 cm", "60 cm", "225 cm"),
            correctIndex = 2,
            explanation = "Keliling persegi = 4 x sisi = 4 x 15 cm = 60 cm."
        ),
        QuizQuestion(
            id = "starter_mtk_5",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Volume balok berukuran panjang 10 cm, lebar 4 cm, dan tinggi 5 cm adalah...",
            options = listOf("100 cm3", "150 cm3", "200 cm3", "250 cm3"),
            correctIndex = 2,
            explanation = "Volume balok = p x l x t = 10 x 4 x 5 = 200 cm3."
        ),
        QuizQuestion(
            id = "starter_mtk_6",
            subject = Subject.MATEMATIKA.code,
            gradeLevel = 6,
            difficulty = Difficulty.SULIT.code,
            prompt = "Jika 35% siswa menyukai IPA dari total 40 siswa, jumlah siswa yang suka IPA adalah...",
            options = listOf("12 siswa", "14 siswa", "16 siswa", "18 siswa"),
            correctIndex = 1,
            explanation = "35/100 x 40 = 1400/100 = 14 siswa."
        ),

        // Bahasa Inggris (6 soal)
        QuizQuestion(
            id = "starter_ing_1",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 3,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Arti kata bahasa Inggris 'Library' adalah...",
            options = listOf("Kantin", "Perpustakaan", "Lapangan", "Laboratorium"),
            correctIndex = 1,
            explanation = "Library artinya perpustakaan tempat membaca dan meminjam buku."
        ),
        QuizQuestion(
            id = "starter_ing_2",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Good morning! How are you? Jawaban yang paling tepat adalah...",
            options = listOf("I am fine, thank you", "Good night", "See you tomorrow", "My name is Budi"),
            correctIndex = 0,
            explanation = "'How are you?' menanyakan kabar, dijawab dengan 'I am fine, thank you'."
        ),
        QuizQuestion(
            id = "starter_ing_3",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 4,
            difficulty = Difficulty.SEDANG.code,
            prompt = "She ___ to school by bicycle every morning.",
            options = listOf("go", "goes", "going", "went"),
            correctIndex = 1,
            explanation = "Untuk subjek tunggal 'She' pada kebiasaan (Simple Present), kata kerja memakai 'goes'."
        ),
        QuizQuestion(
            id = "starter_ing_4",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 5,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Bahasa Inggris dari hari 'Rabu' adalah...",
            options = listOf("Tuesday", "Wednesday", "Thursday", "Friday"),
            correctIndex = 1,
            explanation = "Wednesday adalah hari Rabu dalam bahasa Inggris."
        ),
        QuizQuestion(
            id = "starter_ing_5",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "A place where doctors and nurses help sick people is a...",
            options = listOf("Post office", "Hospital", "Market", "Bakery"),
            correctIndex = 1,
            explanation = "Hospital adalah rumah sakit tempat dokter dan perawat merawat orang sakit."
        ),
        QuizQuestion(
            id = "starter_ing_6",
            subject = Subject.BAHASA_INGGRIS.code,
            gradeLevel = 6,
            difficulty = Difficulty.SULIT.code,
            prompt = "Yesterday, my family ___ to Bali for holiday.",
            options = listOf("travel", "travels", "traveled", "traveling"),
            correctIndex = 2,
            explanation = "Kata 'Yesterday' menandakan masa lampau (Past Tense), sehingga memakai 'traveled'."
        ),

        // IPA (6 soal)
        QuizQuestion(
            id = "starter_ipa_1",
            subject = Subject.IPA.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Planet yang paling dekat dengan Matahari dalam tata surya adalah...",
            options = listOf("Venus", "Merkurius", "Bumi", "Mars"),
            correctIndex = 1,
            explanation = "Merkurius adalah planet pertama yang paling dekat dengan Matahari."
        ),
        QuizQuestion(
            id = "starter_ipa_2",
            subject = Subject.IPA.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Tumbuhan hijau membuat makanannya sendiri melalui proses...",
            options = listOf("Respirasi", "Fotosintesis", "Penguapan", "Fermentasi"),
            correctIndex = 1,
            explanation = "Fotosintesis menggunakan cahaya matahari, air, dan karbon dioksida untuk membuat glukosa."
        ),
        QuizQuestion(
            id = "starter_ipa_3",
            subject = Subject.IPA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Perubahan wujud benda dari gas menjadi cair disebut...",
            options = listOf("Mencair", "Membeku", "Mengembun", "Menyublim"),
            correctIndex = 2,
            explanation = "Mengembun adalah perubahan gas menjadi cair, misalnya titik air di daun pagi hari."
        ),
        QuizQuestion(
            id = "starter_ipa_4",
            subject = Subject.IPA.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Bagian darah manusia yang berfungsi mengangkut oksigen adalah...",
            options = listOf("Sel darah merah", "Sel darah putih", "Keping darah", "Plasma darah"),
            correctIndex = 0,
            explanation = "Sel darah merah mengandung hemoglobin yang mengikat dan mengedarkan oksigen."
        ),
        QuizQuestion(
            id = "starter_ipa_5",
            subject = Subject.IPA.code,
            gradeLevel = 3,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Hewan pemakan tumbuhan saja disebut...",
            options = listOf("Karnivora", "Herbivora", "Omnivora", "Insektivora"),
            correctIndex = 1,
            explanation = "Herbivora adalah hewan pemakan tumbuhan seperti sapi, kambing, dan kelinci."
        ),
        QuizQuestion(
            id = "starter_ipa_6",
            subject = Subject.IPA.code,
            gradeLevel = 6,
            difficulty = Difficulty.SULIT.code,
            prompt = "Gaya yang menghambat gerak benda karena dua permukaan bersentuhan disebut gaya...",
            options = listOf("Gravitasi", "Magnet", "Gesek", "Pegas"),
            correctIndex = 2,
            explanation = "Gaya gesek terjadi saat dua permukaan saling bersentuhan dan menahan gerak."
        ),

        // IPS (6 soal)
        QuizQuestion(
            id = "starter_ips_1",
            subject = Subject.IPS.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Rumah adat Tongkonan berasal dari provinsi...",
            options = listOf("Sumatera Barat", "Sulawesi Selatan", "Kalimantan Timur", "Jawa Tengah"),
            correctIndex = 1,
            explanation = "Rumah adat Tongkonan adalah warisan budaya suku Toraja di Sulawesi Selatan."
        ),
        QuizQuestion(
            id = "starter_ips_2",
            subject = Subject.IPS.code,
            gradeLevel = 4,
            difficulty = Difficulty.MUDAH.code,
            prompt = "Teks Proklamasi Kemerdekaan Indonesia dibacakan pada tanggal...",
            options = listOf("17 Agustus 1945", "10 November 1945", "28 Oktober 1928", "1 Juni 1945"),
            correctIndex = 0,
            explanation = "Proklamasi Kemerdekaan RI dibacakan oleh Ir. Soekarno pada 17 Agustus 1945."
        ),
        QuizQuestion(
            id = "starter_ips_3",
            subject = Subject.IPS.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Pulau terbesar yang seluruh wilayahnya masuk ke dalam Indonesia adalah...",
            options = listOf("Sumatera", "Jawa", "Papua", "Sulawesi"),
            correctIndex = 0,
            explanation = "Sumatera merupakan pulau terbesar yang seluruh wilayahnya berada di Indonesia."
        ),
        QuizQuestion(
            id = "starter_ips_4",
            subject = Subject.IPS.code,
            gradeLevel = 5,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Kegiatan menyalurkan barang dari produsen ke konsumen disebut...",
            options = listOf("Produksi", "Distribusi", "Konsumsi", "Investasi"),
            correctIndex = 1,
            explanation = "Distribusi adalah kegiatan menyalurkan barang atau jasa hingga sampai ke konsumen."
        ),
        QuizQuestion(
            id = "starter_ips_5",
            subject = Subject.IPS.code,
            gradeLevel = 6,
            difficulty = Difficulty.SEDANG.code,
            prompt = "Organisasi kerja sama negara-negara di kawasan Asia Tenggara adalah...",
            options = listOf("PBB", "ASEAN", "APEC", "OPEC"),
            correctIndex = 1,
            explanation = "ASEAN didirikan pada 8 Agustus 1967 sebagai wadah kerja sama negara Asia Tenggara."
        ),
        QuizQuestion(
            id = "starter_ips_6",
            subject = Subject.IPS.code,
            gradeLevel = 6,
            difficulty = Difficulty.SULIT.code,
            prompt = "Garis khayal yang membelah bumi menjadi belahan utara dan selatan disebut...",
            options = listOf("Garis bujur", "Garis khatulistiwa", "Garis pantai", "Garis batas"),
            correctIndex = 1,
            explanation = "Garis khatulistiwa (ekuator) melintang di 0 derajat, termasuk melewati Kota Pontianak."
        )
    )
}
