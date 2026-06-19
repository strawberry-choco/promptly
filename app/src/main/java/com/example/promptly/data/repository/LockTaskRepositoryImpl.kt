package com.example.promptly.data.repository

import android.app.Activity
import com.example.promptly.domain.repository.LockTaskRepository

class LockTaskRepositoryImpl(
    private val activity: Activity
) : LockTaskRepository {

    override suspend fun start(): Boolean {
        return try {
            activity.startLockTask()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun stop() {
        activity.stopLockTask()
    }
}
