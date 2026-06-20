package com.example.promptly.data.repository

import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TargetAppRepositoryImplTest {

    private val packageManager = mockk<PackageManager>()
    private lateinit var repo: TargetAppRepositoryImpl

    @BeforeEach
    fun setUp() {
        repo = TargetAppRepositoryImpl(packageManager)
    }

    @Test
    fun `isInstalled returns true when launch intent exists`() = runTest {
        every { packageManager.getLaunchIntentForPackage("com.ichi2.anki") } returns mockk()

        val result = repo.isInstalled("com.ichi2.anki")

        assertTrue(result)
    }

    @Test
    fun `isInstalled returns false when launch intent is null`() = runTest {
        every { packageManager.getLaunchIntentForPackage("com.ichi2.anki") } returns null

        val result = repo.isInstalled("com.ichi2.anki")

        assertFalse(result)
    }

    @Test
    fun `isInstalled returns false when getLaunchIntentForPackage throws`() = runTest {
        every { packageManager.getLaunchIntentForPackage("com.ichi2.anki") } throws SecurityException()

        val result = repo.isInstalled("com.ichi2.anki")

        assertFalse(result)
    }
}
