package com.example.promptly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.PackageName
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.usecase.OnboardingUseCase
import com.example.promptly.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val onboardingUseCase: OnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsUseCase.load()
            val serviceEnabled = onboardingUseCase.checkServiceEnabled()
            _uiState.value = settings.toUiState(serviceEnabled)
        }
    }

    fun onResume() {
        viewModelScope.launch {
            val enabled = onboardingUseCase.checkServiceEnabled()
            _uiState.value = _uiState.value.copy(
                serviceEnabled = enabled,
                showBanner = !enabled
            )
        }
    }

    fun onLaunchIntervention() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.LaunchIntervention)
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val settings = Settings(
                enabled = enabled,
                cooldownConfig = current.cooldownConfig,
                targetAppPackage = current.targetAppPackage,
                scheduleStart = current.scheduleStart,
                scheduleEnd = current.scheduleEnd
            )
            _uiState.value = if (enabled) {
                settingsUseCase.save(settings).toUiState(current.serviceEnabled)
            } else {
                settingsUseCase.disable().toUiState(current.serviceEnabled)
            }
        }
    }

    fun onTargetAppChanged(packageName: PackageName?) {
        viewModelScope.launch {
            val current = _uiState.value
            val settings = Settings(
                enabled = current.enabled,
                cooldownConfig = current.cooldownConfig,
                targetAppPackage = packageName,
                scheduleStart = current.scheduleStart,
                scheduleEnd = current.scheduleEnd
            )
            _uiState.value = settingsUseCase.save(settings).toUiState(current.serviceEnabled)
        }
    }

    fun onCooldownTypeChanged(config: CooldownConfig) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = settingsUseCase.changeCooldownConfig(config)
                .toUiState(current.serviceEnabled)
        }
    }

    fun onScheduleStartChanged(time: LocalTime) {
        viewModelScope.launch {
            val current = _uiState.value
            val settings = Settings(
                enabled = current.enabled,
                cooldownConfig = current.cooldownConfig,
                targetAppPackage = current.targetAppPackage,
                scheduleStart = time,
                scheduleEnd = current.scheduleEnd
            )
            _uiState.value = settingsUseCase.save(settings).toUiState(current.serviceEnabled)
        }
    }

    fun onScheduleEndChanged(time: LocalTime) {
        viewModelScope.launch {
            val current = _uiState.value
            val settings = Settings(
                enabled = current.enabled,
                cooldownConfig = current.cooldownConfig,
                targetAppPackage = current.targetAppPackage,
                scheduleStart = current.scheduleStart,
                scheduleEnd = time
            )
            _uiState.value = settingsUseCase.save(settings).toUiState(current.serviceEnabled)
        }
    }
}
