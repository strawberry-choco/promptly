package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.repository.TargetAppRepository

class AppRedirectUseCase(
    private val settingsRepository: SettingsRepository,
    private val targetAppRepository: TargetAppRepository
) {
    suspend fun evaluate(): RedirectDecision {
        val settings = settingsRepository.load()
        val packageName = settings.targetAppPackage ?: return RedirectDecision.NoTarget
        return if (targetAppRepository.isInstalled(packageName)) {
            RedirectDecision.Ready(packageName)
        } else {
            settingsRepository.save(settings.copy(targetAppPackage = null))
            RedirectDecision.Uninstalled(packageName)
        }
    }
}
