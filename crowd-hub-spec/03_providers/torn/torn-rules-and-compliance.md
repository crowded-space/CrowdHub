# Torn — Rules & Compliance (research snapshot, Nov 2026)

> Torn's rules can change. These notes are a snapshot of what was found during research. The user MUST re-read Torn's current rules (scripting + API terms) before any release. (Checklist item in `07_checklists/definition-of-done.md`.)

## 1. Scripting rule (core of the guardrails)
A script/tool is acceptable when it works with data from the **official API**, or from a **page the player loaded manually and is currently viewing**. Requests to Torn that are neither API calls nor manually triggered by the player are NOT acceptable. Therefore:
- Allowed: API polling within limits; parsing a page the user has open (button "Capture this page" pressed by the user); user-installed userscripts that decorate the page the user is viewing; opening a link the user tapped.
- Forbidden: background fetching of torn.com pages; prefetch; automatic page refresh loops; auto-click/auto-attack/auto-travel/auto-buy; simulated input on game pages; headless browsing.

## 2. API rules (snapshot)
- Limit: **100 requests per minute per user** (all keys of the same user count together). The app budgets lower (see F02).
- Responses are cached by the service for up to ~**30 seconds**; polling faster is pointless.
- Repeated requests with **invalid keys can cause a temporary IP ban**. -> stop using a key immediately when it is rejected.
- Passwords must never be requested by API-based services.
- Services must be transparent to users about how their key is used (Torn publishes a table: data storage, data sharing, purpose, key storage/sharing, access level). Crowd Hub's declaration (use verbatim in the Privacy screen; VERIFY wording against Torn's current table):
  - Data storage: only locally on the device.
  - Data sharing: nobody (exception: FFScouter, only if the user enables it).
  - Purpose of use: personal, non-commercial companion app.
  - Key storage: locally, encrypted (Android Keystore).
  - Key sharing: not shared (exception: FFScouter, opt-in).
  - Access level required: Limited.
- API keys are read-only access to data; they cannot log in to the website. Web login requires the player's own session (F04).

## 3. Accounts
Torn is generally understood to prohibit multiple accounts per person. Crowd Hub keeps a multi-profile **data model** (for architecture only). Multi-profile UI is hidden in Settings > Advanced and the About/Privacy screen reminds the user that complying with Torn's account rules is their responsibility. (Unverified by this pack: confirm in Torn's rules.)

## 4. Feature compliance map
| Feature | Rule basis | Design constraint |
|---|---|---|
| Dashboard, stocks, war stakeout, flight tracker | API only | Through RateLimiter; no page requests |
| Hospital target list (source A) | User-loaded page | Only on user pressing "Capture"; no auto-navigation/auto-refresh |
| Attack shortcut | User-initiated link | Opens page only; no clicking inside |
| Userscripts | User-installed; run on page user views | No background download; native bridges blocked for non-API torn.com hosts |
| Auto-login (opt-in) | Own session, own credentials | Fills login form only; default no auto-submit; stops on captcha/2FA |
| FFScouter | Third-party service, consent | Explicit consent; disclose key sharing |

## 5. Login methods (PDA lesson)
Torn PDA's developer notes that "Login with Google" does not work inside the web view; players use their Torn username/password (accounts created via Google/Apple still have a password emailed on day 1 / recoverable). Crowd Hub follows the same limitation.
