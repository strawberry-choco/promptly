package com.example.promptly.ui.settings

import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.PackageName
import com.example.promptly.domain.model.Settings
import java.time.LocalTime

sealed interface SettingsEvent {
    data object LaunchIntervention : SettingsEvent
}

data class SettingsUiState(
    val enabled: Boolean = false,
    val cooldownConfig: CooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
    val targetAppPackage: PackageName? = null,
    val scheduleStart: LocalTime = LocalTime.of(9, 0),
    val scheduleEnd: LocalTime = LocalTime.of(17, 0),
    val serviceEnabled: Boolean = false,
    val showBanner: Boolean = false
)

fun Settings.toUiState(serviceEnabled: Boolean = false): SettingsUiState = SettingsUiState(
    enabled = enabled,
    cooldownConfig = cooldownConfig,
    targetAppPackage = targetAppPackage,
    scheduleStart = scheduleStart,
    scheduleEnd = scheduleEnd,
    serviceEnabled = serviceEnabled,
    showBanner = !serviceEnabled
)
