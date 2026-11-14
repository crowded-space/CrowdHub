# PHASE 1A — Core (profile, API, alerts, dashboard, travel, stocks)

## Goal
A working, installable app that: onboards a Torn API key into a profile container, shows the dashboard, fires reliable local timers, tracks your own flights, and charts stocks with price alerts.

## Features in scope
F00 Settings/Privacy/About/Developer · F01 Profile container & vault · F02 API manager · F03 Alert engine · F06 Dashboard · F07 Own travel · F08 Stock market.

## Explicitly OUT of scope (do not build, do not stub in the UI)
Browser, WebView, userscripts, auto-login, FFScouter, war, flight tracker for others, chain/targets, attack shortcut, widgets, portfolio, light theme, export/import.
Bottom bar in 1A shows only: Dashboard, Stocks. Overflow: Settings, About (Profiles only under Settings > Advanced).

## Prerequisites (check in Step 0; report what is missing)
`_samples/`: user-basic, user-bars, user-cooldowns, user-travel, user-money, user-notifications, torn-stocks. Missing sample = implement nullable + VERIFY + tell the user.

## Build order (finish each step before the next)
1. **Skeleton**: package layout (`package-structure.md`), Crow theme (`branding-crow-theme.md`), `CrowdWordmark`, navigation shell with the two tabs, `AppContainer`, notification channels, manifest flags/permissions (`tech-constraints.md`).
2. **Database & vault**: Room (`data-model.md` Phase 1A entities, exportSchema), `KeystoreCipher`, `SecretVault`, `SettingsStore`, `CrowLog`.
3. **F01**: onboarding flow, account validation hook (stub the network call only until step 4 is done, then wire for real), Profiles (advanced), account screen, wipe logic.
4. **F02**: HttpClientFactory, RateLimiter, ResponseCache, Clock, ApiResult/ApiError, TornApi + TornProvider, DTOs from samples, redaction, unit tests (limiter, clock, cache/single-flight, error handling).
5. **F06 + F07**: Dashboard cards + Travel card + countdown component + states.
6. **F03**: AlertDeduper + Scheduler + AlarmReceiver + BootReceiver + PollWorker + WatchModeService; sources `torn.bars`, `torn.status`, `torn.travel`, `torn.cooldowns`; unit tests for the deduper.
7. **F08**: StockRepository (snapshots), list, detail chart (Canvas), alert editor, `StockAlertEvaluator` + tests, `torn.stocks` AlertSource, cleanup job, Watch Mode integration.
8. **F00**: Settings, Privacy, About, Developer (API Probe), Reliability page.
9. **Audit**: run the self-check, produce the Audit Report with evidence for every ID in `07_checklists/phase-1A-checklist.md`.

## Exit criteria
All checklist IDs PASS (or an honest NOT DONE with reason), app compiles and runs, no guardrail violations.

## Reminders (repeat of the critical rules)
Only this phase. No guessing of API fields. All API calls via RateLimiter. No secrets in logs/UI. No Firebase/Gemini/analytics. Ideas -> `BACKLOG_SUGGESTIONS.md`.
