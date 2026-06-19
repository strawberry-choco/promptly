package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository

class SettingsUseCase(
    private val settingsRepository: SettingsRepository,
    private val cooldownRepository: CooldownRepository
) {
    suspend fun load(): Settings {
        return settingsRepository.load()
    }

    suspend fun save(settings: Settings): Settings {
        settingsRepository.save(settings)
        return settings
    }

    suspend fun disable(): Settings {
        val settings = load().copy(enabled = false)
        settingsRepository.save(settings)
        cooldownRepository.clearLastTrigger()
        return settings
    }

    suspend fun changeCooldownConfig(config: CooldownConfig): Settings {
        val settings = load().copy(cooldownConfig = config)
        settingsRepository.save(settings)
        cooldownRepository.clearLastTrigger()
        return settings
    }
}
