package com.example.promptly.domain.model

import java.time.LocalTime

data class Settings(
    val enabled: Boolean = false,
    val cooldownConfig: CooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
    val targetAppPackage: String? = null,
    val scheduleStart: LocalTime = LocalTime.of(9, 0),
    val scheduleEnd: LocalTime = LocalTime.of(17, 0)
)
