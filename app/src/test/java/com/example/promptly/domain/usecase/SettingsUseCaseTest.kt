package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

class SettingsUseCaseTest {

    private val settingsRepo = mockk<SettingsRepository>()
    private val cooldownRepo = mockk<CooldownRepository>()
    private lateinit var useCase: SettingsUseCase

    @BeforeEach
    fun setUp() {
        useCase = SettingsUseCase(settingsRepo, cooldownRepo)
    }

    @Test
    fun `load returns settings from repository`() = runTest {
        val expected = Settings(enabled = true)
        coEvery { settingsRepo.load() } returns expected

        val result = useCase.load()

        assertEquals(expected, result)
    }

    @Test
    fun `save persists and returns settings`() = runTest {
        val settings = Settings(enabled = true)
        coEvery { settingsRepo.save(settings) } returns Unit

        val result = useCase.save(settings)

        assertEquals(settings, result)
        coVerify { settingsRepo.save(settings) }
    }

    @Test
    fun `disable saves enabled false and clears cooldown`() = runTest {
        val current = Settings(enabled = true)
        coEvery { settingsRepo.load() } returns current
        coEvery { settingsRepo.save(any()) } returns Unit
        coEvery { cooldownRepo.clearLastTrigger() } returns Unit

        val result = useCase.disable()

        assertEquals(false, result.enabled)
        coVerify {
            settingsRepo.save(withArg { s -> assertEquals(false, s.enabled) })
            cooldownRepo.clearLastTrigger()
        }
    }

    @Test
    fun `changeCooldownConfig saves new config and clears cooldown`() = runTest {
        val current = Settings(cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT))
        val newConfig = CooldownConfig.NHourInterval(6)
        coEvery { settingsRepo.load() } returns current
        coEvery { settingsRepo.save(any()) } returns Unit
        coEvery { cooldownRepo.clearLastTrigger() } returns Unit

        val result = useCase.changeCooldownConfig(newConfig)

        assertEquals(newConfig, result.cooldownConfig)
        assertEquals(current.enabled, result.enabled)
        coVerify {
            settingsRepo.save(withArg { s -> assertEquals(newConfig, s.cooldownConfig) })
            cooldownRepo.clearLastTrigger()
        }
    }
}
