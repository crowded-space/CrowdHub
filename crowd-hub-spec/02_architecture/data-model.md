# Data Model (Room) — schema v1 (Phase 1A), extended per phase with migrations

Rules: `exportSchema = true`. NO `fallbackToDestructiveMigration`. Each phase that changes the schema bumps the version and ships a `Migration`. All IDs are `String` (UUID) unless stated. Timestamps are epoch milliseconds (`Long`) unless the column name ends with `Sec`.

## Phase 1A entities
```
ProfileEntity(id PK, name, createdAt, isActive)
ProviderAccountEntity(id PK, profileId FK->Profile CASCADE, providerId "torn", externalId /*player id*/ ?, displayName ?,
                      status [UNKNOWN|VALID|INVALID|PAUSED], accessLevel ?, lastValidatedAt ?, createdAt)
ConnectorLinkEntity(id PK, accountId FK->ProviderAccount CASCADE, connectorId "ffscouter", enabled, consentAt ?, UNIQUE(accountId, connectorId))
SecretBlobEntity(alias PK, iv BLOB, ciphertext BLOB, updatedAt)
ScheduledAlertEntity(dedupeKey PK, accountId, channelId, title, body, fireAt, deepLink ?, state [SCHEDULED|FIRED|CANCELLED], updatedAt)
StockSnapshotEntity(id PK auto, stockId Int, ts, price Double)           INDEX(stockId, ts)
StockWatchEntity(id PK, accountId, stockId Int, aboveTarget Double?, belowTarget Double?, pctMove Double?, pctWindowMin Int?, repeating Boolean, cooldownMin Int, armed Boolean, lastTriggeredAt ?, enabled)
CacheEntryEntity(key PK, body TEXT, fetchedAt, ttlMs)                    // optional persistent cache for stale display
```
`StockSnapshotEntity` is not account-scoped (stock prices are public data); retention: max 1 row per stock per 60 s; prune > 30 days; rows older than 48 h downsampled to 15-min buckets by a daily cleanup.

## Phase 1B additions
```
UserscriptEntity(id PK, accountId, name, namespace ?, version ?, description ?, source TEXT, matches (JSON list), excludes (JSON list),
                 runAt [DOCUMENT_START|DOCUMENT_END|DOCUMENT_IDLE], grants (JSON list), connects (JSON list),
                 allowApiKey Boolean, enabled, installedFromUrl ?, installedAt, updatedAt)
ScriptKvEntity(scriptId FK CASCADE, key, value TEXT, PRIMARY KEY(scriptId, key))
ScriptLogEntity(id PK auto, scriptId, ts, level, message)   // keep last 200 per script
```
(Credentials for auto-login go to `SecretVault`, not Room.)

## Phase 2 additions
```
FfCacheEntity(playerId Long PK, fairFight Double?, bsEstimate Long?, bsHuman String?, lastUpdatedSec Long?, fetchedAt)
MemberSnapshotEntity(factionId Long, playerId Long, ts, state, descriptionHash, untilSec ?, planeType ?, lastActionStatus ?, lastActionSec ?, level ?, PRIMARY KEY(factionId, playerId))
   // only the LATEST snapshot per member is persisted; older ones live in memory for the diff
StakeoutEntity(accountId, playerId Long, alertHospital, alertLanding, alertOnline, alertOkay, createdAt, PRIMARY KEY(accountId, playerId))
FlightObservationEntity(id PK auto, playerId Long, direction [OUT|RETURN], place, planeType ?, prevPollAt ?, detectedAt ?, landedPrevPollAt ?, landedDetectedAt ?, observedMinMs ?, observedMaxMs ?, inferredBook Boolean?)
PlayerFlightProfileEntity(playerId Long PK, hasBook Boolean?, lastObservedAt ?)
PlaneTypeMapEntity(apiValue PK, category [STANDARD|AIRSTRIP|WLT|BUSINESS|UNKNOWN], source [DEFAULT|USER])
TargetEntity(accountId, playerId Long, name ?, note ?, source [HOSPITAL_PAGE|FFSCOUTER|MANUAL|WAR], addedAt, PRIMARY KEY(accountId, playerId))
HospitalCaptureEntity(accountId, playerId Long, name, level ?, remainingSecAtCapture Long?, capturedAt, PRIMARY KEY(accountId, playerId))
```

## Deletion semantics
- Delete Profile -> cascade accounts/links; then `SecretVault.deleteByPrefix("acct:<id>:")`; cancel alarms/workers; clear web profile (1B); delete account-scoped rows in all tables above.
- Disable FFScouter -> delete `FfCacheEntity` rows; keep the secret in the vault (it's the Torn key).
