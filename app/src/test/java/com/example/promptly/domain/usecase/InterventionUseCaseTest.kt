package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.InterventionConfig
import com.example.promptly.domain.repository.LockTaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InterventionUseCaseTest {

    private val lockTaskRepo = mockk<LockTaskRepository>()
    private lateinit var useCase: InterventionUseCase

    @BeforeEach
    fun setUp() {
        useCase = InterventionUseCase(lockTaskRepo)
    }

    @Test
    fun `prepare returns Locked when lock task starts successfully`() = runTest {
        coEvery { lockTaskRepo.start() } returns true

        val result = useCase.prepare()

        assertTrue(result is InterventionConfig.Locked)
    }

    @Test
    fun `prepare returns Fallback when lock task fails`() = runTest {
        coEvery { lockTaskRepo.start() } returns false

        val result = useCase.prepare()

        assertTrue(result is InterventionConfig.Fallback)
    }

    @Test
    fun `dismiss calls stop on repository`() = runTest {
        coEvery { lockTaskRepo.stop() } returns Unit

        useCase.dismiss()

        coVerify { lockTaskRepo.stop() }
    }

    @Test
    fun `onResurface returns Locked when lock task restarts`() = runTest {
        coEvery { lockTaskRepo.start() } returns true

        val result = useCase.onResurface()

        assertTrue(result is InterventionConfig.Locked)
    }

    @Test
    fun `onResurface returns Fallback when lock task restart fails`() = runTest {
        coEvery { lockTaskRepo.start() } returns false

        val result = useCase.onResurface()

        assertTrue(result is InterventionConfig.Fallback)
    }
}
