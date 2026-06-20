package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.repository.TargetAppRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppRedirectUseCaseTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val targetAppRepository = mockk<TargetAppRepository>()
    private val useCase = AppRedirectUseCase(settingsRepository, targetAppRepository)

    @Test
    fun `returns NoTarget when no target app configured`() = runTest {
        coEvery { settingsRepository.load() } returns Settings()

        val result = useCase.evaluate()

        assertEquals(RedirectDecision.NoTarget, result)
    }

    @Test
    fun `returns Ready when target app is installed`() = runTest {
        coEvery { settingsRepository.load() } returns Settings(targetAppPackage = "com.example.anki")
        coEvery { targetAppRepository.isInstalled("com.example.anki") } returns true

        val result = useCase.evaluate()

        assertEquals(RedirectDecision.Ready("com.example.anki"), result)
    }

    @Test
    fun `returns Uninstalled with packageName and clears target when target app not installed`() = runTest {
        coEvery { settingsRepository.load() } returns Settings(targetAppPackage = "com.example.anki")
        coEvery { targetAppRepository.isInstalled("com.example.anki") } returns false
        coEvery { settingsRepository.save(any()) } returns Unit

        val result = useCase.evaluate()

        assertEquals(RedirectDecision.Uninstalled("com.example.anki"), result)
        coVerify { settingsRepository.save(match { it.targetAppPackage == null }) }
    }
}
