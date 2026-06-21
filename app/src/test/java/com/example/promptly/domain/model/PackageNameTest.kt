package com.example.promptly.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PackageNameTest {

    @Test
    fun `valid package name creates PackageName`() {
        val result = PackageName.fromRaw("com.ichi2.anki")
        assertTrue(result.isSuccess)
        assertEquals("com.ichi2.anki", result.getOrNull()?.value)
    }

    @Test
    fun `valid package name with underscores`() {
        val result = PackageName.fromRaw("com.example.my_app")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `valid package name with multiple segments`() {
        val result = PackageName.fromRaw("org.example.sub.domain")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `rejects package name without dot`() {
        val result = PackageName.fromRaw("invalid")
        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects package name starting with digit`() {
        val result = PackageName.fromRaw("1com.example.app")
        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects package name with spaces`() {
        val result = PackageName.fromRaw("com example app")
        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects package name with special characters`() {
        val result = PackageName.fromRaw("com.example.app#bad")
        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects package name exceeding 200 characters`() {
        val longName = "com.example." + "a".repeat(200)
        val result = PackageName.fromRaw(longName)
        assertTrue(result.isFailure)
    }

    @Test
    fun `accepts package name at 200 character boundary`() {
        val segments = mutableListOf("com")
        var remaining = 200 - 4 // "com."
        while (remaining > 0) {
            val segmentLen = minOf(remaining - 1, 63)
            if (segmentLen <= 0) break
            segments.add("a".repeat(segmentLen))
            remaining -= segmentLen + 1
        }
        val name = segments.joinToString(".")
        if (name.length <= 200) {
            val result = PackageName.fromRaw(name)
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `single segment after dot is valid`() {
        val result = PackageName.fromRaw("a.b")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `inline value class wraps string`() {
        val pkg = PackageName.fromRaw("com.example.app").getOrThrow()
        assertEquals("com.example.app", pkg.value)
    }
}
