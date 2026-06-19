package com.example.promptly.data.repository

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LockTaskRepositoryImplTest {

    private val activity = mockk<Activity>(relaxed = true)
    private lateinit var repo: LockTaskRepositoryImpl

    @BeforeEach
    fun setUp() {
        repo = LockTaskRepositoryImpl(activity)
    }

    @Test
    fun `start returns true when lock task succeeds`() = runTest {
        every { activity.startLockTask() } returns Unit

        val result = repo.start()

        assertTrue(result)
        verify { activity.startLockTask() }
    }

    @Test
    fun `start returns false when lock task throws`() = runTest {
        every { activity.startLockTask() } throws RuntimeException("Failed")

        val result = repo.start()

        assertFalse(result)
    }

    @Test
    fun `stop calls stopLockTask`() = runTest {
        repo.stop()

        verify { activity.stopLockTask() }
    }
}
