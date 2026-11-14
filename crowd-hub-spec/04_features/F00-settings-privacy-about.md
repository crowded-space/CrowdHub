# F00 — Settings, Privacy, About, Developer tools
Phase: 1A | Depends on: F01, F02 | Package: `feature/settings`

## Purpose
Central place for preferences, transparency about data, legal disclaimer, and a safe way to collect real API samples.

## Behavior
1. **Settings** (list): Notifications (per alert type toggle + lead times), Watch Mode (default duration 1/2/4/6 h), Data (clear caches, clear stock history), Reliability (info page + deep link to system battery settings), Advanced (Profiles — F01), Developer (hidden until tapping the version row 7 times).
2. **Privacy screen**: shows the data inventory summary (from `06_guardrails/security-and-privacy.md`) and the Torn API disclosure block (storage, sharing, purpose, key storage, key sharing, access level) from `torn-rules-and-compliance.md`.
3. **About**: wordmark, version, "Unofficial app. Not affiliated with or endorsed by Torn." and a reminder that the user is responsible for following Torn's rules (accounts, scripts, API).
4. **Developer > API Probe**: pick a selection (dropdown of the selections in `torn-api-reference.md`), optional path params (id), run once with USER priority via F02, show the raw JSON **with secrets redacted** and a Copy button. Shows rate-limiter state (calls in the last 60 s per priority). Also shows last-response HTTP `Date` header and the clock offset.
5. Preferences are stored in DataStore (non-secret).

## Edge cases
- Probe on an INVALID account: show the error, do not call.
- Probe output larger than ~200 KB: truncate display, still allow copy of the first 200 KB.

## Acceptance criteria
- [ ] F00-AC-01 Settings screen lists Notifications, Watch Mode, Data, Reliability, Advanced; Developer appears only after 7 taps on the version row.
- [ ] F00-AC-02 Privacy screen shows the Torn API disclosure block exactly as specified.
- [ ] F00-AC-03 About screen states the app is unofficial/not affiliated with Torn and shows the version.
- [ ] F00-AC-04 API Probe runs via the global RateLimiter (priority USER) and the output never contains the API key.
- [ ] F00-AC-05 API Probe shows clock offset and the rate-limiter usage counters.
- [ ] F00-AC-06 All preferences persist across app restarts and contain no secrets.

## Out of scope
Theme switching, language selection, export/import (Phase 3).
