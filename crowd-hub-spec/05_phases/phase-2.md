# PHASE 2 — FFScouter, Ranked War Stakeout, Flight Tracker, Chain Target Assist

## Goal
Enemy faction monitoring with FF/BS estimates, flight ETA windows with confidence, hospital-based chain targets with filters, and one-tap attack page opening.

## Features in scope
F09 FFScouter connector · F10 Ranked war stakeout · F11 Flight tracker · F12 Chain target assist · F13 Attack shortcut.

## Explicitly OUT of scope
Anything in F14, FFScouter premium endpoints, automation of any kind.
New bottom tabs: War, Targets (total: Dashboard, Stocks, War, Targets, Browser).

## Prerequisites (Step 0 MUST verify; if missing -> ask, do not guess)
`_samples/`: faction-members (with Traveling/Hospital/Abroad/Okay members), faction-rankedwars, user-profile-other, ffscouter-get-stats, ffscouter-get-targets, hospital-page-rows.html, flight-data-notes.md (answers on `status.until` and `plane_image_type`).
Phases 1A and 1B completed and passed. Migration for schema v3 with exportSchema.

## Build order
1. **Contract check (no code)**: in Step 0 list the DTO fields you derived from each sample and every item still VERIFY.
2. **F09**: consent flow, FfRepository, limiter, cache, UI components.
3. **F13**: TornUrls.attack + AttackNavigator.
4. **F10 core**: rankedwars detection, members repository, `MemberDiffer` (+ tests), War screen (list/filters/sort), manual faction fallback.
5. **F11**: FlightTable, PlaneTypeMap, FlightEstimator (+ tests), PlayerFlightProfile learning, chips/alerts, Flight Debug screen.
6. **F10 stakeout**: starred members, alerts, Watch Mode integration, war-start detection.
7. **F12**: HospitalPageParser (+ tests with the sample), Capture button in the browser toolbar (visible only on the hospital URL), Targets screen, sources A/B/C, filters (+ tests), alerts.
8. **Audit** with evidence for `07_checklists/phase-2-checklist.md`.

## Exit criteria
All IDs PASS or honest NOT DONE; no non-API torn.com request without a user press; API budget respected (show limiter counters in the report from a test run if possible).

## Reminders
Estimates are labeled "est." with confidence. No automation. FFScouter only after consent. Do not invent plane-type mappings.
