package com.example.promptly.domain.model

@JvmInline
value class PackageName private constructor(val value: String) {
    companion object {
        private val PACKAGE_NAME_REGEX =
            Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

        fun fromRaw(raw: String): Result<PackageName> {
            if (raw.length > 200) return Result.failure(
                IllegalArgumentException("Package name exceeds 200 characters")
            )
            if (!PACKAGE_NAME_REGEX.matches(raw)) return Result.failure(
                IllegalArgumentException("Invalid package name format")
            )
            return Result.success(PackageName(raw))
        }
    }
}
