package com.example.promptly.data.repository

import android.content.SharedPreferences
import com.example.promptly.domain.model.OnboardingState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OnboardingRepositoryImplTest {

    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true).apply {
        every { putString(any(), any()) } returns this
        every { putBoolean(any(), any()) } returns this
        every { putInt(any(), any()) } returns this
        every { putLong(any(), any()) } returns this
        every { putFloat(any(), any()) } returns this
        every { putStringSet(any(), any()) } returns this
        every { remove(any()) } returns this
        every { clear() } returns this
    }
    private lateinit var repo: OnboardingRepositoryImpl

    @BeforeEach
    fun setUp() {
        every { prefs.edit() } returns editor
        repo = OnboardingRepositoryImpl(prefs)
    }

    @Test
    fun `load returns New when no value persisted`() = runTest {
        every { prefs.getString(any(), any()) } returns null

        val result = repo.load()

        assertTrue(result is OnboardingState.New)
    }

    @Test
    fun `load returns Completed when persisted`() = runTest {
        every { prefs.getString(eq("onboarding_state"), any()) } returns "completed"

        val result = repo.load()

        assertTrue(result is OnboardingState.Completed)
    }

    @Test
    fun `load returns Skipped when persisted`() = runTest {
        every { prefs.getString(eq("onboarding_state"), any()) } returns "skipped"

        val result = repo.load()

        assertTrue(result is OnboardingState.Skipped)
    }

    @Test
    fun `save persists Completed state`() = runTest {
        repo.save(OnboardingState.Completed)

        verify { editor.putString("onboarding_state", "completed") }
        verify { editor.apply() }
    }

    @Test
    fun `save persists Skipped state`() = runTest {
        repo.save(OnboardingState.Skipped)

        verify { editor.putString("onboarding_state", "skipped") }
        verify { editor.apply() }
    }

    @Test
    fun `save does not persist New state`() = runTest {
        repo.save(OnboardingState.New)

        verify(exactly = 0) { editor.putString(any(), any()) }
        verify(exactly = 0) { editor.apply() }
    }
}
