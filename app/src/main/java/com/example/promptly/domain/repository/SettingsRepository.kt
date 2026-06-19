package com.example.promptly.domain.repository

import com.example.promptly.domain.model.Settings

interface SettingsRepository {
    suspend fun load(): Settings
    suspend fun save(settings: Settings)
}
