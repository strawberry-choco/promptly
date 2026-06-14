---
feature: Onboarding & Permissions
epic: App Configuration
status: draft
priority: P1
depends_on:
  - Settings UI
personas:
  - Distracted Professional — wants to set up quickly with minimal friction
  - Focus Seeker — may pause if unclear what permissions are needed
  - Parent or Guardian — may be skeptical of accessibility permission; needs clear explanation
source_docs:
  - CONTEXT.md (domain glossary + technical constraints)
  - User requirements list
---

# Onboarding & Permissions

## Problem Statement

Promptly's core functionality depends on the Android Accessibility Service — without it, unlock events cannot be detected and the intervention never fires. A first-time user opening the app has no guidance on what to enable or why. The permission itself (Accessibility Service) is sensitive and requires user trust — without a clear explanation and guided flow, users will either not complete setup or abandon the app entirely.

## User / Personas

All new users need onboarding regardless of persona. The explanation must be clear enough to overcome the natural skepticism around accessibility permissions.

## Scope

**In scope:**
- First-launch detection: show Onboarding screen only on first open
- Onboarding screen with explanation of why Accessibility Service is needed
- Button to directly open system accessibility settings
- "Skip" option on Onboarding screen → navigates to Settings with persistent nag
- Detection of when the service has been enabled (Settings.Secure query + lifecycle callbacks)
- Post-enablement success confirmation and transition to Settings screen
- Persistent banner in Settings screen until service is enabled

**Out of scope:**
- Video tutorials or animations
- Multi-page walkthrough or swiper
- Account creation or profile setup
- Analytics or data collection consent
- Rate-the-app or feedback prompt
- Cloud backup or restore of settings

## Boundary Conditions

- User force-quits the app during onboarding: first-launch flag is not set until completion, so onboarding shows again
- User enables the service but never returns to the app: intervention works from the unlock handler regardless
- User reinstalls the app: first-launch detection resets (SharedPreferences cleared on uninstall)
- User skips onboarding and never enables the service: banner persists on every Settings open until service is enabled
- User skips onboarding, then enables service later: skip becomes moot; banner dismissed, success flow triggered

## Assumptions

- `Settings.ACTION_ACCESSIBILITY_SETTINGS` intent opens the correct system screen on all target devices (API 26+)
- Service enablement detected via `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` on activity resume; lifecycle callbacks (`onServiceConnected()`) used as secondary signal
- First-launch flag stored in SharedPreferences is sufficient (no server-side tracking)
- Skip preference stored in SharedPreferences survives config changes but not reinstalls

## Scenarios

### Scenario 1: First launch — onboarding screen appears
User opens Promptly for the first time after installation.

**Acceptance Criteria:**
- Given the user opens the Promptly app for the first time,
- when the app launches,
- then the Onboarding screen is displayed (not the Settings screen).
- Given the Onboarding screen is displayed,
- when the user reads the content,
- then it includes: an explanation of what Accessibility Service access enables, and a single action button labeled "Open Accessibility Settings."

### Scenario 2: User navigates to system accessibility settings
User taps the setup button and is taken to the system settings.

**Acceptance Criteria:**
- Given the Onboarding screen is displayed,
- when the user taps "Open Accessibility Settings,"
- then the system Accessibility settings page opens via `startActivity(Settings.ACTION_ACCESSIBILITY_SETTINGS)`.
- Given the system settings are open,
- when the user locates and enables "Promptly" in the installed service list,
- then the service binding is established and `onServiceConnected()` is called.

### Scenario 3: User returns after enabling the service
User successfully enabled Promptly and returns to the app.

**Acceptance Criteria:**
- Given the user enabled the Promptly accessibility service,
- when they return to the Promptly app,
- then a success confirmation is displayed ("Promptly is ready to go").
- Given the confirmation is shown,
- when the user taps "Continue,"
- then the Settings screen opens and the first-launch flag is set.
- Given the first-launch flag is set,
- when the user opens Promptly again,
- then the Settings screen opens directly (no onboarding).

### Scenario 4: User leaves accessibility settings without enabling
User opens accessibility settings but does not enable Promptly, then returns to the app.

**Acceptance Criteria:**
- Given the user was directed to system accessibility settings,
- when they return to Promptly without enabling the service,
- then the Onboarding screen remains displayed (state preserved, not reset).
- Given the user has not enabled the service,
- when they close and reopen the app,
- then the Onboarding screen is still displayed.

### Scenario 5: User skips onboarding
User taps "Skip" on the onboarding screen to explore the app without enabling the service.

**Acceptance Criteria:**
- Given the Onboarding screen is displayed,
- when the user taps "Skip,"
- then the Settings screen opens and the first-launch flag is set.
- Given the Settings screen is displayed and the service is not enabled,
- when the user views the Settings screen,
- then a persistent banner is shown: "Enable the Accessibility Service to start interventions."
- Given the banner is displayed,
- when the user taps the banner,
- then the system Accessibility settings page opens.
- Given the user later enables the service and returns to the app,
- when the app detects the service is active,
- then the banner is dismissed and the Settings screen operates normally.

## Implementation Notes

1. **First-launch detection** — SharedPreferences boolean flag; if not set, route to Onboarding activity; after successful setup, set flag and route to Settings.
2. **Onboarding screen UI** — Single-screen activity with explanation text, "Open Accessibility Settings" button, and "Skip" link; listen for service binding via `Settings.Secure` query on resume.
3. **Service enablement detection** — On activity resume, query `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` and check for Promptly's service ID. Use `AccessibilityService.onServiceConnected()` as secondary signal for immediate transition while in the accessibility settings.
4. **Skip + persistent banner** — Skip stores a flag; on every Settings screen resume while service is missing, check service state and show/hide the banner. Banner is a `Material3 Card` or `Snackbar` at the top of the Settings layout.

## Open Questions

- [x] **Detection method**: Use `Settings.Secure.getString(ENABLED_ACCESSIBILITY_SERVICES)` on activity resume as primary detection; lifecycle callbacks as secondary signal for immediate feedback while in accessibility settings. (Resolved: Settings.Secure)
- [x] **Skip option**: Add a "Skip" link on the onboarding screen. Shows persistent banner in Settings until service is enabled. Banner dismissed once service is detected. (Resolved: skip once, persistent nag)

## Links

- Epic index: [index.md](../index.md)
