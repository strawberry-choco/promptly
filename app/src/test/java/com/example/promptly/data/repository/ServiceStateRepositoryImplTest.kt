package com.example.promptly.data.repository

import android.content.Context
import android.provider.Settings
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ServiceStateRepositoryImplTest {

    private val context = mockk<Context>()
    private lateinit var repo: ServiceStateRepositoryImpl

    @BeforeEach
    fun setUp() {
        repo = ServiceStateRepositoryImpl(context)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `isEnabled returns true when service is in enabled list`() = runTest {
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(
                any(),
                eq(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            )
        } returns "com.example.promptly/com.example.promptly.PromptlyAccessibilityService:com.other.service"

        val result = repo.isEnabled()

        assertEquals(true, result)
    }

    @Test
    fun `isEnabled returns false when service is not in enabled list`() = runTest {
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(
                any(),
                eq(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            )
        } returns "com.other.app/com.other.service"

        val result = repo.isEnabled()

        assertEquals(false, result)
    }

    @Test
    fun `isEnabled returns false when enabled services string is null`() = runTest {
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(
                any(),
                eq(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            )
        } returns null

        val result = repo.isEnabled()

        assertEquals(false, result)
    }
}
