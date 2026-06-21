package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

class SettingsTest {

    @Test
    fun `default settings has expected values`() {
        val settings = Settings()
        assertEquals(false, settings.enabled)
        assertEquals(CooldownConfig.DailyReset(LocalTime.MIDNIGHT), settings.cooldownConfig)
        assertNull(settings.targetAppPackage)
        assertEquals(LocalTime.of(9, 0), settings.scheduleStart)
        assertEquals(LocalTime.of(17, 0), settings.scheduleEnd)
    }

    @Test
    fun `copy with enabled change`() {
        val settings = Settings()
        val updated = settings.copy(enabled = true)
        assertEquals(true, updated.enabled)
        assertEquals(settings.targetAppPackage, updated.targetAppPackage)
    }

    @Test
    fun `copy with targetAppPackage`() {
        val settings = Settings()
        val pkg = PackageName.fromRaw("com.example.app").getOrThrow()
        val updated = settings.copy(targetAppPackage = pkg)
        assertEquals(pkg, updated.targetAppPackage)
    }

    @Test
    fun `copy with cooldownConfig change`() {
        val settings = Settings()
        val interval = CooldownConfig.NHourInterval(8)
        val updated = settings.copy(cooldownConfig = interval)
        assertEquals(interval, updated.cooldownConfig)
    }

    @Test
    fun `copy preserves other fields`() {
        val pkg = PackageName.fromRaw("com.ichi2.anki").getOrThrow()
        val original = Settings(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(6),
            targetAppPackage = pkg,
            scheduleStart = LocalTime.of(8, 0),
            scheduleEnd = LocalTime.of(22, 0)
        )
        val updated = original.copy(enabled = false)
        assertEquals(original.cooldownConfig, updated.cooldownConfig)
        assertEquals(original.targetAppPackage, updated.targetAppPackage)
        assertEquals(original.scheduleStart, updated.scheduleStart)
        assertEquals(original.scheduleEnd, updated.scheduleEnd)
    }
}
