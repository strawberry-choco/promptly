package com.example.promptly.domain.repository

interface TargetAppRepository {
    suspend fun isInstalled(packageName: String): Boolean
}
