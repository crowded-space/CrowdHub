# F09 — FFScouter Connector (consent, enable/disable, repository)
Phase: 2 | Depends on: F01, F02 | Package: `connector/ffscouter`
Read first: `03_providers/torn/connector-ffscouter.md`.

## Purpose
Provide Fair Fight (FF) and estimated battle stats (BS) for players, on demand, with explicit consent.

## Behavior
1. **Enable flow** (Account screen > FFScouter row > Enable): consent dialog with the exact points: (a) your Torn API key will be sent to ffscouter.com; (b) FFScouter says it stores the key and restricts access to its owners (show the current wording from the connector doc; VERIFY); (c) you must be registered at ffscouter.com; (d) you can disable anytime. Buttons: "Open ffscouter.com", "I understand, enable", "Cancel". Consent is stored per account (`ConnectorLinkEntity.consentAt`).
2. **Test**: after enabling, run a `get-stats` call for the user's own player id; map errors (401/403 -> "Not registered at FFScouter").
3. **Repository** `FfRepository`: `suspend fun stats(ids: List<Long>, forceRefresh: Boolean): Map<Long, FfStats>`; batches per `FfDefaults.batchSize` (default 50); caches 1 h in `FfCacheEntity`; uses `FfRateLimiter` (default 10/min); returns cached stale values on errors with a Stale flag.
4. **UI components** (reused by F10/F12): `FfBadge(fairFight)`, `BsText(bsEstimate/human)`, both labeled "est.".
5. **Disable**: confirm; delete `FfCacheEntity` rows; set `enabled=false`. When disabled, FF/BS UI and filters are hidden everywhere.
6. The Torn key is read from SecretVault at call time; never stored elsewhere; never logged.

## Edge cases
- Player missing from FFScouter response: show "—" (no FF) and keep them in lists.
- Network/5xx: show cached data (Stale) or "—".
- Account switched/deleted: cache rows removed.

## Acceptance criteria
- [ ] F09-AC-01 No FFScouter request is ever made before consent (verify by search for the base URL usage).
- [ ] F09-AC-02 Consent dialog contains all four points and is stored per account with a timestamp.
- [ ] F09-AC-03 `FfRepository` batches, caches (1 h), rate-limits (10/min default) and returns stale data on error.
- [ ] F09-AC-04 401/403 errors produce the "Not registered at FFScouter" message with a button to open ffscouter.com.
- [ ] F09-AC-05 Disabling deletes the cache and hides FF/BS UI everywhere.
- [ ] F09-AC-06 DTOs are built from `_samples/ffscouter-*.json`; unknown/missing fields are handled (nullable).
- [ ] F09-AC-07 The key never appears in logs/UI; requests use HTTPS only.

## Out of scope
FFScouter premium endpoints, war-room integration, auto-registration.
