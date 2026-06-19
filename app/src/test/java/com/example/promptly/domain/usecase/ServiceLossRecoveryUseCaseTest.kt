package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.model.RecoveryRoutingDecision
import com.example.promptly.domain.repository.OnboardingRepository
import com.example.promptly.domain.repository.ServiceStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ServiceLossRecoveryUseCaseTest {

    private val onboardingRepo = mockk<OnboardingRepository>()
    private val serviceStateRepo = mockk<ServiceStateRepository>()
    private lateinit var useCase: ServiceLossRecoveryUseCase

    @BeforeEach
    fun setUp() {
        useCase = ServiceLossRecoveryUseCase(onboardingRepo, serviceStateRepo)
    }

    @Test
    fun `returns ProceedToOnboarding when user is New`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.New

        val result = useCase()

        assertTrue(result is RecoveryRoutingDecision.ProceedToOnboarding)
        coVerify(exactly = 0) { serviceStateRepo.isEnabled() }
    }

    @Test
    fun `returns ProceedToSettings when Completed and service enabled`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Completed
        coEvery { serviceStateRepo.isEnabled() } returns true

        val result = useCase()

        assertTrue(result is RecoveryRoutingDecision.ProceedToSettings)
    }

    @Test
    fun `returns ShowRecovery when Completed and service disabled`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Completed
        coEvery { serviceStateRepo.isEnabled() } returns false

        val result = useCase()

        assertTrue(result is RecoveryRoutingDecision.ShowRecovery)
    }

    @Test
    fun `returns ShowRecovery when Skipped and service disabled`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Skipped
        coEvery { serviceStateRepo.isEnabled() } returns false

        val result = useCase()

        assertTrue(result is RecoveryRoutingDecision.ShowRecovery)
    }

    @Test
    fun `loads onboarding state before checking service`() = runTest {
        coEvery { onboardingRepo.load() } returns OnboardingState.Completed
        coEvery { serviceStateRepo.isEnabled() } returns true

        useCase()

        coVerify { onboardingRepo.load() }
        coVerify { serviceStateRepo.isEnabled() }
    }
}
