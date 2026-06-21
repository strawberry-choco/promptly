---
feature: Settings UI
requirement_doc: .lattice/requirements/features/settings-ui.md
created: 2026-06-14
---

# Settings UI

> Configuration screen for Promptly's intervention parameters — enabled state, cooldown type, schedule window, and target app selection.

## Design: Level 1 -- Capabilities

1. **View all current settings** — On launch, user sees every config option loaded from persisted state.
2. **Toggle Promptly on/off** — Flip switch to enable/disable intervention. Disabling clears cooldown state.
3. **Set a target app** — Free-form text entry for Android package name with regex format validation. Persisted immediately. Shows "No target app" when empty.
4. **Configure cooldown behavior** — Choose Daily Reset (time picker) or N-hour Interval (1–24 selector). Changing type resets cooldown state.
5. **Set the schedule window** — Start/end time pickers for active period. Supports overnight windows (start > end).

## Design: Level 2 -- Components

| # | Component | Layer | Responsibility |
|---|-----------|-------|---------------|
| 1 | SettingsActivity | UI (Android) | Hosts XML layout, lifecycle owner, delegates to ViewModel |
| 2 | SettingsViewModel | UI | Holds UI state as StateFlow, maps domain model to UI model, exposes save actions |
| 3 | SettingsUseCase | Domain | Single orchestrator: loads current settings on init, persists changes on each update |
| 4 | Settings | Domain (model) | Pure Kotlin data class — immutable value object, no Android dependencies |
| 5 | SettingsRepository | Domain (interface) | Contract: `load(): Settings`, `save(settings: Settings)` |
| 6 | SettingsRepositoryImpl | Data | SharedPreferences-backed; maps to/from domain Settings |

## Design: Level 3 -- Interactions

### Flow 1: Load Settings (screen opens)
```
SettingsActivity.onCreate()
  → SettingsViewModel.init()
    → SettingsUseCase.load()
      → SettingsRepository.load()
        → SharedPreferences read → Settings
    ← Settings
  ← Settings → maps to UiState → emitted via StateFlow
```

### Flow 2: Validate and Save Target App (text entry with validation)
```
User types in text entry dialog and confirms
  → SettingsViewModel.onTargetAppChanged(raw: String)
    → PackageName.fromRaw(raw)  → success → PackageName
    → PackageName.fromRaw(raw)  → failure → show inline error, abort save
  → if valid: SettingsUseCase.save(settings.copy(targetAppPackage = packageName))
    → SettingsRepository.save(settings)
      → SharedPreferences write (string value or null)
  ← new UiState emitted via StateFlow ("No target app" when null)
```

### Flow 3: Save Setting on Change (toggle, picker, time change)
```
User interaction
  → SettingsViewModel.onXChanged(value)
    → SettingsUseCase.save(settings.copy(x = value))
      → SettingsRepository.save(settings)
        → SharedPreferences write
    ← Unit
  ← new UiState emitted via StateFlow
```

### Flow 4: Change Cooldown Type (resets cooldown state)
```
Same as Flow 2 + after save:
  → SettingsUseCase.save(newSettings)
  → CooldownRepository.clearLastTrigger()
    → SharedPreferences write (lastTriggerTimestamp = null)
```

### Flow 5: Toggle Off (resets cooldown state)
```
Same as Flow 3 — save(enabled = false) + CooldownRepository.clearLastTrigger()
```

### Key flow decisions
- Cooldown state reset is orchestrated by `SettingsUseCase`, not the repository.
- A `CooldownRepository` interface (domain) + impl (data) handles the cooldown timestamp.
- All saves are immediate — no debounce, no save button.

## Design: Level 4 -- Contracts

### Domain model
```kotlin
// domain/model/PackageName.kt
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

// domain/model/CooldownConfig.kt
sealed interface CooldownConfig {
    data class DailyReset(val resetTime: LocalTime) : CooldownConfig
    data class NHourInterval(val intervalHours: Int) : CooldownConfig {
        init { require(intervalHours in 1..24) }
    }
}

// domain/model/Settings.kt
data class Settings(
    val enabled: Boolean,
    val cooldownConfig: CooldownConfig,
    val targetAppPackage: PackageName?,   // null = "No target app"
    val scheduleStart: LocalTime,
    val scheduleEnd: LocalTime
)
```

### Domain repository interfaces
```kotlin
// domain/repository/SettingsRepository.kt
interface SettingsRepository {
    suspend fun load(): Settings
    suspend fun save(settings: Settings)
}

// domain/repository/CooldownRepository.kt
interface CooldownRepository {
    suspend fun clearLastTrigger()
}
```

### Domain use case
```kotlin
// domain/usecase/SettingsUseCase.kt
class SettingsUseCase(
    private val settingsRepository: SettingsRepository,
    private val cooldownRepository: CooldownRepository
) {
    suspend fun load(): Settings
    suspend fun save(settings: Settings)
    suspend fun disable(): Settings                    // save(enabled=false) + clear cooldown
    suspend fun changeCooldownConfig(config: CooldownConfig): Settings  // save + clear cooldown
}
```

### Data layer implementations
```kotlin
// data/repository/SettingsRepositoryImpl.kt
class SettingsRepositoryImpl(
    private val prefs: SharedPreferences
) : SettingsRepository

// data/repository/CooldownRepositoryImpl.kt
class CooldownRepositoryImpl(
    private val prefs: SharedPreferences
) : CooldownRepository
```

### UI layer
```kotlin
// ui/settings/SettingsViewModel.kt
class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {
    val uiState: StateFlow<Settings>  // domain model doubles as UI state
}
```

## Decisions Log

<!-- Add new at bottom. Never remove. -->

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-14 | Level 1 approved with 5 capabilities matching the requirement spec scope. | All scenarios map directly to capabilities; no scope reduction or extension needed. | N/A — straightforward mapping. |
| 2026-06-14 | Cooldown state reset orchestrated by SettingsUseCase, not SettingsRepositoryImpl. | Keeps SettingsRepository focused on settings CRUD; UseCase coordinates cross-cutting concern with separate CooldownRepository. | (a) SettingsRepositoryImpl handles both — violates SRP; (b) ViewModel orchestrates — leaks domain orchestration into UI. |
| 2026-06-14 | Sum type `sealed interface CooldownConfig` replaces `CooldownType` enum + nullable fields. | Makes illegal states unrepresentable; `intervalHours` and `resetTime` only exist on the variant that uses them. | Enum + nullable `LocalTime?`/`Int?` fields — allows invalid state combinations at compile time. |
| 2026-06-14 | Domain `Settings` model used directly as ViewModel UI state. | Settings is pure Kotlin (no Android deps); separate UiState model adds mapping boilerplate with zero safety benefit. | Separate `SettingsUiState` data class — pure boilerplate for this feature. |
| 2026-06-14 | Design approved at Level 4. Blueprint complete ready for implementation. | All four design levels agreed and persisted; contracts define clean boundaries between layers. | N/A |
| 2026-06-15 | `TargetApp` data class + provider added to domain/model. | Pure data definition for the curated app list (Anki, Medito, None). Belongs in domain because it defines business-relevant app targets. | Hardcoded in Activity — coupling data to UI; string resources only — no structured model. |
| 2026-06-15 | `ActivitySettingsBinding` (view binding) enabled for type-safe view access. | Eliminates `findViewById` boilerplate and null-safety issues. Consistent with standard Android practice. | Manual `findViewById` — verbose, error-prone; DataBinding — overkill for simple layouts. |
| 2026-06-15 | `SettingsActivity` extends `AppCompatActivity` (not `ComponentActivity`) for Fragment support. | `MaterialTimePicker` requires `FragmentManager` from `FragmentActivity`. `AppCompatActivity` provides this while still enabling edge-to-edge. | Using older `TimePickerDialog` — loses Material 3 visual consistency; adding Fragment dependency separately — unnecessary complication. |
| 2026-06-15 | Settings persisted to SharedPreferences using "HH:mm:ss" ISO time strings. | Human-readable, easily debuggable, standard Java time format parsing. | Minutes-since-midnight Int — less readable when debugging; custom format string — more code with no benefit. |
| 2026-06-15 | Cooldown type selection uses `MaterialButtonToggleGroup` (segmented button). | Clean Material 3 appearance, single-selection enforced, naturally shows both options. | `RadioGroup` with MaterialRadioButtons — more vertical space; Spinner/dropdown — hides options behind a tap. |
| 2026-06-15 | Data layer tests use `verify` on chained `SharedPreferences.Editor` with `returns this` mock config. | `mockk` relaxed mode returns new mock instances for chained calls by default, breaking `verify` assertions. Explicit `every { ... } returns this` enables proper chaining verification. | — |
| 2026-06-21 | Target app changed from curated list (Anki, Medito, None) to free-form text entry with regex validation. | Requirement spec mandates free-form entry; curated list is too restrictive for real-world use. | Keeping curated list — doesn't match requirements; text field without validation — allows invalid package names to persist. |
| 2026-06-21 | `PackageName` inline value class with `fromRaw` factory validates format via regex + 200-char limit. | Self-validating value object makes invalid states unrepresentable. Inline class avoids allocation overhead. | Validation in ViewModel — leaks domain rule into UI; raw String — no type safety. |
| 2026-06-21 | Design reconciled with requirement spec. Blueprint complete ready for implementation. | All four design levels updated to match requirement spec: curated list replaced with free-form text entry + PackageName value object. | N/A |

## Design Summary

### Components and layer assignments
| Component | Layer |
|-----------|-------|
| SettingsActivity | UI |
| SettingsViewModel | UI |
| SettingsUseCase | Domain |
| Settings (model) | Domain |
| CooldownConfig (sealed interface) | Domain |
| PackageName (inline value class) | Domain |
| SettingsRepository (interface) | Domain |
| CooldownRepository (interface) | Domain |
| SettingsRepositoryImpl | Data |
| CooldownRepositoryImpl | Data |

### Key contracts and interfaces
- `SettingsRepository` — `load(): Settings`, `save(settings: Settings)`
- `CooldownRepository` — `clearLastTrigger()`
- `SettingsUseCase` — `load()`, `save()`, `disable()`, `changeCooldownConfig()`
- `Settings` data class with `CooldownConfig` sum type — illegal states unrepresentable
- `PackageName` inline value class with `fromRaw(raw: String): Result<PackageName>` factory

### Architectural constraints
- Domain layer is pure Kotlin — no Android framework imports
- SharedPreferences in data layer only
- No architecture pattern (MVVM/MVI) established — using ViewModel + StateFlow
- Manual constructor injection (no DI framework)
- XML layouts (not Compose)

### Domain model decisions
- `Settings` is a value object (immutable, replaced atomically)
- `CooldownConfig` is a sealed interface sum type with two variants
- `PackageName` is an inline value class with `fromRaw` factory — validates format at construction
- No aggregates or entities — settings is a single cohesive config document
- Domain model doubles as UI state (pure Kotlin, no framework deps)

### Open questions resolved during design
- Cooldown state reset ownership → orchestrated by `SettingsUseCase`
- Sum type for cooldown config → `sealed interface CooldownConfig`
- UiState vs domain model → domain model used directly
- Target app approach → free-form text entry with `PackageName` value object validation

### Design status
**Approved — ready for implementation**

## Open Questions

<!-- When resolved, capture as decision above and remove from here. -->

## Constraints

- All settings persisted immediately on change — no save button, no debounce.
- SharedPreferences is the sole storage mechanism.
- Cooldown type change resets last trigger timestamp.
- Toggle off also resets last trigger timestamp.
- Overnight schedule windows (start > end) are supported.
- No authentication, no multi-user, no cloud sync.

## Key Files

| Path | Role |
|------|------|
| `domain/model/CooldownConfig.kt` | Sealed interface — DailyReset and NHourInterval variants with validation. |
| `domain/model/Settings.kt` | Immutable settings value object with defaults. |
| `domain/model/PackageName.kt` | Inline value class with `fromRaw` factory — validates format regex + 200-char limit. |
| `domain/repository/SettingsRepository.kt` | Interface: `load()`, `save()`. |
| `domain/repository/CooldownRepository.kt` | Interface: `clearLastTrigger()`. |
| `domain/usecase/SettingsUseCase.kt` | Orchestrator — load, save, disable (clear cooldown), changeCooldownConfig (clear cooldown). |
| `data/repository/SettingsRepositoryImpl.kt` | SharedPreferences-backed implementation. ISO time strings. |
| `data/repository/CooldownRepositoryImpl.kt` | SharedPreferences-backed — removes last trigger key. |
| `ui/settings/SettingsViewModel.kt` | Holds `StateFlow<Settings>`, exposes mutation methods, delegates to UseCase. |
| `ui/settings/SettingsActivity.kt` | AppCompatActivity with view binding, Material 3 cards, time pickers. |
| `res/layout/activity_settings.xml` | Scrollable card-based layout, MaterialButtonToggleGroup for cooldown type, conditional visibility sections. |
| `res/values/strings.xml` | Settings-related strings. |
| `res/values/dimens.xml` | Padding and margin values for settings layout. |
