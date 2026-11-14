# F05 — Userscript Engine & Manager (PDA-compatible)
Phase: 1B | Depends on: F04, F02 | Package: `core/scripts`, `feature/scripts`

## Purpose
Run Tampermonkey-style userscripts on torn.com pages inside the in-app browser, in a way that existing Torn PDA-compatible scripts can work.

## Script format
Standard metadata block `// ==UserScript== ... // ==/UserScript==`. Supported keys: `@name @namespace @version @description @match @exclude @run-at @grant @connect`. Ignore unknown keys. `@require` and `@resource` are NOT supported (show a warning at install; script may fail). `@run-at`: `document-start`, `document-end` (default), `document-idle`.
Match patterns: Tampermonkey-style (`https://www.torn.com/*`). Only patterns whose host is in `Provider.allowedWebHosts` are valid; others are ignored with a warning.

## Injection
- `document-start`: `WebViewCompat.addDocumentStartJavaScript` (requires `WebViewFeature.DOCUMENT_START_SCRIPT`; allowed origin rules = provider hosts). If unsupported: inject at `onPageStarted` best-effort and warn in the manager.
- `document-end` / `document-idle`: `evaluateJavascript` from `onPageFinished` (idle = post a delayed task ~500 ms).
- Each script runs inside an IIFE with the GM shim; errors are caught and written to `ScriptLogEntity`.
- A global **Kill switch** ("Disable all scripts") is always visible in the browser menu; when on, nothing is injected.

## GM API surface (shim)
`GM_getValue, GM_setValue, GM_deleteValue, GM_listValues, GM_addStyle, GM_log, GM_info, GM_registerMenuCommand (list in menu), GM_xmlhttpRequest, unsafeWindow (= window)` plus promise versions `GM.getValue/setValue/deleteValue/listValues/xmlHttpRequest/info`. Storage = `ScriptKvEntity` (per script, per account).

## Native bridge (cross-origin requests)
- Transport: `WebViewCompat.addWebMessageListener` on a single named object (e.g., `__crowd`), `allowedOriginRules` = provider hosts ONLY. No `addJavascriptInterface`.
- `GM_xmlhttpRequest`/`GM.xmlHttpRequest` and PDA helpers `PDA_httpGet(url, headers?)` / `PDA_httpPost(url, headers?, body)` (VERIFY the exact PDA contract against Torn PDA's docs) are executed natively with OkHttp.
- Restrictions (enforced natively): https only; destination host must be in the script's `@connect` list (or equal to `api.torn.com`); max response 2 MB; timeout 20 s; **hosts `torn.com`/`www.torn.com` (non-API pages) are BLOCKED** from the bridge (C-02); requests to `api.torn.com` pass through RateLimiter class SCRIPT (F02) and return an error object `{error:"rate_limited"}` when throttled; the user's API key is never visible to the script except through the placeholder rule below.
- **API key placeholder (PDA style)**: if the script's `allowApiKey` flag is ON and the source contains the literal placeholder `###PDA-APIKEY###` (VERIFY the exact token against PDA docs), replace it with the active account's key at injection time. Default OFF per script; the install dialog shows the flag and warns that the script can read the key. The key is only substituted into scripts injected on allowed hosts.

## Manager UI (Scripts screen)
- List: name, version, matches, enabled toggle, grants badges (`API key`, `network`), last error.
- Add: paste source, or install from URL (https). Install dialog shows: metadata summary, matches, grants, connects, `allowApiKey` switch, a full-source preview, and a warning: "You are responsible for scripts you install. Torn forbids automation."
- Edit source (plain text editor), delete (also deletes KV + logs), "Clear storage", "View log" (last 200 lines).
- **No automatic updates and no background downloads** (D-05). "Check for update" is a manual button that re-fetches the install URL only when pressed.
- Scripts belong to an account (`accountId`).

## Edge cases
- Script throws: caught, logged, shown as "last error"; never crashes the page/app.
- Huge script (> 500 KB): refuse at install.
- Two scripts modify the same DOM: not our problem; keep isolation per IIFE.
- Page navigations (SPA): inject on each full page load; SPA route changes are the script's responsibility.

## Acceptance criteria
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

## Out of scope
Script marketplace/browser, `@require`, auto-update, Greasemonkey 3 legacy APIs beyond the list.
