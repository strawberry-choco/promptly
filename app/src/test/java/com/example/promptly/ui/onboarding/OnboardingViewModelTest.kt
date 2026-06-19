package com.example.promptly.ui.onboarding

import com.example.promptly.domain.usecase.OnboardingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val useCase = mockk<OnboardingUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init state is loading`() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(useCase)

        val state = vm.uiState.first()
        assertTrue(state.isLoading)
        assertFalse(state.showSuccess)
    }

    @Test
    fun `onResume when service enabled sets showSuccess true`() = runTest(testDispatcher) {
        coEvery { useCase.checkServiceEnabled() } returns true
        val vm = OnboardingViewModel(useCase)

        vm.onResume()

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertTrue(state.showSuccess)
    }

    @Test
    fun `onResume when service not enabled sets showSuccess false`() = runTest(testDispatcher) {
        coEvery { useCase.checkServiceEnabled() } returns false
        val vm = OnboardingViewModel(useCase)

        vm.onResume()

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertFalse(state.showSuccess)
    }

    @Test
    fun `onSkip saves state via use case`() = runTest(testDispatcher) {
        coEvery { useCase.skip() } returns Unit
        val vm = OnboardingViewModel(useCase)

        vm.onSkip()

        coVerify { useCase.skip() }
    }

    @Test
    fun `onContinue saves Completed state`() = runTest(testDispatcher) {
        coEvery { useCase.complete() } returns Unit
        val vm = OnboardingViewModel(useCase)

        vm.onContinue()

        coVerify { useCase.complete() }
    }

    @Test
    fun `onResume after navigation does not check service`() = runTest(testDispatcher) {
        coEvery { useCase.skip() } returns Unit
        val vm = OnboardingViewModel(useCase)

        vm.onSkip()
        vm.onResume()

        coVerify(exactly = 0) { useCase.checkServiceEnabled() }
    }
}
