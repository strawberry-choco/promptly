---
project: Promptly
last_updated: 2026-06-21
---

# Requirements Index — Promptly

## Definitions

**Epic:** Named group of related features forming a coherent product area. One epic = meaningful increment of product value — not one feature, not entire product.

**Feature:** Complete, self-contained unit of product behavior. Independently designable and implementable.

---

## Epics

### App Configuration

Settings screen, onboarding flow, and service-loss recovery. Covers all setup and ongoing configuration surfaces.

| Feature | Summary | Status | Priority | Depends On |
|---|---|---|---|---|
| [Settings UI](features/settings-ui.md) | Main settings screen with all configurable parameters | draft | P0 | — |
| [Onboarding & Permissions](features/onboarding-permissions.md) | First-launch accessibility service setup flow | draft | P1 | Settings UI |
| [Service Loss Recovery](features/service-loss-recovery.md) | Re-prompt when the accessibility service is disabled after onboarding | draft | P1 | Settings UI, Onboarding & Permissions |

### Intervention Engine

The core intervention behavior — display, redirect, cooldown/schedule, manual launch, and pinning removal.

| Feature | Summary | Status | Priority | Depends On |
|---|---|---|---|---|
| [Intervention Display](features/intervention-display.md) | Full-screen activity on unlock with Dismiss button | draft | P0 | — |
| [Cooldown Schedule](features/cooldown-schedule.md) | Cooldown and schedule window rules that gate intervention firing | draft | P1 | Intervention Display |
| [Target App Redirect](features/target-app-redirect.md) | Auto-redirect to a configured app after intervention display | draft | P1 | Intervention Display |
| [Manual Intervention Launch](features/manual-intervention-launch.md) | Button in settings to launch the intervention immediately | draft | P1 | — |
| [Remove Intervention Pinning](features/remove-intervention-pinning.md) | Remove lock task pinning from the intervention activity | draft | P1 | — |
