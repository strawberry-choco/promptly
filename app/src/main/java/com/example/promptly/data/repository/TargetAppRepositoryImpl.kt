package com.example.promptly.data.repository

import android.content.pm.PackageManager
import com.example.promptly.domain.repository.TargetAppRepository

class TargetAppRepositoryImpl(
    private val packageManager: PackageManager
) : TargetAppRepository {

    override suspend fun isInstalled(packageName: String): Boolean {
        return try {
            packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (e: Exception) {
            false
        }
    }
}
