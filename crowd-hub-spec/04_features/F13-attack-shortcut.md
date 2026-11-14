# F13 — Attack Shortcut
Phase: 2 | Depends on: F04 | Package: `provider/torn/TornUrls`, `feature/browser`

## Behavior
`AttackNavigator.open(playerId)` builds `TornUrls.attack(playerId)` = `https://www.torn.com/loader.php?sid=attack&user2ID={playerId}` and opens it via `BrowserNavigator.open(url)` (F04). Setting "Open attacks in": In-app browser (default) / External browser.
- If the web session is LOGGED_OUT the browser shows the login page (user logs in manually; then the user taps the Attack button again).
- The app NEVER interacts with the attack page (no clicking "Start fight", no script injection by Crowd Hub; user-enabled userscripts still run as usual).
- Attack buttons appear in: War rows (F10), Targets rows (F12), and the member detail sheet.

## Acceptance criteria
- [ ] F13-AC-01 The URL is built exactly as specified from the player id; only digits are accepted as id.
- [ ] F13-AC-02 The attack page opens in the chosen browser mode; external mode uses an Intent.
- [ ] F13-AC-03 No Crowd Hub code clicks or scripts anything on the attack page (verify by search).
- [ ] F13-AC-04 Logged-out state leads to the login page without losing the intended target (the user can tap Attack again).

## Out of scope
Attack automation, fight result tracking.
