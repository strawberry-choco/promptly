package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.model.OnboardingState
import com.example.promptly.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(
    private val prefs: SharedPreferences
) : OnboardingRepository {

    override suspend fun load(): OnboardingState {
        val raw = prefs.getString(KEY_ONBOARDING_STATE, null)
        return when (raw) {
            VALUE_COMPLETED -> OnboardingState.Completed
            VALUE_SKIPPED -> OnboardingState.Skipped
            else -> OnboardingState.New
        }
    }

    override suspend fun save(state: OnboardingState) {
        val value = when (state) {
            OnboardingState.Completed -> VALUE_COMPLETED
            OnboardingState.Skipped -> VALUE_SKIPPED
            OnboardingState.New -> return
        }
        prefs.edit().putString(KEY_ONBOARDING_STATE, value).apply()
    }

    companion object {
        private const val KEY_ONBOARDING_STATE = "onboarding_state"
        private const val VALUE_COMPLETED = "completed"
        private const val VALUE_SKIPPED = "skipped"
    }
}
