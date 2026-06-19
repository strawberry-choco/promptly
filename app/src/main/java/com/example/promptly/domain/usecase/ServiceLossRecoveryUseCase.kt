package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.model.RecoveryRoutingDecision
import com.example.promptly.domain.repository.OnboardingRepository
import com.example.promptly.domain.repository.ServiceStateRepository

class ServiceLossRecoveryUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val serviceStateRepository: ServiceStateRepository
) {
    suspend operator fun invoke(): RecoveryRoutingDecision {
        val onboardingState = onboardingRepository.load()
        return when (onboardingState) {
            OnboardingState.New -> RecoveryRoutingDecision.ProceedToOnboarding
            OnboardingState.Completed, OnboardingState.Skipped -> {
                if (serviceStateRepository.isEnabled()) {
                    RecoveryRoutingDecision.ProceedToSettings
                } else {
                    RecoveryRoutingDecision.ShowRecovery
                }
            }
        }
    }
}
