package com.example.promptly.domain.model

sealed class RecoveryRoutingDecision {
    data object ProceedToSettings : RecoveryRoutingDecision()
    data object ShowRecovery : RecoveryRoutingDecision()
    data object ProceedToOnboarding : RecoveryRoutingDecision()
}
