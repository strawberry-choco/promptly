---
feature: Settings UI
epic: App Configuration
status: draft
priority: P0
depends_on: []
personas:
  - Distracted Professional — sets up once, rarely changes
  - Focus Seeker — adjusts schedule window and cooldown per routine
  - Parent or Guardian — changes schedule and target app based on family needs
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Settings UI

## Problem Statement

The intervention has multiple configurable parameters — enabled state, schedule window, cooldown type, target app — but no way for the user to view or change them. Without a settings screen, the app is locked to its defaults and cannot adapt to different user needs or schedules.

## User / Personas

All personas need configuration. Settings are a one-time setup for most, but the Parent or Guardian persona adjusts settings more frequently based on changing daily routines.

## Scope

**In scope:**
- Global enabled/disabled toggle
- Cooldown type selector: Daily Reset or N-hour Interval
- Reset time picker (visible when Daily Reset is selected)
- Interval hours selector (visible when N-hour Interval is selected)
- Schedule window start and end time pickers
- Target app picker from a hardcoded curated list
- All settings persisted to SharedPreferences immediately on change

**Out of scope:**
- Visual customization of the intervention screen
- Analytics, usage stats, or history
- Multiple configuration profiles
- Import/export of settings
- Cloud sync
- Custom app targets beyond the curated list

## Boundary Conditions

- Cooldown type change resets the existing cooldown state (last trigger timestamp)
- Overnight schedule window (start > end) is supported and interpreted as crossing midnight
- Settings changes take effect immediately — no save button required
- App process death: all settings survive via SharedPreferences

## Assumptions

- SharedPreferences is sufficient for all configuration storage
- No authentication or multi-user support is needed
- The app is opened from the launcher to access Settings (no other entry point)
- The curated target app list is maintained in the app code (not remotely configured)

## Scenarios

### Scenario 1: User opens app, sees current settings
The app launches and displays all configuration options with their current persisted values.

**Acceptance Criteria:**
- Given the user opens the Promptly app from the launcher,
- when the app loads,
- then the Settings screen displays with sections: Enabled, Target App, Cooldown Type, Schedule Window.
- Given the user has previously configured settings,
- when the Settings screen loads,
- then each field shows its current value loaded from SharedPreferences.

### Scenario 2: User enables or disables Promptly
The user flips the global enabled toggle.

**Acceptance Criteria:**
- Given the enabled toggle is Off,
- when the user turns it On,
- then the setting is persisted to SharedPreferences immediately.
- Given Promptly is enabled,
- when the user turns it Off,
- then the setting is persisted and the current cooldown state (last trigger timestamp) is cleared.
- Given the enabled state was changed,
- when the user closes and reopens the app,
- then the toggle shows the new state.

### Scenario 3: User selects a target app
The user opens the target app picker and chooses from a hardcoded curated list.

**Acceptance Criteria:**
- Given the user taps the Target App picker row,
- when the picker opens,
- then a hardcoded list of curated apps is displayed.
- Given the list is displayed,
- when the user taps an app,
- then the app is selected, the picker closes, and the selection is persisted.
- Given an app was selected,
- when the Settings screen reappears,
- then the selected app name is displayed.
- Given the user selects "None" or clears the selection,
- when the Settings screen reappears,
- then "No target app" is displayed.

### Scenario 4: User configures cooldown type
The user switches between Daily Reset and N-hour Interval.

**Acceptance Criteria:**
- Given the user selects "Daily Reset" as the cooldown type,
- when the selection is made,
- then a time picker for the reset time (default 00:00) appears.
- Given the user selects "N-hour Interval" as the cooldown type,
- when the selection is made,
- then a number picker for hours (1–24, default 4) appears.
- Given the user changes the cooldown type,
- when the setting is persisted,
- then the previous cooldown state (last trigger timestamp) is reset.

### Scenario 5: User configures the schedule window
The user sets the active hours for the intervention.

**Acceptance Criteria:**
- Given the user edits the schedule start time,
- when a time is selected,
- then it is persisted as the start of the active window.
- Given the user edits the schedule end time,
- when a time is selected,
- then it is persisted as the end of the active window.
- Given the start time is set after the end time (e.g., 22:00 to 06:00),
- when the settings are saved,
- then the system interprets it as an overnight window.

## Implementation Notes

1. **Settings screen layout** — Build main screen with sections (Enabled, Target App, Cooldown, Schedule); load current values from SharedPreferences on create.
2. **Enabled toggle** — Immediate persistence and cooldown state reset on toggle.
3. **Cooldown type configuration** — Radio group with conditional TimePicker (Daily) or NumberPicker (Interval); reset cooldown state on change.
4. **Schedule window pickers** — Two TimePicker views for start and end; support overnight (start > end).
5. **Target app picker** — Dialog listing hardcoded app names + icons; persist selection as package name.

## Open Questions

- [x] What apps should be in the hardcoded curated list? **Resolved**: Anki and Medito.

## Glossary

- **Curated target app list** — The two apps the user can select as the intervention target: Anki (flashcard app) and Medito (meditation app). Defined in `TargetAppProvider.kt`.

## Links

- Epic index: [index.md](../index.md)
