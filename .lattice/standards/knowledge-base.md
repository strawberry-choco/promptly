---
feature: "Promptly Knowledge Base"
mode: override
created: "2026-06-14"
---

> This is the knowledge base for **Promptly** (Android app). It primes AI with project-specific context -- tech stack, architecture, trusted sources, and project structure -- so generated code fits this codebase rather than defaulting to generic patterns.

## 1. Architecture Overview

Single-module Android application targeting modern phones (minSdk 26). Standard Android component lifecycle with `ComponentActivity` entrypoint. XML-based layouts with Material 3 DayNight theming, NoActionBar. No architecture pattern (MVVM / MVI / etc.) established yet -- project is in early stage. No backend API, local database, or DI framework currently configured.

## 2. Tech Stack and Versions

| Technology | Version |
|---|---|
| Gradle | 8.13 (wrapper) |
| AGP | 8.9.1 |
| Kotlin | 2.1.0 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Java target | 17 |
| Theme | Material 3 DayNight, NoActionBar |
| UI | Android XML layouts (not Jetpack Compose) |
| Entrypoint | `ComponentActivity` |
| Android SDK | `/home/tofu/Android` |

No DI framework in use. No test framework configured. No CI pipeline set up.

## 3. Curated Knowledge Sources

### Official Documentation
| Topic | Source | Why We Trust It |
|---|---|---|
| Android development | https://developer.android.com/docs | Official reference, canonical |
| Kotlin language | https://kotlinlang.org/docs/ | Official language docs |
| Material Design 3 | https://m3.material.io/ | Official design guidelines |

### Internal References
| Topic | Path | What It Captures |
|---|---|---|
| ADRs | docs/adr/ | Architecture decisions (to be created) |
| Project context | CONTEXT.md | Evolving project decisions and domain language |

## 4. Project Structure

```
promptly/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/promptly/
│           │   └── MainActivity.kt
│           ├── res/
│           │   ├── drawable/
│           │   ├── layout/
│           │   │   └── activity_main.xml
│           │   ├── mipmap-anydpi-v26/
│           │   └── values/
│           │       ├── strings.xml
│           │       └── themes.xml
│           └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── gradle.properties
├── gradlew / gradlew.bat
├── opencode.json
└── AGENTS.md
```

Standard single-module Android project. All source code under `app/src/main/java/com/example/promptly/`. Resources under `app/src/main/res/`.

## 5. Project Conventions

<!-- TODO: Fill in during next revision -->

---

*Generated for Promptly on 2026-06-14. Mode: override.*
*Produced by the knowledge-priming-refiner skill.*
