package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true).apply {
        every { putBoolean(any(), any()) } returns this
        every { putString(any(), any()) } returns this
        every { putInt(any(), any()) } returns this
        every { putLong(any(), any()) } returns this
        every { putFloat(any(), any()) } returns this
        every { putStringSet(any(), any()) } returns this
        every { remove(any()) } returns this
        every { clear() } returns this
        every { commit() } returns true
    }
    private lateinit var repo: SettingsRepositoryImpl

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        every { prefs.edit() } returns editor
        repo = SettingsRepositoryImpl(prefs)
    }

    @Test
    fun `load returns defaults when no values persisted`() = runTest {
        every { prefs.getBoolean(any(), any()) } returns false
        every { prefs.getString(any(), any()) } returns null
        every { prefs.getInt(any(), any()) } returns 0

        val result = repo.load()

        assertEquals(false, result.enabled)
        assertNull(result.targetAppPackage)
    }

    @Test
    fun `load reads persisted values`() = runTest {
        every { prefs.getBoolean(eq("enabled"), any()) } returns true
        every { prefs.getString(eq("cooldown_type"), any()) } returns "n_hour"
        every { prefs.getInt(eq("interval_hours"), any()) } returns 6
        every { prefs.getString(eq("target_app_package"), any()) } returns "com.ichi2.anki"
        every { prefs.getString(eq("schedule_start"), any()) } returns "08:00:00"
        every { prefs.getString(eq("schedule_end"), any()) } returns "22:00:00"

        val result = repo.load()

        assertEquals(true, result.enabled)
        assertEquals(CooldownConfig.NHourInterval(6), result.cooldownConfig)
        assertEquals("com.ichi2.anki", result.targetAppPackage)
        assertEquals(LocalTime.of(8, 0), result.scheduleStart)
        assertEquals(LocalTime.of(22, 0), result.scheduleEnd)
    }

    @Test
    fun `save writes all settings to preferences`() = runTest {
        val settings = Settings(
            enabled = true,
            cooldownConfig = CooldownConfig.DailyReset(LocalTime.of(2, 0)),
            targetAppPackage = "medito.app",
            scheduleStart = LocalTime.of(7, 0),
            scheduleEnd = LocalTime.of(23, 0)
        )

        repo.save(settings)

        verify { editor.putBoolean("enabled", true) }
        verify { editor.putString("target_app_package", "medito.app") }
        verify { editor.putString("schedule_start", "07:00:00") }
        verify { editor.putString("schedule_end", "23:00:00") }
        verify { editor.putString("cooldown_type", "daily_reset") }
        verify { editor.putString("reset_time", "02:00:00") }
        verify(exactly = 2) { editor.commit() }
    }

    @Test
    fun `save writes NHourInterval cooldown config`() = runTest {
        val settings = Settings(
            cooldownConfig = CooldownConfig.NHourInterval(12)
        )

        repo.save(settings)

        verify { editor.putString("cooldown_type", "n_hour") }
        verify { editor.putInt("interval_hours", 12) }
        verify(exactly = 2) { editor.commit() }
    }

    @Test
    fun `load reads DailyReset with reset time`() = runTest {
        every { prefs.getBoolean(eq("enabled"), any()) } returns true
        every { prefs.getString(eq("cooldown_type"), any()) } returns "daily_reset"
        every { prefs.getString(eq("reset_time"), any()) } returns "06:30:00"
        every { prefs.getString(eq("target_app_package"), any()) } returns null
        every { prefs.getString(eq("schedule_start"), any()) } returns "09:00:00"
        every { prefs.getString(eq("schedule_end"), any()) } returns "17:00:00"

        val result = repo.load()

        val dailyReset = result.cooldownConfig as CooldownConfig.DailyReset
        assertEquals(LocalTime.of(6, 30), dailyReset.resetTime)
    }

    @Test
    fun `save clears target app when null`() = runTest {
        val settings = Settings(targetAppPackage = null)

        repo.save(settings)

        verify { editor.putString("target_app_package", null as String?) }
        verify(exactly = 2) { editor.commit() }
    }
}
