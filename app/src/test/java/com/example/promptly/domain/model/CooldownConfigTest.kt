package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalTime

class CooldownConfigTest {

    @Test
    fun `DailyReset stores resetTime`() {
        val time = LocalTime.of(6, 30)
        val config: CooldownConfig = CooldownConfig.DailyReset(time)
        assertEquals(time, (config as CooldownConfig.DailyReset).resetTime)
    }

    @Test
    fun `NHourInterval stores intervalHours`() {
        val config: CooldownConfig = CooldownConfig.NHourInterval(8)
        assertEquals(8, (config as CooldownConfig.NHourInterval).intervalHours)
    }

    @Test
    fun `NHourInterval rejects hours below 1`() {
        assertThrows<IllegalArgumentException> {
            CooldownConfig.NHourInterval(0)
        }
    }

    @Test
    fun `NHourInterval rejects hours above 24`() {
        assertThrows<IllegalArgumentException> {
            CooldownConfig.NHourInterval(25)
        }
    }

    @Test
    fun `NHourInterval accepts boundary values`() {
        CooldownConfig.NHourInterval(1)
        CooldownConfig.NHourInterval(24)
    }

    @Test
    fun `DailyReset and NHourInterval are different types`() {
        val daily: CooldownConfig = CooldownConfig.DailyReset(LocalTime.MIDNIGHT)
        val interval: CooldownConfig = CooldownConfig.NHourInterval(4)
        assertTrue(daily !is CooldownConfig.NHourInterval)
        assertTrue(interval !is CooldownConfig.DailyReset)
    }
}
