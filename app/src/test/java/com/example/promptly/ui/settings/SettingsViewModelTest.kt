package com.example.promptly.ui.settings

import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.PackageName
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.usecase.OnboardingUseCase
import com.example.promptly.domain.usecase.SettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsUseCase = mockk<SettingsUseCase>()
    private val onboardingUseCase = mockk<OnboardingUseCase>()
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
    fun `init loads settings and checks service state`() = runTest(testDispatcher) {
        val settings = Settings(enabled = true)
        coEvery { settingsUseCase.load() } returns settings
        coEvery { onboardingUseCase.checkServiceEnabled() } returns false

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)

        val state = vm.uiState.first()
        assertEquals(true, state.enabled)
        assertFalse(state.showBanner)
    }

    @Test
    fun `init shows banner when service is not enabled`() = runTest(testDispatcher) {
        val settings = Settings()
        coEvery { settingsUseCase.load() } returns settings
        coEvery { onboardingUseCase.checkServiceEnabled() } returns false

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)

        assertTrue(vm.uiState.first().showBanner)
    }

    @Test
    fun `onResume updates service status`() = runTest(testDispatcher) {
        val settings = Settings()
        coEvery { settingsUseCase.load() } returns settings
        coEvery { onboardingUseCase.checkServiceEnabled() } returns false

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)

        coEvery { onboardingUseCase.checkServiceEnabled() } returns true

        vm.onResume()

        assertFalse(vm.uiState.first().showBanner)
    }

    @Test
    fun `onEnabledChanged to false calls disable`() = runTest(testDispatcher) {
        val initial = Settings(enabled = true)
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.disable() } returns initial.copy(enabled = false)

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onEnabledChanged(false)

        coVerify { settingsUseCase.disable() }
        assertEquals(false, vm.uiState.first().enabled)
    }

    @Test
    fun `onEnabledChanged to true calls save`() = runTest(testDispatcher) {
        val initial = Settings(enabled = false)
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.save(any()) } returns initial.copy(enabled = true)

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onEnabledChanged(true)

        coVerify { settingsUseCase.save(any()) }
        assertEquals(true, vm.uiState.first().enabled)
    }

    @Test
    fun `onTargetAppChanged with null clears target app`() = runTest(testDispatcher) {
        val initial = Settings(targetAppPackage = PackageName.fromRaw("com.ichi2.anki").getOrThrow())
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.save(any()) } answers { firstArg() }

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onTargetAppChanged(null)

        coVerify { settingsUseCase.save(withArg { s -> assertNull(s.targetAppPackage) }) }
        assertNull(vm.uiState.first().targetAppPackage)
    }

    @Test
    fun `onTargetAppChanged with package saves it`() = runTest(testDispatcher) {
        val initial = Settings()
        val pkg = PackageName.fromRaw("com.ichi2.anki").getOrThrow()
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.save(any()) } answers { firstArg() }

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onTargetAppChanged(pkg)

        coVerify { settingsUseCase.save(withArg { s -> assertEquals(pkg, s.targetAppPackage) }) }
        assertEquals(pkg, vm.uiState.first().targetAppPackage)
    }

    @Test
    fun `onCooldownTypeChanged calls changeCooldownConfig`() = runTest(testDispatcher) {
        val initial = Settings()
        val newConfig = CooldownConfig.NHourInterval(8)
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.changeCooldownConfig(any()) } returns initial.copy(cooldownConfig = newConfig)

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onCooldownTypeChanged(newConfig)

        coVerify { settingsUseCase.changeCooldownConfig(newConfig) }
        assertEquals(newConfig, vm.uiState.first().cooldownConfig)
    }

    @Test
    fun `onScheduleStartChanged calls save with new time`() = runTest(testDispatcher) {
        val initial = Settings()
        val newTime = LocalTime.of(10, 0)
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.save(any()) } answers { firstArg() }

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onScheduleStartChanged(newTime)

        assertEquals(newTime, vm.uiState.first().scheduleStart)
    }

    @Test
    fun `onScheduleEndChanged calls save with new time`() = runTest(testDispatcher) {
        val initial = Settings()
        val newTime = LocalTime.of(20, 0)
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true
        coEvery { settingsUseCase.save(any()) } answers { firstArg() }

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        vm.onScheduleEndChanged(newTime)

        assertEquals(newTime, vm.uiState.first().scheduleEnd)
    }

    @Test
    fun `onLaunchIntervention emits LaunchIntervention event`() = runTest(testDispatcher) {
        val initial = Settings()
        coEvery { settingsUseCase.load() } returns initial
        coEvery { onboardingUseCase.checkServiceEnabled() } returns true

        val vm = SettingsViewModel(settingsUseCase, onboardingUseCase)
        val events = mutableListOf<SettingsEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.onLaunchIntervention()

        assertEquals(listOf(SettingsEvent.LaunchIntervention), events)
        job.cancel()
    }
}
