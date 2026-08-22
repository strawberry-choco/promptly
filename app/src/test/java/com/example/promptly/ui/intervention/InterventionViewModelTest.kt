package com.example.promptly.ui.intervention

import com.example.promptly.domain.gate.Gate
import com.example.promptly.domain.gate.PendingClaim
import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.usecase.AppRedirectUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterventionViewModelTest {

    private val gate = mockk<Gate>(relaxed = true)
    private val appRedirectUseCase = mockk<AppRedirectUseCase>()
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
    fun `init state is Loading`() = runTest(testDispatcher) {
        val vm = InterventionViewModel(appRedirectUseCase, gate)

        val state = vm.uiState.first()

        assertEquals(InterventionMode.Loading, state.mode)
    }

    @Test
    fun `onCreated transitions to Showing`() = runTest(testDispatcher) {
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onCreated()

        val state = vm.uiState.first()
        assertEquals(InterventionMode.Showing, state.mode)
    }

    @Test
    fun `onDismiss transitions to Dismissing`() = runTest(testDispatcher) {
        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onDismiss()

        assertEquals(InterventionMode.Dismissing, vm.uiState.first().mode)
    }

    @Test
    fun `onCreated with Ready decision transitions to Redirecting`() = runTest(testDispatcher) {
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.Ready("com.example.test")

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals(InterventionMode.Redirecting("com.example.test"), state.mode)
    }

    @Test
    fun `onCreated with Uninstalled decision shows error message with package name`() = runTest(testDispatcher) {
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.Uninstalled("com.example.target")

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals("Target app 'com.example.target' not found.", state.errorMessage)
    }

    @Test
    fun `onAppLaunchFailed shows error with package name`() = runTest(testDispatcher) {
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onCreated(0L)
        vm.onAppLaunchFailed("com.example.broken")

        val state = vm.uiState.first()
        assertEquals("Failed to launch 'com.example.broken'.", state.errorMessage)
        assertEquals(InterventionMode.Showing, state.mode)
    }

    @Test
    fun `onCreated with NoTarget decision shows helpful message`() = runTest(testDispatcher) {
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals("No target app configured.", state.errorMessage)
    }

    @Test
    fun `onShown hands the pending claim through to the gate confirm`() = runTest(testDispatcher) {
        val claim = PendingClaim(triggerEpochMillis = 1_000L)

        val vm = InterventionViewModel(appRedirectUseCase, gate)
        vm.onShown(claim)

        coVerify { gate.confirm(claim) }
    }}
