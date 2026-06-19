package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.repository.OnboardingRepository
import com.example.promptly.domain.repository.ServiceStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OnboardingUseCaseTest {

    private val onboardingRepo = mockk<OnboardingRepository>()
    private val serviceStateRepo = mockk<ServiceStateRepository>()
    private lateinit var useCase: OnboardingUseCase

    @BeforeEach
    fun setUp() {
        useCase = OnboardingUseCase(onboardingRepo, serviceStateRepo)
    }

    @Test
    fun `getState returns New from repository`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.New

        val result = useCase.getState()

        assertTrue(result is OnboardingState.New)
    }

    @Test
    fun `getState returns Completed from repository`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Completed

        val result = useCase.getState()

        assertTrue(result is OnboardingState.Completed)
    }

    @Test
    fun `getState returns Skipped from repository`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Skipped

        val result = useCase.getState()

        assertTrue(result is OnboardingState.Skipped)
    }

    @Test
    fun `complete saves Completed state`() = runTest {
        coEvery { onboardingRepo.save(any()) } returns Unit

        useCase.complete()

        coVerify { onboardingRepo.save(OnboardingState.Completed) }
    }

    @Test
    fun `skip saves Skipped state`() = runTest {
        coEvery { onboardingRepo.save(any()) } returns Unit

        useCase.skip()

        coVerify { onboardingRepo.save(OnboardingState.Skipped) }
    }

    @Test
    fun `checkServiceEnabled returns true when service is enabled`() = runTest {
        coEvery { serviceStateRepo.isEnabled() } returns true

        val result = useCase.checkServiceEnabled()

        assertEquals(true, result)
    }

    @Test
    fun `checkServiceEnabled returns false when service is not enabled`() = runTest {
        coEvery { serviceStateRepo.isEnabled() } returns false

        val result = useCase.checkServiceEnabled()

        assertEquals(false, result)
    }
}
