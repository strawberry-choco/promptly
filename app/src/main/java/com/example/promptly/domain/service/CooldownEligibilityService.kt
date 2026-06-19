package com.example.promptly.domain.service

import com.example.promptly.domain.model.BlockReason
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.EligibilityResult
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class CooldownEligibilityService(
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    fun evaluate(
        enabled: Boolean,
        cooldownConfig: CooldownConfig,
        scheduleStart: LocalTime,
        scheduleEnd: LocalTime,
        lastTriggerEpochMillis: Long?,
        nowEpochMillis: Long
    ): EligibilityResult {
        if (!enabled) return EligibilityResult.Blocked(BlockReason.FEATURE_DISABLED)

        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
        val nowTime = now.toLocalTime()

        val inSchedule = if (scheduleStart <= scheduleEnd) {
            !nowTime.isBefore(scheduleStart) && !nowTime.isAfter(scheduleEnd)
        } else {
            !nowTime.isBefore(scheduleStart) || !nowTime.isAfter(scheduleEnd)
        }
        if (!inSchedule) return EligibilityResult.Blocked(BlockReason.OUTSIDE_SCHEDULE_WINDOW)

        val lastTrigger = lastTriggerEpochMillis ?: return EligibilityResult.Eligible

        return when (cooldownConfig) {
            is CooldownConfig.DailyReset -> {
                val today = now.toLocalDate()
                val todayReset = today.atTime(cooldownConfig.resetTime)
                    .atZone(zone).toInstant().toEpochMilli()
                val mostRecentReset = if (nowEpochMillis < todayReset) {
                    today.minusDays(1).atTime(cooldownConfig.resetTime)
                        .atZone(zone).toInstant().toEpochMilli()
                } else {
                    todayReset
                }
                if (lastTrigger < mostRecentReset) EligibilityResult.Eligible
                else EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE)
            }
            is CooldownConfig.NHourInterval -> {
                val intervalMs = cooldownConfig.intervalHours * 3_600_000L
                if (lastTrigger + intervalMs <= nowEpochMillis) EligibilityResult.Eligible
                else EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE)
            }
        }
    }
}
