package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeMode
import com.example.model.VrEnvironmentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "duel_pintar_prefs")

data class LocalAppPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 18,
    val dailyReminderMinute: Int = 30,
    val vrSplitViewDefault: Boolean = false,
    val pendingLoginRole: String = "student",
    val quizDurationSeconds: Int = 120,
    val vrEnvironmentMode: VrEnvironmentMode = VrEnvironmentMode.RUANG_KELAS
)

class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val VR_SPLIT_VIEW = booleanPreferencesKey("vr_split_view")
        val PENDING_LOGIN_ROLE = stringPreferencesKey("pending_login_role")
        val QUIZ_DURATION_SECONDS = intPreferencesKey("quiz_duration_seconds")
        val VR_ENVIRONMENT_MODE = stringPreferencesKey("vr_environment_mode")
    }

    val prefsFlow: Flow<LocalAppPrefs> = context.dataStore.data.map { prefs ->
        LocalAppPrefs(
            themeMode = ThemeMode.fromCode(prefs[Keys.THEME_MODE]),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 18,
            dailyReminderMinute = prefs[Keys.DAILY_REMINDER_MINUTE] ?: 30,
            vrSplitViewDefault = prefs[Keys.VR_SPLIT_VIEW] ?: false,
            pendingLoginRole = prefs[Keys.PENDING_LOGIN_ROLE] ?: "student",
            quizDurationSeconds = (prefs[Keys.QUIZ_DURATION_SECONDS] ?: 120).coerceIn(30, 600),
            vrEnvironmentMode = VrEnvironmentMode.fromCode(prefs[Keys.VR_ENVIRONMENT_MODE])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.code }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.DAILY_REMINDER_HOUR] = hour
            it[Keys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setVrSplitViewDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VR_SPLIT_VIEW] = enabled }
    }

    suspend fun setPendingLoginRole(role: String) {
        context.dataStore.edit { it[Keys.PENDING_LOGIN_ROLE] = role }
    }

    suspend fun setQuizSettings(durationSeconds: Int, environmentMode: VrEnvironmentMode) {
        context.dataStore.edit {
            it[Keys.QUIZ_DURATION_SECONDS] = durationSeconds.coerceIn(30, 600)
            it[Keys.VR_ENVIRONMENT_MODE] = environmentMode.code
        }
    }
}
