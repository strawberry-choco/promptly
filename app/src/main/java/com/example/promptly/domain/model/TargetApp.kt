package com.example.promptly.domain.model

data class TargetApp(
    val displayName: String,
    val packageName: String?
) {
    companion object {
        val CURATED_LIST = listOf(
            TargetApp("No target app", null),
            TargetApp("Anki", "com.ichi2.anki"),
            TargetApp("Medito", "meditofoundation.medito")
        )
    }
}
