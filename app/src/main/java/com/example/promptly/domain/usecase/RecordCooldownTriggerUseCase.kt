package com.example.promptly.domain.usecase

import com.example.promptly.domain.repository.CooldownRepository

class RecordCooldownTriggerUseCase(
    private val cooldownRepository: CooldownRepository
) {
    suspend operator fun invoke() {
        cooldownRepository.saveLastTriggerEpochMillis(System.currentTimeMillis())
    }
}
