package com.example.promptly.data.repository

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CooldownRepositoryImplTest {

    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true).apply {
        every { putBoolean(any(), any()) } returns this
        every { putString(any(), any()) } returns this
        every { putInt(any(), any()) } returns this
        every { putLong(any(), any()) } returns this
        every { putFloat(any(), any()) } returns this
        every { putStringSet(any(), any()) } returns this
        every { remove(any()) } returns this
        every { clear() } returns this
    }
    private lateinit var repo: CooldownRepositoryImpl

    @BeforeEach
    fun setUp() {
        every { prefs.edit() } returns editor
        repo = CooldownRepositoryImpl(prefs)
    }

    @Test
    fun `clearLastTrigger removes the key from preferences`() = runTest {
        repo.clearLastTrigger()

        verify { editor.remove("last_trigger_timestamp") }
        verify { editor.apply() }
    }
}
