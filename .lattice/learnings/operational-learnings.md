# Operational Learnings

Experiential patterns from practice. Complements standards (what should be) with experience (what we keep learning).

## Design Patterns

- 2026-06-14 [design] Sum types over nullable fields for variant config models — When a model has conditional fields based on a discriminator (e.g., cooldown type), use a sealed interface/class instead of nullable fields. Prevents illegal state combinations at compile time.
- 2026-06-14 [design] UseCase as cross-context orchestrator — When a domain action triggers side effects in another area (settings change → cooldown state reset), the UseCase coordinates both repositories rather than leaking orchestration into the ViewModel or a single repository.

## Implementation Craft

- 2026-06-15 [implementation] MockK chained fluent mocks require explicit `returns this` — When mocking fluent interfaces (e.g., SharedPreferences.Editor), relaxed mode returns a new mock for each chained call, causing `verify` to fail. Configure `every { method() } returns this` for each method in the chain.
- 2026-06-15 [implementation] AppCompatActivity needed for MaterialTimePicker — `ComponentActivity` lacks FragmentManager. `MaterialTimePicker.show()` requires one. Use `AppCompatActivity` (extends `ComponentActivity`) to get both edge-to-edge and fragment support.
- 2026-06-15 [implementation] StateFlow observation pattern for Activities — Use `lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect { } } }` for lifecycle-aware observation. Avoids leaks and resubscribes on lifecycle restart.
- 2026-06-15 [implementation] JUnit 5 over kotlin.test for Android unit tests — Use `org.junit.jupiter.api.Assertions.*` imports. Avoid `kotlin.test` package which needs an explicit `kotlin-test` dependency that may version-conflict with the project's Kotlin version.

## Quality Signals
<!-- Recurring quality issues that keep appearing despite rules -->

## Reliability
<!-- Bug root causes, failure modes, fragile areas, boundary condition gaps -->

## Structural Health
<!-- Architectural drift, debt accumulation, coupling issues, migration lessons -->
