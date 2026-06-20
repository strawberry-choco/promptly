package com.example.promptly.ui.intervention

import com.example.promptly.domain.model.InterventionConfig
import com.example.promptly.domain.model.RedirectDecision
import com.example.promptly.domain.usecase.AppRedirectUseCase
import com.example.promptly.domain.usecase.InterventionUseCase
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
class InterventionViewModelTest {

    private val useCase = mockk<InterventionUseCase>()
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
        val vm = InterventionViewModel(useCase, appRedirectUseCase)

        val state = vm.uiState.first()

        assertEquals(InterventionMode.Loading, state.mode)
    }

    @Test
    fun `onCreated transitions to Showing with Locked config`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated()

        val state = vm.uiState.first()
        assertEquals(InterventionMode.Showing, state.mode)
        assertTrue(state.config is InterventionConfig.Locked)
    }

    @Test
    fun `onCreated transitions to Showing with Fallback config`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Fallback
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated()

        val state = vm.uiState.first()
        assertEquals(InterventionMode.Showing, state.mode)
        assertTrue(state.config is InterventionConfig.Fallback)
    }

    @Test
    fun `onDismiss transitions to Dismissing`() = runTest(testDispatcher) {
        coEvery { useCase.dismiss() } returns Unit

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onDismiss()

        assertEquals(InterventionMode.Dismissing, vm.uiState.first().mode)
        coVerify { useCase.dismiss() }
    }

    @Test
    fun `onPause sets isCallInterrupted when showing`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated()
        vm.onPause()

        assertTrue(vm.uiState.first().isCallInterrupted)
    }

    @Test
    fun `onResumeAfterCall when interrupted calls onResurface`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget
        coEvery { useCase.onResurface() } returns InterventionConfig.Locked

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated()
        vm.onPause()
        vm.onResumeAfterCall()

        coVerify { useCase.onResurface() }
        assertFalse(vm.uiState.first().isCallInterrupted)
    }

    @Test
    fun `onResumeAfterCall when not interrupted does nothing`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated()
        vm.onResumeAfterCall()

        coVerify(exactly = 0) { useCase.onResurface() }
    }

    // Regression: Ready decision should transition to Redirecting mode
    @Test
    fun `onCreated with Ready decision transitions to Redirecting`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { useCase.dismiss() } returns Unit
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.Ready("com.example.test")

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals(InterventionMode.Redirecting("com.example.test"), state.mode)
    }

    // Regression: Uninstalled decision should set error message
    @Test
    fun `onCreated with Uninstalled decision shows error message with package name`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.Uninstalled("com.example.target")

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals("Target app 'com.example.target' not found.", state.errorMessage)
    }

    @Test
    fun `onAppLaunchFailed shows error with package name`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated(0L)
        vm.onAppLaunchFailed("com.example.broken")

        val state = vm.uiState.first()
        assertEquals("Failed to launch 'com.example.broken'.", state.errorMessage)
        assertEquals(InterventionMode.Showing, state.mode)
    }

    // BUG: NoTarget decision should provide user feedback, not stay silent
    @Test
    fun `onCreated with NoTarget decision shows helpful message`() = runTest(testDispatcher) {
        coEvery { useCase.prepare() } returns InterventionConfig.Locked
        coEvery { appRedirectUseCase.evaluate() } returns RedirectDecision.NoTarget

        val vm = InterventionViewModel(useCase, appRedirectUseCase)
        vm.onCreated(0L)

        val state = vm.uiState.first()
        assertEquals("No target app configured.", state.errorMessage)
    }
}
