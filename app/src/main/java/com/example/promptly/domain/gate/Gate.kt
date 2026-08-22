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
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val pendingClaimFreshnessMillis: Long = 10_000L
) {

    private val eligibilityService = CooldownEligibilityService(zone)

    private var lastForegroundPackage: String? = null

    private var pendingClaim: PendingClaim? = null

    suspend fun decide(foregroundPackage: String): GateDecision {
        val now = clock()
        pendingClaim?.let { pending ->
            if (now - pending.triggerEpochMillis < pendingClaimFreshnessMillis) {
                return GateDecision.Skip
            } else {
                pendingClaim = null
            }
        }
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
            nowEpochMillis = now
        )
        if (result != EligibilityResult.Eligible) return GateDecision.Skip

        val claim = PendingClaim(triggerEpochMillis = clock())
        pendingClaim = claim
        return GateDecision.Show(claim)
    }

    suspend fun confirm(claim: PendingClaim) {
        cooldownRepository.saveLastTriggerEpochMillis(claim.triggerEpochMillis)
    }}
