package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.repository.CooldownRepository

class CooldownRepositoryImpl(
    private val prefs: SharedPreferences
) : CooldownRepository {

    override suspend fun clearLastTrigger() {
        prefs.edit().remove(KEY_LAST_TRIGGER).apply()
    }

    companion object {
        private const val KEY_LAST_TRIGGER = "last_trigger_timestamp"
    }
}
