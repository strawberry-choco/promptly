package com.example.promptly.domain.gate

import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.Settings
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.time.ZoneId

class GateTest {

    private val zone = ZoneId.of("UTC")

    private inner class FakeSettingsRepository(initial: Settings) : SettingsRepository {
        var settings = initial

        override suspend fun load(): Settings = settings

        override suspend fun save(settings: Settings) {
            this.settings.copy(enabled = settings.enabled)
        }
    }

    private inner class FakeCooldownRepository(var lastTriggerEpochMillis: Long?) :
        CooldownRepository {

        val savedTimestamps = mutableListOf<Long>()
        var loadCalls = 0

        override suspend fun loadLastTriggerEpochMillis(): Long? {
            loadCalls++
            return lastTriggerEpochMillis
        }

        override suspend fun saveLastTriggerEpochMillis(timestamp: Long) {
            savedTimestamps += timestamp
            lastTriggerEpochMillis = timestamp
        }

        override suspend fun clearLastTrigger() {
            lastTriggerEpochMillis = null
        }
    }

    private fun fakeClock(now: Long): () -> Long = { now }
    // 2026-08-22T10:00:00Z, inside the default 09:00-17:00 UTC schedule
    private val eligibleNowEpochMillis = 1787392800000L

    @Test
    fun `eligible evaluation shows with a pending claim and persists nothing`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(eligibleNowEpochMillis),
            zone = zone
        )

        val decision = gate.decide(foregroundPackage = "com.example.anyapp")

        assertTrue(decision is GateDecision.Show)
        val shown = decision as GateDecision.Show
        assertEquals(eligibleNowEpochMillis, shown.pendingClaim.triggerEpochMillis)
        assertTrue(shown.pendingClaim.isUnconfirmed)
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }

    @Test
    fun `repeat evaluation for same foreground package skips without consulting storage`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(eligibleNowEpochMillis),
            zone = zone
        )

        assertTrue(gate.decide(foregroundPackage = "com.example.firstapp") is GateDecision.Show)
        assertEquals(GateDecision.Skip, gate.decide(foregroundPackage = "com.example.firstapp"))
        assertEquals(0, cooldownRepository.savedTimestamps.size)
        assertEquals(1, cooldownRepository.loadCalls)
    }

    // 2026-08-22T08:00:00Z, before the default 09:00 UTC schedule start
    private val outsideScheduleNowEpochMillis = 1787385600000L

    // 2026-08-22T09:00:00Z, two hours before eligibleNow inside a 2h interval
    private val recentTriggerEpochMillis = 1787389200000L

    @Test
    fun `disabled evaluation skips without launching or persisting`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(enabled = false, targetAppPackage = null)
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(eligibleNowEpochMillis),
            zone = zone
        )

        val decision = gate.decide(foregroundPackage = "com.example.anyapp")

        assertEquals(GateDecision.Skip, decision)
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }

    @Test
    fun `schedule-blocked evaluation skips without launching or persisting`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(outsideScheduleNowEpochMillis),
            zone = zone
        )

        val decision = gate.decide(foregroundPackage = "com.example.anyapp")

        assertEquals(GateDecision.Skip, decision)
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }

    @Test
    fun `cooldown-blocked evaluation skips without launching or persisting`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository =
            FakeCooldownRepository(lastTriggerEpochMillis = recentTriggerEpochMillis)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(eligibleNowEpochMillis),
            zone = zone
        )

        val decision = gate.decide(foregroundPackage = "com.example.anyapp")

        assertEquals(GateDecision.Skip, decision)
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }

    @Test
    fun `blocked then enabled transition skips while blocked and shows for a fresh package after enabling`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = false,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = fakeClock(eligibleNowEpochMillis),
            zone = zone
        )

        assertEquals(GateDecision.Skip, gate.decide(foregroundPackage = "com.example.blockedapp"))
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())

        settingsRepository.settings = Settings(
            enabled = true,
            cooldownConfig = CooldownConfig.NHourInterval(2),
            targetAppPackage = null
        )

        assertEquals(GateDecision.Skip, gate.decide(foregroundPackage = "com.example.blockedapp"))
        assertTrue(gate.decide(foregroundPackage = "com.example.freshapp") is GateDecision.Show)
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }

    @Test
    fun `confirm persists the claimed trigger timestamp verbatim`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        var now = eligibleNowEpochMillis
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = { now },
            zone = zone
        )

        val decision = gate.decide(foregroundPackage = "com.example.anyapp")
        val claim = (decision as GateDecision.Show).pendingClaim
        now += 5_000

        gate.confirm(claim)

        assertEquals(listOf(claim.triggerEpochMillis), cooldownRepository.savedTimestamps)
    }
    @Test
    fun `a second eligible evaluation within the freshness window skips without launching or persisting`() = runTest {
        val settingsRepository = FakeSettingsRepository(
            Settings(
                enabled = true,
                cooldownConfig = CooldownConfig.NHourInterval(2),
                targetAppPackage = null
            )
        )
        val cooldownRepository = FakeCooldownRepository(lastTriggerEpochMillis = null)
        var now = eligibleNowEpochMillis
        val gate = Gate(
            settingsRepository,
            cooldownRepository,
            clock = { now },
            zone = zone,
            pendingClaimFreshnessMillis = 10_000L
        )

        assertTrue(gate.decide(foregroundPackage = "com.example.firstapp") is GateDecision.Show)
        now += 3_000

        assertEquals(GateDecision.Skip, gate.decide(foregroundPackage = "com.example.secondapp"))
        assertTrue(cooldownRepository.savedTimestamps.isEmpty())
    }}
