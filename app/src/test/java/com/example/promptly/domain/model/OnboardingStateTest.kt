package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingStateTest {

    @Test
    fun `New is default state`() {
        val state: OnboardingState = OnboardingState.New
        assertTrue(state is OnboardingState.New)
    }

    @Test
    fun `Completed is a distinct state`() {
        assertTrue(OnboardingState.Completed is OnboardingState.Completed)
        assertTrue(OnboardingState.Completed !is OnboardingState.New)
        assertTrue(OnboardingState.Completed !is OnboardingState.Skipped)
    }

    @Test
    fun `Skipped is a distinct state`() {
        assertTrue(OnboardingState.Skipped is OnboardingState.Skipped)
        assertTrue(OnboardingState.Skipped !is OnboardingState.New)
        assertTrue(OnboardingState.Skipped !is OnboardingState.Completed)
    }

    @Test
    fun `all three states are exhaustive`() {
        val states = listOf<OnboardingState>(
            OnboardingState.New,
            OnboardingState.Completed,
            OnboardingState.Skipped
        )
        val mapped = states.map { state ->
            when (state) {
                OnboardingState.New -> "new"
                OnboardingState.Completed -> "completed"
                OnboardingState.Skipped -> "skipped"
            }
        }
        assertTrue(mapped.containsAll(listOf("new", "completed", "skipped")))
    }
}
