package com.example.promptly.domain.repository

import com.example.promptly.domain.model.OnboardingState

interface OnboardingRepository {
    suspend fun load(): OnboardingState
    suspend fun save(state: OnboardingState)
}
