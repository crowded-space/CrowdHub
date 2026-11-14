# Phase 1B Checklist (GENERATED — do not edit; edit the feature files and run tools/regenerate.py)

For each line the agent reports: `ID | PASS/FAIL/NOT DONE/NA | evidence: file#symbol | note`.
The user verifies a sample of PASS items by reading the cited code and running `07_checklists/qa-manual-test-plan.md`.

## Feature acceptance criteria

### F04 — In-App Browser & Persistent Session
- [ ] F04-AC-01 Browser loads torn.com; non-allowed hosts trigger the external-open dialog; `intent:` URLs are blocked.
- [ ] F04-AC-02 WebView hardening settings (all listed) are applied; no `addJavascriptInterface` on untrusted origins.
- [ ] F04-AC-03 After a manual login and a full app restart, the session is still logged in when Torn's cookies are persistent (document the observed behavior in the report).
- [ ] F04-AC-04 Per-account WebView profile used when MULTI_PROFILE is supported; fallback path works and shows the note otherwise; deleting the account wipes the web data.
- [ ] F04-AC-05 Auto-login is opt-in, stores credentials only in SecretVault, fills the form (no submit by default), stops on captcha/2FA/failed attempts, and never logs credentials.
- [ ] F04-AC-06 Session chip reflects Logged in/out via `Provider.isLoginUrl`.
- [ ] F04-AC-07 Quick links are editable and restricted to allowed hosts.
- [ ] F04-AC-08 No code path performs automatic page loads/refreshes/clicks on torn.com (verify by search).
- [ ] F04-AC-09 `BrowserNavigator.open(url)` works from other screens and opens the login page when logged out.

### F05 — Userscript Engine & Manager (PDA-compatible)
- [ ] F05-AC-01 Metadata parser handles the supported keys and ignores the rest (unit tested incl. `@match`/`@exclude` matching).
- [ ] F05-AC-02 Scripts inject at document-start / end / idle per `@run-at`; unsupported document-start falls back with a warning.
- [ ] F05-AC-03 Kill switch disables all injection immediately.
- [ ] F05-AC-04 GM_* and GM.* functions listed are implemented with persistent per-script storage.
- [ ] F05-AC-05 The bridge uses `addWebMessageListener` with strict origins; no `addJavascriptInterface`.
- [ ] F05-AC-06 `GM_xmlhttpRequest`/`PDA_http*` enforce https-only, `@connect` allowlist, size/time limits, and BLOCK non-API torn.com hosts.
- [ ] F05-AC-07 API calls from scripts to `api.torn.com` use RateLimiter class SCRIPT and never exceed 20/min.
- [ ] F05-AC-08 API-key placeholder substitution happens only when the per-script flag is ON, only on allowed hosts, and the install dialog warns about it.
- [ ] F05-AC-09 Install from URL/paste works with preview + confirmation; no automatic updates or background downloads exist (verify by search).
- [ ] F05-AC-10 Script errors are caught and logged (last 200 lines) without crashing.
- [ ] F05-AC-11 Deleting an account/script removes its scripts, KV storage and logs.

## Definition of Done
- [ ] DOD-01 Project compiles with the template's Gradle/Kotlin versions; no new warnings-as-errors disabled.
- [ ] DOD-02 App launches, survives rotation, background/foreground, and process death without crashing.
- [ ] DOD-03 No TODO/FIXME/placeholder logic reachable by the user.
- [ ] DOD-04 Search for `firebase|FirebaseApp|generativeai|GoogleGenerativeAI|crashlytics|analytics` -> no hits in code/Gradle.
- [ ] DOD-05 Search for `api.torn.com` -> hits only inside `provider/torn` (and the script bridge in 1B) and always via RateLimiter.
- [ ] DOD-06 Search for `GlobalScope`, `!!` on DTO fields, `Log.d(` with request/response bodies -> none.
- [ ] DOD-07 Manifest has `allowBackup=false`, cleartext disabled, only permitted permissions.
- [ ] DOD-08 No secret appears in logs, UI (unmasked), notifications, or backups.
- [ ] DOD-09 About screen states "unofficial / not affiliated with Torn".
- [ ] DOD-10 Every data screen has Loading/Error/Empty/Stale states.
- [ ] DOD-11 Countdowns use `Clock`.
- [ ] DOD-12 Unit tests exist for the pure classes listed in E-06 for the features of this phase (or the report explains why the template cannot run tests).

## Guardrail spot checks
- [ ] GR-01 A-01..A-09 process rules respected (only this phase, plan approved, no invented data, no renames).
- [ ] GR-02 B-01..B-06 platform rules respected (Kotlin/Compose, single module, no Firebase/Gemini/analytics, allowed libs only).
- [ ] GR-03 C-01..C-11 Torn compliance respected (API only or user-loaded page; no automation; limiter used).
- [ ] GR-04 D-01..D-06 security/privacy respected (vault only, redaction, FLAG_SECURE, WebView hardening).
- [ ] GR-05 E-01..E-09 code-quality rules respected (defensive parsing, Clock, states, no GlobalScope, tests for pure logic).
- [ ] GR-06 F-01..F-05 UX rules respected (Crow palette, a11y, text+color status, staleness).
