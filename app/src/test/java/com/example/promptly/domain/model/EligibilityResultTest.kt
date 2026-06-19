package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EligibilityResultTest {

    @Test
    fun `Eligible is an EligibilityResult`() {
        assertEquals(EligibilityResult.Eligible, EligibilityResult.Eligible as EligibilityResult)
    }

    @Test
    fun `Blocked stores three BlockReason values`() {
        val reasons = BlockReason.entries
        assertEquals(3, reasons.size)
    }

    @Test
    fun `Blocked with FEATURE_DISABLED`() {
        val result = EligibilityResult.Blocked(BlockReason.FEATURE_DISABLED)
        assertEquals(BlockReason.FEATURE_DISABLED, (result as EligibilityResult.Blocked).reason)
    }

    @Test
    fun `Blocked with OUTSIDE_SCHEDULE_WINDOW`() {
        val result = EligibilityResult.Blocked(BlockReason.OUTSIDE_SCHEDULE_WINDOW)
        assertEquals(BlockReason.OUTSIDE_SCHEDULE_WINDOW, (result as EligibilityResult.Blocked).reason)
    }

    @Test
    fun `Blocked with COOLDOWN_ACTIVE`() {
        val result = EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE)
        assertEquals(BlockReason.COOLDOWN_ACTIVE, (result as EligibilityResult.Blocked).reason)
    }

    @Test
    fun `Blocked equality compares by reason`() {
        assertEquals(
            EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE),
            EligibilityResult.Blocked(BlockReason.COOLDOWN_ACTIVE)
        )
    }
}
