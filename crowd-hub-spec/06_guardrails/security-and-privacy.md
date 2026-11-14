# Security & Privacy

## Data inventory
| Data | Stored where | Encrypted | Leaves device? | Notes |
|---|---|---|---|---|
| Torn API key | `SecretVault` (`secret_blob`) | Yes (Keystore AES-GCM) | `api.torn.com`; `ffscouter.com` only if consented | One key per ProviderAccount |
| Torn web user/pass (optional) | `SecretVault` | Yes | Only typed into torn.com login form | Opt-in (F04) |
| WebView cookies/storage | Android WebView profile storage | OS-level | Only to torn.com | Per-profile isolation when supported |
| Settings/preferences | DataStore (non-secret) | No | No | No secrets here |
| Stock history, caches, flights, targets | Room | No | No | Non-secret |
| Userscripts + GM storage | Room | No | No | Scripts are user-installed code |

## SecretVault design
- Interface: `put(alias, value)`, `get(alias): String?`, `delete(alias)`, `deleteByPrefix(prefix)`, `has(alias)`.
- Master key: AES-256, `AndroidKeyStore`, alias `crowdhub_master_v1`, `KeyGenParameterSpec` with GCM, no user-auth requirement in Phase 1-2.
- Per-secret random 12-byte IV; store `(alias, iv, ciphertext)` in Room table `secret_blob`.
- Alias convention: `acct:<accountId>:api_key`, `acct:<accountId>:web_user`, `acct:<accountId>:web_pass`.
- If decrypt fails (key invalidated): treat as "secret missing", prompt re-entry. Never crash.
- Do NOT use deprecated/third-party crypto wrappers; use platform `javax.crypto` + `KeyStore`.

## Masking & display
- API key shown as `••••••••<last4>`. No "reveal" button in Phase 1-2.
- No copy-to-clipboard for secrets. (Raw JSON in API Probe is redacted and may be copied.)
- Screens with secrets set `FLAG_SECURE`.

## Backups
- `android:allowBackup="false"`; also provide `dataExtractionRules`/`fullBackupContent` excluding everything.
- Settings export (Phase 3) must exclude secrets by default.

## Logging
- Use a `Log` wrapper `CrowLog` that redacts anything matching: the active API keys, `key=` query values, `Authorization` headers, password fields.
- No logging of request/response bodies in release. Debug builds may log bodies only through API Probe (user-initiated, redacted).

## Network
- HTTPS only. `usesCleartextTraffic=false` + network security config denying cleartext.
- OkHttp default certificate validation; no custom TrustManagers, no pinning in Phase 1-2.
- Timeouts: connect 15 s, read 20 s, call 30 s.

## WebView (F04) — summary
- JavaScript on (needed by torn.com), DOM storage on, file access off, content access off, no universal access from file URLs, mixed content blocked.
- Navigation guard: only provider `allowedWebHosts`; other hosts open in the external browser after a confirmation dialog.
- Bridges: `WebViewCompat.addWebMessageListener` with strict `allowedOriginRules`; never `addJavascriptInterface` on arbitrary origins.

## Threat notes (why the rules exist)
- Lost phone: vault + FLAG_SECURE reduce exposure; consider biometric lock in Phase 3.
- Malicious userscript: sandboxed to the WebView; native bridges restricted by `@connect`, https-only, rate-limited, and blocked for non-API torn.com hosts.
- Third-party leak: FFScouter receives the Torn key only after explicit per-profile consent and can be disabled at any time (deletes cache; the key stays in the vault).
