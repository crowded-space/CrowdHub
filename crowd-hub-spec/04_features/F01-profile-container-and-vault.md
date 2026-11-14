# F01 — Profile Container & Secret Vault
Phase: 1A | Depends on: core/db, core/vault | Package: `core/profile`, `core/vault`, `feature/profiles`

## Purpose
A generic container that groups everything about one account: provider account(s), secrets, connectors, preferences, data. Multi-profile ready, single-profile UX.

## Concepts
`Profile` (name) -> `ProviderAccount` (Torn, player id) -> `ConnectorLink` (FFScouter...). Secrets in `SecretVault` keyed `acct:<accountId>:<name>`.

## Behavior
1. **First launch -> Onboarding (exactly these steps, no carousel):**
   1. Welcome (wordmark + tagline "Watch the flock." + "Get started").
   2. Create profile: name field, default "Main".
   3. Add Torn account: explains "Use a Limited-access API key. You can revoke it anytime in Torn." + button "Open Torn API page" (opens the in-app/external browser to `https://www.torn.com/preferences.php#tab=api`, VERIFY) + key input (masked, paste-friendly, 16 chars expected but do not hard-fail on length) + "Validate".
   4. Validation: call `user/basic` (USER priority). On success store player id + name, status VALID. Then probe one Limited selection (`bars`); if error code 16 -> show "Key access level too low; create a Limited key" and do not mark the onboarding complete.
   5. Permissions in context: notifications (Android 13+), exact-alarm info (explain, deep-link to the system screen if not granted), skip allowed.
   6. Land on Dashboard.
2. **Active profile**: exactly one profile is `isActive`. All queries are scoped to the active profile's accounts. Phase 1 UI uses the first/only Torn account.
3. **Settings > Advanced > Profiles** (hidden by default; see C-11): list, create, rename, delete, switch active. Deleting asks for confirmation and performs the full wipe (see `data-model.md`).
4. **Account screen** (from Profiles or Settings): shows status rows — "Torn API: Valid (Limited)", "Web session" (Phase 1B; hidden in 1A), "FFScouter" (Phase 2; hidden in 1A). Actions: Replace key, Remove key, Delete account.
5. **Replace key**: validate new key first, then swap atomically; keep data. If the new key belongs to a different player id -> warn and require "Reset data for this account" confirmation.
6. **Vault** per `security-and-privacy.md`. API key masked everywhere (`••••••••1234`). `FLAG_SECURE` on onboarding step 3 and account screen.
7. Account status values: UNKNOWN, VALID, INVALID, PAUSED. When F02 marks a key INVALID/PAUSED the UI shows a persistent banner on Dashboard: "API key problem — tap to fix".

## Edge cases
- User kills the app mid-onboarding: resume at the last incomplete step; no half-created accounts with missing secrets (transaction: create account row only after successful validation).
- Keystore failure: show "Secure storage unavailable on this device" with a clear message; do not store the key in plaintext as fallback.
- Pasted key with whitespace/newlines: trim.

## Acceptance criteria
- [ ] F01-AC-01 First launch shows onboarding steps 1-6 in order; completing them lands on Dashboard.
- [ ] F01-AC-02 The API key is validated through `user/basic` and stored only in SecretVault (verify no plaintext copy in Room/DataStore/logs).
- [ ] F01-AC-03 Insufficient access level (code 16) blocks completion with a clear message.
- [ ] F01-AC-04 Key is masked in all UI; `FLAG_SECURE` is set on key entry/account screens.
- [ ] F01-AC-05 Profiles UI is reachable only via Settings > Advanced; create/rename/delete/switch work; deleting wipes secrets, rows, alarms.
- [ ] F01-AC-06 Replace key validates before swapping; different player id triggers a warning and a reset confirmation.
- [ ] F01-AC-07 INVALID/PAUSED account status shows a persistent banner and stops API calls for that account.
- [ ] F01-AC-08 Killing the app during onboarding resumes correctly; no account row without a secret.
- [ ] F01-AC-09 Keystore failure shows an error and never falls back to plaintext.

## Out of scope
Biometric lock, profile export/import, cloud sync, multi-provider UI.
