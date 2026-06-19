package com.example.promptly.domain.usecase

import com.example.promptly.domain.repository.CooldownRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RecordCooldownTriggerUseCaseTest {

    private val cooldownRepository = mockk<CooldownRepository>(relaxed = true)
    private val useCase = RecordCooldownTriggerUseCase(cooldownRepository)

    @Test
    fun `saves current timestamp when invoked`() = runTest {
        useCase.invoke()

        coVerify { cooldownRepository.saveLastTriggerEpochMillis(any()) }
    }
}
