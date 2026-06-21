# Operational Learnings

Experiential patterns from practice. Complements standards (what should be) with experience (what we keep learning).

## Design Patterns

- 2026-06-14 [design] Sum types over nullable fields for variant config models — When a model has conditional fields based on a discriminator (e.g., cooldown type), use a sealed interface/class instead of nullable fields. Prevents illegal state combinations at compile time.
- 2026-06-14 [design] UseCase as cross-context orchestrator — When a domain action triggers side effects in another area (settings change → cooldown state reset), the UseCase coordinates both repositories rather than leaking orchestration into the ViewModel or a single repository.
- 2026-06-19 [design] Activity-coupled framework APIs wrapped via domain interface + Data impl — When a domain operation requires an Activity lifecycle (e.g., startLockTask/stopLockTask), define the interface in domain (pure Kotlin) and implement it in data layer with Activity reference. Pattern already established by ServiceStateRepositoryImpl receiving Context; keeps domain testable while acknowledging framework reality.
- 2026-06-19 [design] Repository interface extension over proliferation — When a dependent feature needs additional persistence operations on an existing repository interface, extend it with new methods rather than creating a parallel interface. Keeps related data access cohesive; the alternative (one tiny interface per feature) scatters read/write of related data across multiple contracts and increases constructor complexity in consumers. Applied here: CooldownRepository extended with load/save instead of creating CooldownStateRepository.
- 2026-06-20 [design] Sealed result types for use case outcomes — When a use case can produce multiple distinct outcomes, return a sealed interface (e.g., `RedirectDecision: NoTarget | Ready | Uninstalled`) rather than nullable fields or boolean flags. Makes branching explicit and exhaustive at the call site, preventing unhandled cases. Pattern consistent with existing `InterventionConfig` sealed class.

## Implementation Craft

- 2026-06-15 [implementation] MockK chained fluent mocks require explicit `returns this` — When mocking fluent interfaces (e.g., SharedPreferences.Editor), relaxed mode returns a new mock for each chained call, causing `verify` to fail. Configure `every { method() } returns this` for each method in the chain.
- 2026-06-15 [implementation] AppCompatActivity needed for MaterialTimePicker — `ComponentActivity` lacks FragmentManager. `MaterialTimePicker.show()` requires one. Use `AppCompatActivity` (extends `ComponentActivity`) to get both edge-to-edge and fragment support.
- 2026-06-15 [implementation] StateFlow observation pattern for Activities — Use `lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect { } } }` for lifecycle-aware observation. Avoids leaks and resubscribes on lifecycle restart.
- 2026-06-15 [implementation] JUnit 5 over kotlin.test for Android unit tests — Use `org.junit.jupiter.api.Assertions.*` imports. Avoid `kotlin.test` package which needs an explicit `kotlin-test` dependency that may version-conflict with the project's Kotlin version.
- 2026-06-19 [implementation] Inject ZoneId with systemDefault default for timezone-aware domain services — When a pure domain service computes dates/times (daily reset, schedule windows), accept `ZoneId` as a constructor parameter defaulting to `ZoneId.systemDefault()`. Tests inject a fixed zone for deterministic results without Android instrumentation. Production behavior unchanged; test determinism guaranteed.
- 2026-06-20 [implementation] Timing concerns (delays, countdowns) in ViewModel, not domain — When a feature needs a brief delay before acting (e.g., 1.5s auto-redirect), the delay belongs in the ViewModel via `kotlinx.coroutines.delay()`. The domain use case is synchronous from the caller's perspective; timing is a UI-layer concern. Keeps domain logic testable without virtual time.
- 2026-06-20 [implementation] Buffer capacity for ViewModel-emitted SharedFlow events — When a ViewModel emits one-shot events via SharedFlow to an Activity collector, use `extraBufferCapacity = 1` on the `MutableSharedFlow`. The default (`extraBufferCapacity = 0`) causes `emit` to suspend when no collector is attached (test setup or lifecycle timing gaps). Buffer of 1 preserves at-most-once semantics while avoiding suspension in tests and production.
- 2026-06-21 [implementation] Coroutine scope only for actual async work — When a ViewModel method becomes synchronous after removing an async call (e.g., `onDismiss` no longer calls a use case), remove the surrounding `viewModelScope.launch`. Synchronous state transitions don't need a coroutine scope — keeps the call site simpler and avoids unnecessary coroutine overhead.

## Process

- 2026-06-20 [process] Follow skill workflow before coding — When a skill is loaded and the task matches it, execute the workflow steps in order before writing any code. For requirement-forge: Step 1 (check existing docs), Step 2 (intake), Step 3-4 (epic/feature discovery), Step 5 (spec draft), Step 6 (finalize). The output is a spec document, not implementation. Breaking the sequence (e.g., coding directly without intake or writing a feature spec first) violates the protocol and produces wrong output.

## Quality Signals
<!-- Recurring quality issues that keep appearing despite rules -->

## Reliability
<!-- Bug root causes, failure modes, fragile areas, boundary condition gaps -->

## Structural Health
- 2026-06-21 [implementation] Inside-out deletion order for multi-layer removal — When removing a feature that spans layers, delete in dependency order: Domain models → Domain interfaces → Domain use cases → Data implementations → UI modifications. This guarantees no compile errors at any intermediate step and surfaces missing references layer by layer.
