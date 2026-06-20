package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsRepositoryImpl(
    private val prefs: SharedPreferences
) : SettingsRepository {

    override suspend fun load(): Settings {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val cooldownType = prefs.getString(KEY_COOLDOWN_TYPE, TYPE_DAILY_RESET) ?: TYPE_DAILY_RESET
        val cooldownConfig = when (cooldownType) {
            TYPE_N_HOUR -> CooldownConfig.NHourInterval(
                prefs.getInt(KEY_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS)
            )
            else -> CooldownConfig.DailyReset(
                parseTime(prefs.getString(KEY_RESET_TIME, DEFAULT_RESET_TIME) ?: DEFAULT_RESET_TIME)
            )
        }
        val targetAppPackage = prefs.getString(KEY_TARGET_APP, null)
        val scheduleStart = parseTime(
            prefs.getString(KEY_SCHEDULE_START, DEFAULT_SCHEDULE_START) ?: DEFAULT_SCHEDULE_START
        )
        val scheduleEnd = parseTime(
            prefs.getString(KEY_SCHEDULE_END, DEFAULT_SCHEDULE_END) ?: DEFAULT_SCHEDULE_END
        )
        return Settings(
            enabled = enabled,
            cooldownConfig = cooldownConfig,
            targetAppPackage = targetAppPackage,
            scheduleStart = scheduleStart,
            scheduleEnd = scheduleEnd
        )
    }

    override suspend fun save(settings: Settings) {
        withContext(Dispatchers.IO) {
            prefs.edit()
                .putBoolean(KEY_ENABLED, settings.enabled)
                .putString(KEY_TARGET_APP, settings.targetAppPackage)
                .putString(KEY_SCHEDULE_START, formatTime(settings.scheduleStart))
                .putString(KEY_SCHEDULE_END, formatTime(settings.scheduleEnd))
                .commit()
            when (val config = settings.cooldownConfig) {
                is CooldownConfig.DailyReset -> {
                    prefs.edit()
                        .putString(KEY_COOLDOWN_TYPE, TYPE_DAILY_RESET)
                        .putString(KEY_RESET_TIME, formatTime(config.resetTime))
                        .commit()
                }
                is CooldownConfig.NHourInterval -> {
                    prefs.edit()
                        .putString(KEY_COOLDOWN_TYPE, TYPE_N_HOUR)
                        .putInt(KEY_INTERVAL_HOURS, config.intervalHours)
                        .commit()
                }
            }
        }
    }

    private fun formatTime(time: LocalTime): String =
        time.format(DateTimeFormatter.ISO_LOCAL_TIME)

    private fun parseTime(value: String): LocalTime =
        LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME)

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_COOLDOWN_TYPE = "cooldown_type"
        private const val KEY_RESET_TIME = "reset_time"
        private const val KEY_INTERVAL_HOURS = "interval_hours"
        private const val KEY_TARGET_APP = "target_app_package"
        private const val KEY_SCHEDULE_START = "schedule_start"
        private const val KEY_SCHEDULE_END = "schedule_end"

        private const val TYPE_DAILY_RESET = "daily_reset"
        private const val TYPE_N_HOUR = "n_hour"

        private const val DEFAULT_INTERVAL_HOURS = 4
        private const val DEFAULT_RESET_TIME = "00:00:00"
        private const val DEFAULT_SCHEDULE_START = "09:00:00"
        private const val DEFAULT_SCHEDULE_END = "17:00:00"
    }
}
