package com.example.promptly.domain.model

sealed class OnboardingState {
    data object New : OnboardingState()
    data object Completed : OnboardingState()
    data object Skipped : OnboardingState()
}
