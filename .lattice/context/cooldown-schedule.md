---
feature: Cooldown Schedule
requirement_doc: .lattice/requirements/features/cooldown-schedule.md
created: 2026-06-19
---

# Cooldown Schedule

> Configurable cooldown and schedule window logic that gates whether the intervention fires on phone unlock, preventing user fatigue from repeated interruptions.

## Decisions Log

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | Level 1 approved — 5 capabilities matching requirement spec scope | All requirement scenarios map directly to capabilities; no scope reduction or extension needed | N/A |
| 2026-06-19 | Level 2 — CooldownEligibilityService as pure domain service (no I/O, no clock) | Keeps business logic testable without Android instrumentation; service takes all inputs as parameters, not pulling from repositories | Embedding I/O in service (mixed concerns, untestable); static utility methods (less discoverable, harder to mock) |
| 2026-06-19 | Level 2 — CheckCooldownUseCase orchestrates both SettingsRepository + CooldownRepository | Use case owns cross-repository coordination; service stays pure. Pattern matches existing SettingsUseCase doing the same | Service calls repositories directly (breaks separation of pure logic from I/O coordination) |
| 2026-06-19 | Level 2 — EligibilityResult sealed class instead of raw Boolean | Compiler-enforced exhaustive handling; BlockReason makes test assertions explicit about why blocked | Raw Boolean (no reason, ambiguity in tests); nullable Boolean (tri-state confusion) |
| 2026-06-19 | Level 2 — RecordCooldownTriggerUseCase created as separate use case even though thin | Consistent with architecture: all operations route through UseCase. Keeps trigger recording independently callable without coupling to InterventionUseCase | Adding record method to InterventionUseCase (couples intervention display to cooldown); inline in caller (leaks domain coordination out of use case layer) |
| 2026-06-19 | Level 2 — CooldownRepository extended with load/save methods (keeping clearLastTrigger) | Single repository for all cooldown persistence. load/save for reading/writing last trigger; clearLastTrigger kept for SettingsUseCase compatibility | Separate CooldownStateRepository (proliferation of tiny repositories); merge into SettingsRepository (mixed concerns — settings vs runtime state) |
| 2026-06-19 | Level 3 — Cooldown check runs before InterventionActivity launch, not inside it | Decouples cooldown eligibility from intervention display lifecycle. Cleaner separation: cooldown gates launch, intervention handles display | Embedding in InterventionUseCase.prepare() (couples features); checking inside InterventionActivity (too late — activity already launched) |
| 2026-06-19 | Level 3 — Trigger timestamp recorded at launch time, not dismissal time | Cooldown measures "intervention was shown to user" not "user dismissed it". Matches user expectation: once shown, cooldown period starts | Recording on dismiss (confuses cooldown window with user response time; if user leaves phone unlocked, cooldown never starts) |
| 2026-06-19 | Design approved at Level 4. Blueprint complete ready for implementation | All 5 capabilities, 4 components, 3 interaction flows, 5+ contracts specified | N/A |
| 2026-06-19 | CooldownEligibilityService accepts injectable ZoneId (default systemDefault) | Makes daily reset computation deterministic in tests without test-timezone coupling; production uses system default unchanged | Hardcoding ZoneId.systemDefault() (untestable across timezones); making test a Clock parameter (over-engineered for this scope) |
| 2026-06-19 | All components implemented + tested in single pass | Blueprint was fully specified at Level 4 contracts — no deviations needed during implementation | N/A |

## Open Questions

## Constraints

- `CooldownRepository` interface lives in domain layer — framework dependencies kept in data layer
- Cooldown eligibility evaluated at unlock time — must be fast, no network calls
- Trigger timestamp stored as UTC epoch millis for DST/timezone resilience
- Schedule window supports overnight ranges (start > end)
- Cooldown state reset on settings change owned by `SettingsUseCase` (existing behavior)

## Key Files

| Path | Role | Status |
|------|------|--------|
| `domain/model/EligibilityResult.kt` | Sealed class: Eligible / Blocked with reason | **New** — done |
| `domain/service/CooldownEligibilityService.kt` | Pure business logic: evaluates enabled, schedule, cooldown expiry | **New** — done |
| `domain/usecase/CheckCooldownUseCase.kt` | Loads Settings + CooldownState from repos, delegates to EligibilityService | **New** — done |
| `domain/usecase/RecordCooldownTriggerUseCase.kt` | Saves current timestamp as last trigger | **New** — done |
| `domain/repository/CooldownRepository.kt` | Extended: +loadLastTriggerEpochMillis(), +saveLastTriggerEpochMillis() | **Modified** — done |
| `data/repository/CooldownRepositoryImpl.kt` | SharedPreferences impl of new methods | **Modified** — done |
| `domain/service/CooldownEligibilityServiceTest.kt` | 21 tests: schedule, daily reset, N-hour, boundaries, evaluation order | **New** — done |
| `domain/model/EligibilityResultTest.kt` | 6 tests: sealed class structure, block reasons | **New** — done |
| `domain/usecase/CheckCooldownUseCaseTest.kt` | 3 tests: Eligible/Blocked delegation, parameter passing | **New** — done |
| `domain/usecase/RecordCooldownTriggerUseCaseTest.kt` | 1 test: timestamp saved via repository | **New** — done |

## Design: Level 1 — Capabilities

1. **Daily reset cooldown** — Once the intervention fires, subsequent unlocks do not re-trigger it until a configurable daily reset time (default 00:00) passes.
2. **N-hour interval cooldown** — Once the intervention fires, subsequent unlocks are blocked until a configurable time interval (1 min – 24 hours) elapses from the last trigger.
3. **Schedule window** — The intervention only fires within a configurable active time window (start/end time); outside these hours, unlock events produce no intervention regardless of cooldown state.
4. **Global enabled toggle** — When the feature is disabled, no intervention ever fires, overriding all cooldown and schedule rules.
5. **Persistent cooldown state** — Cooldown state survives device reboot (SharedPreferences, UTC epoch millis), resilient to DST/timezone changes.

## Design: Level 2 — Components

### Component Table

| # | Component | Layer | Responsibility |
|---|-----------|-------|---------------|
| 1 | CooldownEligibilityService | Domain (Service) | Pure business logic: evaluates enabled flag, schedule window containment, and cooldown expiry against config type (DailyReset or NHourInterval). No I/O, no clock dependency — all inputs passed as parameters. Returns EligibilityResult |
| 2 | CheckCooldownUseCase | Domain (UseCase) | Loads Settings from SettingsRepository and last trigger timestamp from CooldownRepository. Delegates to CooldownEligibilityService. Returns EligibilityResult to caller |
| 3 | RecordCooldownTriggerUseCase | Domain (UseCase) | Saves current epoch millis as last trigger via CooldownRepository. Called when intervention is successfully shown |
| 4 | EligibilityResult | Domain (Value Object) | Sealed class: Eligible or Blocked(BlockReason). BlockReason enum: FEATURE_DISABLED, OUTSIDE_SCHEDULE_WINDOW, COOLDOWN_ACTIVE |
| 5 | CooldownRepository (interface) | Domain (Repository) | Contract for cooldown persistence. **Extended** with loadLastTriggerEpochMillis(), saveLastTriggerEpochMillis(). Existing clearLastTrigger() retained for SettingsUseCase compatibility |

### Existing Components Referenced

| # | Component | Layer | Source Feature |
|---|-----------|-------|---------------|
| R1 | SettingsRepository | Domain (Repository) | Settings UI — load()/save() Settings |
| R2 | Settings (model) | Domain (Model) | Settings UI — enabled, cooldownConfig, scheduleStart, scheduleEnd |
| R3 | CooldownConfig (sealed interface) | Domain (Model) | Settings UI — DailyReset, NHourInterval |
| R4 | CooldownRepositoryImpl | Data | Settings UI — SharedPreferences backing, extended with new methods |
| R5 | SettingsRepositoryImpl | Data | Settings UI — SharedPreferences backing |

### Dependency Diagram

```
Domain Layer:
  CheckCooldownUseCase ──→ SettingsRepository (load Settings)
  CheckCooldownUseCase ──→ CooldownRepository (load last trigger)
  CheckCooldownUseCase ──→ CooldownEligibilityService (pure eval)
  RecordCooldownTriggerUseCase ──→ CooldownRepository (save trigger)
  CooldownEligibilityService ──→ CooldownConfig (reads variant)
  CooldownEligibilityService ──→ EligibilityResult (returns)

Data Layer:
  CooldownRepositoryImpl : CooldownRepository (SharedPreferences)

Caller (unlock trigger):
  [AccessibilityService] ──→ CheckCooldownUseCase
  [AccessibilityService] ──→ RecordCooldownTriggerUseCase
```

Dependency direction: UI → Domain ← Data (implements)

### DDD Classifications

- **Value Objects**: EligibilityResult (sealed class, immutable, no identity). CooldownConfig (sealed interface, already defined). Settings (data class, already defined).
- **Entities**: None — no object with lifecycle identity tracking.
- **Aggregates**: None — no transactional invariants across multiple objects.
- **Domain Events**: None — cooldown state changes are not observed by other aggregates.
- **Domain Services**: CooldownEligibilityService — stateless, pure logic spanning multiple value objects.

### Component Layer Validation (Architecture)

| Check | Status |
|-------|--------|
| Business logic in domain layer (not ViewModel/Activity) | ✓ CooldownEligibilityService is pure domain — no Android imports |
| UseCase orchestrates, doesn't embed business rules | ✓ CheckCooldownUseCase loads data + delegates to service |
| Domain contains no outer-layer imports | ✓ All domain types are pure Kotlin |
| Data implements domain interfaces | ✓ CooldownRepositoryImpl implements CooldownRepository |
| Single responsibility per component | ✓ Service = evaluation, UseCase1 = check, UseCase2 = record |
| Domain testable without Android instrumentation | ✓ Service tests need no Android; UseCase tests mock repositories |

## Design: Level 3 — Interactions

### Flow 1: Unlock — Check Cooldown Before Intervention

```
UnlockTrigger (caller)     CheckCooldownUseCase    SettingsRepository    CooldownRepository    CooldownEligibilityService
       │                          │                       │                     │                        │
       │  invoke()                │                       │                     │                        │
       │─────────────────────────>│                       │                     │                        │
       │                          │  load()               │                     │                        │
       │                          │──────────────────────>│                     │                        │
       │                          │<──── Settings ────────│                     │                        │
       │                          │                       │                     │                        │
       │                          │  loadLastTriggerEpochMillis()               │                        │
       │                          │───────────────────────────────────────────>│                        │
       │                          │<──── Long? ────────────────────────────────│                        │
       │                          │                       │                     │                        │
       │                          │  evaluate(enabled, cooldownConfig,         │                        │
       │                          │    scheduleStart, scheduleEnd,             │                        │
       │                          │    lastTrigger, nowEpochMillis)            │                        │
       │                          │───────────────────────────────────────────────────────────────────>│
       │                          │<── EligibilityResult ──────────────────────────────────────────────│
       │<── EligibilityResult ────│                       │                     │                        │
       │                          │                       │                     │                        │
       │  [if Eligible:           │                       │                     │                        │
       │   launch Intervention,   │                       │                     │                        │
       │   record trigger]        │                       │                     │                        │
       │  [if Blocked: no-op]     │                       │                     │                        │
```

### Flow 2: Record Trigger After Intervention Launch

```
UnlockTrigger (caller)     RecordCooldownTriggerUseCase    CooldownRepository
       │                             │                           │
       │  invoke()                   │                           │
       │────────────────────────────>│                           │
       │                             │  saveLastTriggerEpochMillis(   │
       │                             │    System.currentTimeMillis())│
       │                             │──────────────────────────>│
       │                             │<──── Unit ────────────────│
       │<──── Unit ─────────────────│                           │
       │                             │                           │
  [then launch InterventionActivity] │                           │
```

### Data Flow Description

1. **Cooldown eligibility check** (read-only, on unlock):
   - `CheckCooldownUseCase.invoke()` → `SettingsRepository.load()` → `CooldownRepository.loadLastTriggerEpochMillis()` → `CooldownEligibilityService.evaluate()` with all parameters → returns `EligibilityResult`
   - Service evaluates in order: enabled → schedule window → cooldown expiry (short-circuit on first block)

2. **Trigger recording** (command, after eligible check passes):
   - `RecordCooldownTriggerUseCase.invoke()` → `CooldownRepository.saveLastTriggerEpochMillis(now)`
   - Called immediately before launching InterventionActivity

3. **Daily reset evaluation** (inside CooldownEligibilityService):
   - If lastTrigger is null → Eligible
   - Compute most recent reset time (today at resetTime; if now is before today's reset, use yesterday's reset)
   - If lastTrigger < mostRecentReset → Eligible (reset has occurred since last trigger)
   - If lastTrigger >= mostRecentReset → Blocked(COOLDOWN_ACTIVE)

4. **N-hour interval evaluation** (inside CooldownEligibilityService):
   - If lastTrigger is null → Eligible
   - If (lastTrigger + intervalHours * 3_600_000) <= now → Eligible (interval has elapsed)
   - If (lastTrigger + intervalHours * 3_600_000) > now → Blocked(COOLDOWN_ACTIVE)

5. **Schedule window check** (inside CooldownEligibilityService):
   - If start <= end (normal): now in [start, end] → within window
   - If start > end (overnight): now >= start OR now <= end → within window
   - Outside window → Blocked(OUTSIDE_SCHEDULE_WINDOW)

### Interaction Validation (Architecture & DDD)

| Check | Status |
|-------|--------|
| UI → Domain direction respected | ✓ UseCase calls repository interface — data layer implements it |
| State-changing ops go through domain + Repository | ✓ recordTrigger saves via CooldownRepository |
| Read-only queries use Repository pattern | ✓ eligibility check loads via SettingsRepository + CooldownRepository |
| Domain events for cross-aggregate communication | N/A — cooldown is internal to this feature |
| Data crossing boundaries uses simple structures | ✓ EligibilityResult sealed class, Long?, Settings data class |
| Fallback paths documented | ✓ All block reasons clearly distinguished via BlockReason enum |

## Design: Level 4 — Contracts

### Package: `com.example.promptly.domain.model`

**EligibilityResult.kt** — Sealed class representing cooldown eligibility outcome.

```kotlin
package com.example.promptly.domain.model

sealed class EligibilityResult {
    data object Eligible : EligibilityResult()
    data class Blocked(val reason: BlockReason) : EligibilityResult()
}

enum class BlockReason {
    FEATURE_DISABLED,
    OUTSIDE_SCHEDULE_WINDOW,
    COOLDOWN_ACTIVE
}
```

### Package: `com.example.promptly.domain.repository`

**CooldownRepository.kt** — Extended contract for cooldown persistence.

```kotlin
package com.example.promptly.domain.repository

interface CooldownRepository {
    suspend fun loadLastTriggerEpochMillis(): Long?
    suspend fun saveLastTriggerEpochMillis(timestamp: Long)
    suspend fun clearLastTrigger()
}
```

### Package: `com.example.promptly.domain.service`

**CooldownEligibilityService.kt** — Pure domain service evaluating cooldown eligibility.

```kotlin
package com.example.promptly.domain.service

import com.example.promptly.domain.model.BlockReason
import com.example.promptly.domain.model.CooldownConfig
import com.example.promptly.domain.model.EligibilityResult
import java.time.LocalTime

class CooldownEligibilityService {

    fun evaluate(
        enabled: Boolean,
        cooldownConfig: CooldownConfig,
        scheduleStart: LocalTime,
        scheduleEnd: LocalTime,
        lastTriggerEpochMillis: Long?,
        nowEpochMillis: Long
    ): EligibilityResult
}
```

### Package: `com.example.promptly.domain.usecase`

**CheckCooldownUseCase.kt** — Orchestrates cooldown eligibility check.

```kotlin
package com.example.promptly.domain.usecase

import com.example.promptly.domain.model.EligibilityResult
import com.example.promptly.domain.repository.CooldownRepository
import com.example.promptly.domain.repository.SettingsRepository
import com.example.promptly.domain.service.CooldownEligibilityService

class CheckCooldownUseCase(
    private val settingsRepository: SettingsRepository,
    private val cooldownRepository: CooldownRepository,
    private val eligibilityService: CooldownEligibilityService
) {
    suspend operator fun invoke(): EligibilityResult
}
```

**RecordCooldownTriggerUseCase.kt** — Records intervention trigger timestamp.

```kotlin
package com.example.promptly.domain.usecase

import com.example.promptly.domain.repository.CooldownRepository

class RecordCooldownTriggerUseCase(
    private val cooldownRepository: CooldownRepository
) {
    suspend operator fun invoke()
}
```

### Contract Validation (DDD + Architecture)

| Check | Status |
|-------|--------|
| Every Level 3 interaction maps to at least one contract | ✓ Flow 1 → CheckCooldownUseCase, CooldownEligibilityService; Flow 2 → RecordCooldownTriggerUseCase |
| No new interactions introduced not agreed at Level 3 | ✓ |
| Value objects validate in constructor | ✓ EligibilityResult sealed class — no invalid states; BlockReason enum — finite set |
| Repository interfaces defined in domain layer | ✓ CooldownRepository in domain.repository |
| Infrastructure implements domain interfaces | ✓ CooldownRepositoryImpl in data.repository |
| Boundary data uses simple structures | ✓ EligibilityResult, Long?, Settings all cross layer boundaries safely |
| No implementation logic in contracts | ✓ Signatures only; no function bodies |

## Design Summary

### Components & Layer Assignments

| Component | Layer | Status |
|-----------|-------|--------|
| CooldownEligibilityService | Domain (Service) | **New** |
| CheckCooldownUseCase | Domain (UseCase) | **New** |
| RecordCooldownTriggerUseCase | Domain (UseCase) | **New** |
| EligibilityResult | Domain (Value Object) | **New** |
| CooldownRepository (interface) | Domain (Repository) | **Modified** — added 2 methods |
| CooldownRepositoryImpl | Data | **Modified** — implement new methods |

### Key Contracts

- **`EligibilityResult`** — sealed class: `Eligible`, `Blocked(reason)` with `BlockReason` enum (3 reasons).
- **`CooldownRepository.loadLastTriggerEpochMillis(): Long?`** / **`saveLastTriggerEpochMillis(timestamp: Long)`** — extended interface.
- **`CooldownEligibilityService.evaluate(...): EligibilityResult`** — pure domain service, all inputs as parameters.
- **`CheckCooldownUseCase.invoke(): EligibilityResult`** — orchestrates load + evaluate.
- **`RecordCooldownTriggerUseCase.invoke()`** — persists trigger timestamp.

### Architectural Constraints

- Dependencies flow inward: UI → Domain ← Data
- CooldownEligibilityService is stateless and pure — no I/O, no Android imports, no clock reference
- CheckCooldownUseCase coordinates two repositories (SettingsRepository + CooldownRepository)
- Trigger recording happens at launch time, not dismissal time
- UTC epoch millis for timestamp storage — DST/timezone resilient
- Schedule window supports overnight ranges (start > end)
- Cooldown state reset on settings change remains owned by SettingsUseCase

### Domain Model Decisions

- No entities or aggregates — cooldown is stateless evaluation against persisted values
- `EligibilityResult` sealed class over raw Boolean — exhaustive handling, explicit block reasons
- `CooldownEligibilityService` receives all inputs as parameters — no I/O coupling, fully testable
- `RecordCooldownTriggerUseCase` is independently callable — not coupled to InterventionUseCase

### Open Questions Resolved During Design

- Eligibility evaluation order → enabled → schedule window → cooldown expiry (short-circuit)
- Trigger recording timing → at launch, before InterventionActivity starts
- Schedule window overnight support → start > end means "active from start through midnight to end"
- Cooldown state reset ownership → remains in SettingsUseCase (not duplicated in RecordCooldownTriggerUseCase)
- Repository extension vs new repository → extend CooldownRepository (single persistence concern)

### Design Status

**Approved — ready for implementation**

Design complete at Level 4 (Contracts). Implementation is a separate concern handled by the code-forge skill. Run `/code-forge` to begin coding against this blueprint.

### Completion Decision

| Date | Decision | Reasoning | Alternatives Considered |
|------|----------|-----------|------------------------|
| 2026-06-19 | Design approved at Level 4. Blueprint complete ready for implementation | All 5 capabilities defined, 4 new + 2 modified components with layer assignments, 2 interaction flows documented, 5+ contracts specified | N/A — design complete |
