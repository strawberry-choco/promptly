package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.BlockReason
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.EligibilityResult
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.service.CooldownEligibilityService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class CheckCooldownUseCaseTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val cooldownRepository = mockk<CooldownRepository>()
    private val eligibilityService = mockk<CooldownEligibilityService>()
    private val useCase = CheckCooldownUseCase(
        settingsRepository,
        cooldownRepository,
        eligibilityService
    )

    @Test
    fun `returns Eligible when service returns Eligible`() = runTest {
        coEvery { settingsRepository.load() } returns Settings()
        coEvery { cooldownRepository.loadLastTriggerEpochMillis() } returns null
        every { eligibilityService.evaluate(any(), any(), any(), any(), any(), any()) } returns EligibilityResult.Eligible

        val result = useCase.invoke()

        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `returns Blocked when service returns Blocked`() = runTest {
        coEvery { settingsRepository.load() } returns Settings()
        coEvery { cooldownRepository.loadLastTriggerEpochMillis() } returns 12345L
        every { eligibilityService.evaluate(any(), any(), any(), any(), any(), any()) } returns EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE)

        val result = useCase.invoke()

        assertEquals(EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE), result)
    }

    @Test
    fun `loads settings and lastTrigger before evaluating`() = runTest {
        val settings = Settings(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(6, 0))
        )
        val lastTrigger = 99999L
        coEvery { settingsRepository.load() } returns settings
        coEvery { cooldownRepository.loadLastTriggerEpochMillis() } returns lastTrigger
        every { eligibilityService.evaluate(any(), any(), any(), any(), any(), any()) } returns EligibilityResult.Eligible

        useCase.invoke()

        verify {
            eligibilityService.evaluate(
                settings.enabled,
                settings.cooldownConfig,
                settings.scheduleStart,
                settings.scheduleEnd,
                lastTrigger,
                any()
            )
        }
    }
}
