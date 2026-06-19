package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.EligibilityResult
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.service.CooldownEligibilityService

class CheckCooldownUseCase(
    private val settingsRepository: SettingsRepository,
    private val cooldownRepository: CooldownRepository,
    private val eligibilityService: CooldownEligibilityService
) {
    suspend operator fun invoke(): EligibilityResult {
        val settings = settingsRepository.load()
        val lastTrigger = cooldownRepository.loadLastTriggerEpochMillis()
        return eligibilityService.evaluate(
            enabled = settings.enabled,
            cooldownConfig = settings.cooldownConfig,
            scheduleStart = settings.scheduleStart,
            scheduleEnd = settings.scheduleEnd,
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = System.currentTimeMillis()
        )
    }
}
