package com.example.promptly.domain.model

import java.time.LocalTime

sealed interface CooldownConfig {
    data class DailyReset(val resetTime: LocalTime) : CooldownConfig

    data class NHourInterval(val intervalHours: Int) : CooldownConfig {
        init {
            require(intervalHours in 1..24) {
                "Interval hours must be between 1 and 24, got $intervalHours"
            }
        }
    }
}
