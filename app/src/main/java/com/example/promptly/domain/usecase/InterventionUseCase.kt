package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.InterventionConfig
import com.example.promptly.domain.repository.LockTaskRepository

class InterventionUseCase(
    private val lockTaskRepository: LockTaskRepository
) {
    suspend fun prepare(): InterventionConfig {
        return if (lockTaskRepository.start()) {
            InterventionConfig.Locked
        } else {
            InterventionConfig.Fallback
        }
    }

    suspend fun dismiss() {
        lockTaskRepository.stop()
    }

    suspend fun onResurface(): InterventionConfig {
        return if (lockTaskRepository.start()) {
            InterventionConfig.Locked
        } else {
            InterventionConfig.Fallback
        }
    }
}
