package com.example.promptly.domain.gate

import com.example.promptly.domain.model.EligibilityResult
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.service.CooldownEligibilityService
import java.time.ZoneId

sealed class GateDecision {
    data class Show(val pendingClaim: PendingClaim) : GateDecision()
    data object Skip : GateDecision()
}

data class PendingClaim(
    val triggerEpochMillis: Long,
    val isUnconfirmed: Boolean = true
)

class Gate(
    private val settingsRepository: SettingsRepository,
    private val cooldownRepository: CooldownRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    private val eligibilityService = CooldownEligibilityService(zone)

    private var lastForegroundPackage: String? = null

    suspend fun decide(foregroundPackage: String): GateDecision {
        if (foregroundPackage == lastForegroundPackage) return GateDecision.Skip
        lastForegroundPackage = foregroundPackage

        val settings = settingsRepository.load()
        val lastTrigger = cooldownRepository.loadLastTriggerEpochMillis()
        val result = eligibilityService.evaluate(
            enabled = settings.enabled,
            cooldownConfig = settings.cooldownConfig,
            scheduleStart = settings.scheduleStart,
            scheduleEnd = settings.scheduleEnd,
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = clock()
        )
        if (result != EligibilityResult.Eligible) return GateDecision.Skip

        return GateDecision.Show(PendingClaim(triggerEpochMillis = clock()))
    }
}
