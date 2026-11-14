# PHASE 1B — Browser, Persistent Session, Userscripts

## Goal
Open torn.com in-app with a login that survives restarts, optional auto re-login, and a PDA-compatible userscript engine with a manager UI.

## Features in scope
F04 In-app browser & session · F05 Userscript engine & manager.

## Explicitly OUT of scope
FFScouter, war, flight tracker, chain/targets, attack shortcut, script marketplace, auto-update of scripts, tabs.
New tab in the bottom bar: Browser. Overflow adds: Scripts.

## Prerequisites
Phase 1A is complete and passed its checklist. `_samples/login-page.html` provided (else mark selectors VERIFY and fail safe: auto-login disabled until provided). Migration for schema v2 (Phase 1B entities) with exportSchema.

## Build order
1. **Browser shell**: `CrowWebView` composable, hardening settings, `UrlGuard`, toolbar, quick links, `BrowserNavigator`.
2. **Session**: `WebProfileManager` (MULTI_PROFILE with fallback), cookie flush, `SessionMonitor` using `Provider.isLoginUrl`, account-screen row "Web session".
3. **Auto-login (opt-in)**: credentials in SecretVault, opt-in dialog, `TornSelectors` fill logic, attempt limiter, captcha/2FA stop. Default: fill only.
4. **Script storage**: Room v2 entities + migration, `UserscriptMetaParser` (+ unit tests), `ScriptRepository`.
5. **Injection**: GM shim, document-start/end/idle, kill switch.
6. **Bridge**: `addWebMessageListener`, GM/PDA native requests with restrictions, RateLimiter class SCRIPT, API-key placeholder rule.
7. **Manager UI**: list, install (paste/URL with preview + warning), edit, delete, logs, per-script storage clear.
8. **Audit** with evidence for `07_checklists/phase-1B-checklist.md`.

## Exit criteria
All checklist IDs PASS or honestly NOT DONE; no automatic page loads/clicks; no background downloads; app compiles/runs.

## Reminders
Only this phase. WebView hardening is mandatory. Never log credentials. Never use `addJavascriptInterface`. No automation of game actions.
