package com.example.promptly.domain.model

sealed class EligibilityResult {
    data object Eligible : EligibilityResult()
    data class Blocked(val reason: BlockReason) : EligibilityResult()
}

enum class BlockReason {
    FEATURE_DISABLED,
    OUTSIDE_SCHEDULE_WINDOW,
    COOLDOWN_ACTIVE
}
