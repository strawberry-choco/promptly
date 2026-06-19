package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecoveryRoutingDecisionTest {

    @Test
    fun `ProceedToSettings is a RecoveryRoutingDecision`() {
        val decision: RecoveryRoutingDecision = RecoveryRoutingDecision.ProceedToSettings
        assertEquals(RecoveryRoutingDecision.ProceedToSettings, decision)
    }

    @Test
    fun `ShowRecovery is a RecoveryRoutingDecision`() {
        val decision: RecoveryRoutingDecision = RecoveryRoutingDecision.ShowRecovery
        assertEquals(RecoveryRoutingDecision.ShowRecovery, decision)
    }

    @Test
    fun `ProceedToOnboarding is a RecoveryRoutingDecision`() {
        val decision: RecoveryRoutingDecision = RecoveryRoutingDecision.ProceedToOnboarding
        assertEquals(RecoveryRoutingDecision.ProceedToOnboarding, decision)
    }

    @Test
    fun `three variants exist`() {
        assertEquals(3, RecoveryRoutingDecision::class.sealedSubclasses.size)
    }
}
