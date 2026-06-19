package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.repository.CooldownRepository

class CooldownRepositoryImpl(
    private val prefs: SharedPreferences
) : CooldownRepository {

    override suspend fun loadLastTriggerEpochMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_TRIGGER, MISSING_VALUE)
        return if (value == MISSING_VALUE) null else value
    }

    override suspend fun saveLastTriggerEpochMillis(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_TRIGGER, timestamp).apply()
    }

    override suspend fun clearLastTrigger() {
        prefs.edit().remove(KEY_LAST_TRIGGER).apply()
    }

    companion object {
        private const val KEY_LAST_TRIGGER = "last_trigger_timestamp"
        private const val MISSING_VALUE = -1L
    }
}
