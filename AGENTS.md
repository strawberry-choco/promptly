# AGENTS.md — promptly

Android (Kotlin) app. Single module at `:app`.

## Build & run

```sh
./gradlew assembleDebug              # full build
./gradlew :app:assembleDebug         # single module
```

Build produces `app/build/outputs/apk/debug/app-debug.apk`.

## Stack

| Layer | Version |
|---|---|
| Gradle | 8.13 (wrapper) |
| AGP | 8.9.1 |
| Kotlin | 2.1.0 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| Java target | 17 |
| Android SDK | `/home/tofu/Android` (set in `local.properties`) |
| Theme | Material 3 DayNight, NoActionBar |
| Entrypoint | `com.example.promptly.MainActivity` (`ComponentActivity`) |

## Conventions

- `kotlin.code.style=official` in `gradle.properties`
- AndroidX enabled, non-transitive R classes
- No test framework or CI configured yet
- `.gitignore` covers standard Android artifacts (`.gradle`, `build/`, `local.properties`, `.idea`, APK/AAB)

## OpenCode config

`opencode.json` at root enables skills from `.opencode/skills/`. The 55 skill files are agent workflow definitions — do not modify unless asked.

## No tests, no lint, no CI

All absent. An agent adding any of these should choose what's appropriate for Android/Kotlin (e.g. JUnit + Compose UI Test, detekt/ktlint, GitHub Actions with `gradle` setup action).

## Constraints

- `.opencode/skills/` is infrastructure — leave it alone unless explicitly asked to edit it.
