package com.example.promptly.ui.recovery

import com.example.promptly.domain.model.RecoveryRoutingDecision
import com.example.promptly.domain.usecase.ServiceLossRecoveryUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryViewModelTest {

    private val useCase = mockk<ServiceLossRecoveryUseCase>()
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
    fun `init state is checking`() = runTest(testDispatcher) {
        val vm = RecoveryViewModel(useCase)

        val state = vm.uiState.first()
        assertTrue(state.isChecking)
        assertFalse(state.serviceEnabled)
    }

    @Test
    fun `checkService when service enabled sets serviceEnabled true`() = runTest(testDispatcher) {
        coEvery { useCase.invoke() } returns RecoveryRoutingDecision.ProceedToSettings
        val vm = RecoveryViewModel(useCase)

        vm.checkService()

        val state = vm.uiState.first()
        assertFalse(state.isChecking)
        assertTrue(state.serviceEnabled)
    }

    @Test
    fun `checkService when service disabled sets serviceEnabled false`() = runTest(testDispatcher) {
        coEvery { useCase.invoke() } returns RecoveryRoutingDecision.ShowRecovery
        val vm = RecoveryViewModel(useCase)

        vm.checkService()

        val state = vm.uiState.first()
        assertFalse(state.isChecking)
        assertFalse(state.serviceEnabled)
    }

    @Test
    fun `onReEnable sets checking to false and serviceEnabled to false`() = runTest(testDispatcher) {
        coEvery { useCase.invoke() } returns RecoveryRoutingDecision.ShowRecovery
        val vm = RecoveryViewModel(useCase)

        vm.onReEnable()

        val state = vm.uiState.first()
        assertFalse(state.isChecking)
        assertFalse(state.serviceEnabled)
    }
}
