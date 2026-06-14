---
project: Promptly
last_updated: 2026-06-14
---

# Requirements Index — Promptly

## Definitions

**Epic:** A named group of related features forming a coherent product area or capability.

**Feature:** A complete, self-contained unit of product behavior that is independently designable and implementable.

---

## Epics

### Intervention Engine
Controls the core behavior — detecting phone unlock, displaying a full-screen Intervention, handling Dismiss and Redirect flows, and enforcing cooldown rules so the intervention only appears once per configured window.

| Feature | Summary | Status | Priority | Depends On |
|---|---|---|---|---|---|
| [Intervention Display](features/intervention-display.md) | Full-screen non-closable activity on phone unlock with Dismiss button | draft | P0 | — |
| [Target App Redirect](features/target-app-redirect.md) | Launch configured app from Intervention | draft | P1 | Intervention Display |
| [Cooldown Schedule](features/cooldown-schedule.md) | Time-of-day windows and daily/N-hour reset after trigger | draft | P1 | Intervention Display |

### App Configuration
Provides the first-launch onboarding flow (Accessibility Service setup), the in-app Settings screen for managing all parameters, and the permissions infrastructure to keep the service running.

| Feature | Summary | Status | Priority | Depends On |
|---|---|---|---|---|---|
| [Settings UI](features/settings-ui.md) | In-app configuration for all intervention parameters | draft | P0 | — |
| [Onboarding & Permissions](features/onboarding-permissions.md) | First-launch flow plus accessibility service setup | draft | P1 | Settings UI |
| [Service Loss Recovery](features/service-loss-recovery.md) | Post-setup detection of service disablement and re-enable flow | draft | P1 | Settings UI, Onboarding & Permissions |

## Glossary

| Term | Definition |
|---|---|
| Intervention | A full-screen, non-dismissable activity that appears after the user unlocks the phone. Exitable only via Dismiss or Redirect. |
| Dismissing | Pressing the close button on the Intervention to remove it and return to normal phone usage. |
| Redirecting | Auto-launching the configured target app from the Intervention, after which the Intervention closes. |
| Cooldown | The behavior where, once an Intervention has been triggered, it will not appear again on subsequent unlocks until a reset event (daily reset or N-hour interval expiry). |
| Settings | The configuration UI of the Promptly app where parameters are managed. |
| Lock Task Mode | Android kiosk-mode API (`startLockTask`/`stopLockTask`) used to prevent the Intervention from being bypassed via Back, Home, or Recents. |

## Source Materials

| Document | Type | Features Derived |
|---|---|---|
| CONTEXT.md | Domain glossary + technical constraints | All features — provided domain terminology and implementation assumptions |
| User requirements list | Product wishlist | All features — defined the 5 capability areas |

## Deferred Items

Content from source materials intentionally not mapped to a feature in this cycle.

- CONTEXT.md "Technical constraints" section — specific implementation decisions (Accessibility Service broadcast receiver, Lock Task Mode, SharedPreferences, PackageManager query) — incorporated into feature specs as assumptions and implementation notes. Not standalone features.
- Per-app intervention rules (out of scope per Intervention Display) — deferred for future consideration.
