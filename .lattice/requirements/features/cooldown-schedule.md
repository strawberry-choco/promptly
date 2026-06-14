---
feature: Cooldown Schedule
epic: Intervention Engine
status: draft
priority: P1
depends_on:
  - Intervention Display
personas:
  - Distracted Professional — wants the intervention only once per work session, not every unlock
  - Focus Seeker — wants the intervention to fire at the start of each deep-work block
  - Parent or Guardian — wants the intervention once per day, not nagging each unlock
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Cooldown Schedule

## Problem Statement

If the intervention fires on every unlock throughout the day, the user becomes numb to it or frustrated by the constant friction. The cooldown ensures the intervention appears at most once per configured window — enough to create a deliberate reset without becoming background noise. The schedule window additionally lets the user constrain when the intervention is allowed to appear at all, so it only fires during the hours it's actually wanted.

## User / Personas

Same personas as Intervention Display. Cooldown preferences differ:
- Distracted Professional: daily reset — one nudge per day is enough
- Focus Seeker: N-hour interval — aligns with pomodoro or study block cadence
- Parent or Guardian: daily reset with morning activation — one check-in per day

## Scope

**In scope:**
- Daily reset mode with configurable reset time (default 00:00)
- N-hour interval mode with configurable interval (1 minute – 1 week)
- Once triggered, subsequent unlocks do not trigger until the next reset
- Cooldown state persists across device reboots (SharedPreferences)
- Schedule window: configurable active hours (start/end time) outside which the intervention never fires
- Global enabled/disabled toggle that overrides all schedule and cooldown rules

**Out of scope:**
- Multiple schedule profiles or presets
- Per-day-of-week schedule exceptions
- Calendar integration or exception dates
- Adaptive or smart cooldown based on user behavior
- Location-based schedule rules

## Boundary Conditions

- Device reboot: cooldown state is persisted and survives reboot
- Clock change (DST, manual adjustment): cooldown state tracked via UTC epoch millis — unaffected by DST transitions. The repeated hour during fall-back is treated as a single continuous duration; no re-check on the second pass.
- Timezone change: eligibility re-evaluated on next unlock (UTC epoch comparison unaffected)
- Interval mode: minimum 1 minute, maximum 1 week via Settings enforcement
- Reset time equals current time exactly: eligible for trigger (reset is considered complete)

## Assumptions

- SharedPreferences is sufficient for cooldown state persistence (no database needed)
- The system clock is trustworthy (user has not intentionally manipulated it to bypass cooldown)
- The schedule window and cooldown settings are configured via the Settings UI feature
- Global enabled toggle takes precedence over all other settings

## Scenarios

### Scenario 1: Daily reset — blocks until next reset
Configured for daily reset at 00:00. Intervention triggers at 10:00. Subsequent unlocks that day do nothing; after midnight, the next unlock triggers again.

**Acceptance Criteria:**
- Given the cooldown type is "daily reset" with reset time 00:00,
- when the intervention triggers at 10:00,
- then subsequent unlocks before the next 00:00 do not trigger the intervention.
- Given the intervention was triggered at 10:00,
- when the system clock passes 00:00 and the user unlocks,
- then the intervention triggers again.
- Given the intervention triggered after reset,
- when the user unlocks again before the next 00:00,
- then the intervention does not trigger.

### Scenario 2: N-hour interval — blocks until interval expires
Configured for 4-hour interval. Intervention triggers at 10:00.

**Acceptance Criteria:**
- Given the cooldown type is "N-hour interval" with interval of 4 hours,
- when the intervention triggers at 10:00,
- then unlocks before 14:00 do not trigger the intervention.
- Given the cooldown was triggered at 10:00,
- when the user unlocks at 14:00 or later,
- then the intervention triggers again.
- Given the intervention triggered again at 14:00,
- when the user unlocks at 15:00,
- then the intervention does not trigger (cooldown window extended from 14:00).

### Scenario 3: Outside the active schedule — no intervention
Schedule window is set to 09:00–17:00. The user unlocks outside these hours.

**Acceptance Criteria:**
- Given the schedule window is 09:00 to 17:00,
- when the user unlocks at 08:00,
- then no intervention appears.
- Given the current time is outside the schedule window,
- when the user unlocks multiple times,
- then no intervention appears for any unlock.
- Given the current time moves into the schedule window (09:00),
- when the user unlocks,
- then the intervention appears (cooldown rules apply from this trigger).

### Scenario 4: Feature is disabled — no intervention ever
The global enabled toggle is set to Off.

**Acceptance Criteria:**
- Given the enabled toggle is Off,
- when the user unlocks at any time,
- then no intervention appears regardless of schedule or cooldown state.
- Given the enabled toggle is Off and the intervention did not appear,
- when the user toggles it back to On during a valid schedule window with no active cooldown,
- then the next unlock triggers the intervention normally.

## Implementation Notes

1. **Cooldown state persistence** — Store last trigger timestamp (epoch millis), cooldown type, and last reset timestamp in SharedPreferences; gate the unlock handler against these values.
2. **Daily reset logic** — Compare last trigger timestamp against the configured daily reset time; if current time is past reset and last trigger was before reset, allow trigger.
3. **N-hour interval logic** — Compare (lastTriggerTimestamp + intervalHours * 3600000) against current time; if current time is past, allow trigger.
4. **Schedule window check** — Verify current time falls within configured start/end hours before any cooldown check; if outside, skip intervention entirely.

## Open Questions

(none — all resolved)

## Links

- Epic index: [index.md](../index.md)
