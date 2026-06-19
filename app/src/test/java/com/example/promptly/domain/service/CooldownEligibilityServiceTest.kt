package com.example.promptly.domain.service

import com.example.promptly.domain.model.BlockReason
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.EligibilityResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class CooldownEligibilityServiceTest {

    private val zone = ZoneOffset.UTC
    private val service = CooldownEligibilityService(zone)

    private fun epochMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `disabled feature returns FEATURE_DISABLED`() {
        val result = service.evaluate(
            enabled = false,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.FEATURE_DISABLED), result)
    }

    @Test
    fun `disabled feature overrides all other settings`() {
        val result = service.evaluate(
            enabled = false,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = 0L,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.FEATURE_DISABLED), result)
    }

    @Test
    fun `outside normal schedule window returns OUTSIDE_SCHEDULE_WINDOW`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(9, 0),
            scheduleEnd = LocalTime.of(17, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(8, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.OUTSIDE_SCHEDULE_WINDOW), result)
    }

    @Test
    fun `within normal schedule window proceeds to cooldown check`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(9, 0),
            scheduleEnd = LocalTime.of(17, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `overnight schedule active after start returns Eligible`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(22, 0),
            scheduleEnd = LocalTime.of(6, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(23, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `overnight schedule active before end returns Eligible`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(22, 0),
            scheduleEnd = LocalTime.of(6, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 20), LocalTime.of(5, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `overnight schedule outside window returns OUTSIDE_SCHEDULE_WINDOW`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(22, 0),
            scheduleEnd = LocalTime.of(6, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(12, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.OUTSIDE_SCHEDULE_WINDOW), result)
    }

    @Test
    fun `schedule window boundary at start is inside`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(9, 0),
            scheduleEnd = LocalTime.of(17, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(9, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `schedule window boundary at end is inside`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(9, 0),
            scheduleEnd = LocalTime.of(17, 0),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(17, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `daily reset with no last trigger returns Eligible`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(0, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `daily reset blocks when last trigger is after reset`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(8, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(0, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE), result)
    }

    @Test
    fun `daily reset allows when last trigger is before reset`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 18), LocalTime.of(22, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(0, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `daily reset with non-midnight reset time blocks when trigger after today reset`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(7, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(6, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE), result)
    }

    @Test
    fun `daily reset with non-midnight reset allows when reset occurred since last trigger`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(5, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(6, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `daily reset before today reset uses yesterday as most recent`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(3, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 18), LocalTime.of(20, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(6, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE), result)
    }

    @Test
    fun `daily reset before today reset allows when trigger before yesterday reset`() {
        val now = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(3, 0))
        val lastTrigger = epochMillis(LocalDate.of(2026, 6, 18), LocalTime.of(4, 0))
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(6, 0)),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `N-hour interval with no last trigger returns Eligible`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(4),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = null,
            nowEpochMillis = 1000L
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `N-hour interval blocks when interval not elapsed`() {
        val now = 10_000L
        val lastTrigger = 5_000L
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(1),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE), result)
    }

    @Test
    fun `N-hour interval allows when interval has elapsed`() {
        val lastTrigger = 0L
        val now = 3_600_000L * 2
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(2),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `N-hour interval boundary at exact expiry is Eligible`() {
        val lastTrigger = 0L
        val now = 3_600_000L * 4
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(4),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = lastTrigger,
            nowEpochMillis = now
        )
        assertEquals(EligibilityResult.Eligible, result)
    }

    @Test
    fun `evaluation order disabled overrides schedule window`() {
        val result = service.evaluate(
            enabled = false,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(0, 0),
            scheduleEnd = LocalTime.of(23, 59),
            lastTriggerEpochMillis = null,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(10, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.FEATURE_DISABLED), result)
    }

    @Test
    fun `evaluation order schedule window checked before cooldown`() {
        val result = service.evaluate(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT),
            scheduleStart = LocalTime.of(9, 0),
            scheduleEnd = LocalTime.of(17, 0),
            lastTriggerEpochMillis = 0L,
            nowEpochMillis = epochMillis(LocalDate.of(2026, 6, 19), LocalTime.of(8, 0))
        )
        assertEquals(EligibilityResult.Blocked(BlockReason.OUTSIDE_SCHEDULE_WINDOW), result)
    }
}
