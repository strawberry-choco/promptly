package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.repository.OnboardingRepository
import com.example.promptly.domain.repository.ServiceStateRepository

class OnboardingUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val serviceStateRepository: ServiceStateRepository
) {
    suspend fun getState(): OnboardingState = onboardingRepository.load()

    suspend fun complete() {
        onboardingRepository.save(OnboardingState.Completed)
    }

    suspend fun skip() {
        onboardingRepository.save(OnboardingState.Skipped)
    }

    suspend fun checkServiceEnabled(): Boolean = serviceStateRepository.isEnabled()
}
