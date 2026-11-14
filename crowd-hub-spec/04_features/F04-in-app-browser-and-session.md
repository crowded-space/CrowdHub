# F04 — In-App Browser & Persistent Session
Phase: 1B | Depends on: F01 | Package: `core/browser`, `feature/browser`

## Purpose
Open torn.com inside the app with a login that survives restarts. The API key CANNOT log in to the website (API = data only); the web session is separate.

## Session layers (implement in this order)
1. **API-only mode (default)**: every non-browser feature works without any web login.
2. **Persistent cookies**: user logs in once manually; cookies persist in the WebView profile storage. Flush the cookie store on pause/stop. NOTE: session-only cookies (no expiry) may be dropped when the process restarts -> that is why layer 3 exists.
3. **Auto re-login (OPT-IN, off by default)**: user may store Torn username/password in `SecretVault` (`acct:<id>:web_user`, `acct:<id>:web_pass`). When `Provider.isLoginUrl(currentUrl)` is true and credentials exist, the app fills the login form using selectors from `TornSelectors.kt` (VERIFY with `_samples/login-page.html`). Default: fill only; the user taps Login. A separate toggle "Auto-submit" (default OFF) submits the form.
   - Max 2 auto-fill attempts per 10 minutes. If the page still shows the login form after submit, or a captcha/2FA/verification step is detected, STOP and show "Manual login required".
   - Credentials are inserted into the page via `evaluateJavascript` using properly escaped JSON strings; clear JS variables after use; never log them.
   - Google/Apple sign-in does not work inside the WebView (known limitation). Explain in the opt-in dialog: "Use your Torn username & password. Accounts created with Google/Apple still have a Torn password (check your first email or use Torn's password recovery)."
   - Opt-in dialog text must state: credentials are stored encrypted on this device only and are only typed into the torn.com login form.

## WebView profiles (container isolation)
- If `WebViewFeature.MULTI_PROFILE` is supported (`WebViewFeature.isFeatureSupported`), each ProviderAccount uses its own WebView profile named `acct_<accountId>` (create via `ProfileStore.getInstance().getOrCreateProfile(name)`; attach with `WebViewCompat.setProfile(webView, name)` BEFORE the first load; delete via `ProfileStore.deleteProfile` when the account is deleted).
- If not supported: use the default shared profile; show a small note on the account screen: "Session isolation not supported on this device". Deleting the account clears cookies via `CookieManager.removeAllCookies`.
- `WebProfileManager` hides this behind `fun attach(webView, accountId)` / `fun wipe(accountId)`.

## Browser behavior
1. Single WebView (no tabs). Toolbar: back, forward, reload, home, quick-links menu, scripts menu (Phase 1B F05: kill switch + per-page script list), session chip (Logged in / Logged out / Unknown).
2. Quick links: defaults Home (`https://www.torn.com/index.php`), Profile (`https://www.torn.com/profiles.php?XID={playerId}`), Hospital (`https://www.torn.com/hospitalview.php`). User can add/remove links (host must be allowed).
3. **Settings applied** (hardening, D-04): JavaScript on; DOM storage on; `allowFileAccess=false`; `allowContentAccess=false`; `allowFileAccessFromFileURLs=false`; `allowUniversalAccessFromFileURLs=false`; mixed content blocked; `setSupportMultipleWindows(false)`; no geolocation; file chooser not supported; downloads blocked.
4. **UrlGuard**: only `Provider.allowedWebHosts` load in-app. Other hosts: dialog "Open in your browser?" -> external Custom Tab/Intent. `intent:`/custom schemes are blocked.
5. **Session monitor**: on page load, `Provider.isLoginUrl(url)` -> state LOGGED_OUT, else LOGGED_IN (best effort; UNKNOWN before the first load). Emits state for the account screen and for F04 auto-login.
6. State survives rotation/process death (restore URL; session cookies as per layer 2).
7. Deep-link entry: other features can open the browser with a URL (e.g., attack page) via `BrowserNavigator.open(url)`; if LOGGED_OUT the browser shows the login page.
8. **No automation**: the browser never injects code except (a) the user's enabled userscripts (F05) and (b) the login fill above (layer 3) and (c) user-triggered capture scripts (Phase 2). No auto-refresh timers, no prefetch.

## Edge cases
- Torn shows maintenance/error pages: show the page; no special handling.
- WebView provider missing/outdated: show a clear error with a link to update "Android System WebView".
- Back button inside the browser navigates history first, then leaves the screen.

## Acceptance criteria
- [ ] F04-AC-01 Browser loads torn.com; non-allowed hosts trigger the external-open dialog; `intent:` URLs are blocked.
- [ ] F04-AC-02 WebView hardening settings (all listed) are applied; no `addJavascriptInterface` on untrusted origins.
- [ ] F04-AC-03 After a manual login and a full app restart, the session is still logged in when Torn's cookies are persistent (document the observed behavior in the report).
- [ ] F04-AC-04 Per-account WebView profile used when MULTI_PROFILE is supported; fallback path works and shows the note otherwise; deleting the account wipes the web data.
- [ ] F04-AC-05 Auto-login is opt-in, stores credentials only in SecretVault, fills the form (no submit by default), stops on captcha/2FA/failed attempts, and never logs credentials.
- [ ] F04-AC-06 Session chip reflects Logged in/out via `Provider.isLoginUrl`.
- [ ] F04-AC-07 Quick links are editable and restricted to allowed hosts.
- [ ] F04-AC-08 No code path performs automatic page loads/refreshes/clicks on torn.com (verify by search).
- [ ] F04-AC-09 `BrowserNavigator.open(url)` works from other screens and opens the login page when logged out.

## Out of scope
Tabs, downloads, password manager integration, Google sign-in, ad blocking.
