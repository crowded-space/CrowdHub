# Manual QA Plan (real device preferred)

Use a Limited API key. Record results in the audit follow-up.

## Phase 1A
1. **Onboarding**: fresh install -> complete steps; kill the app at step 3 and reopen (resumes). Enter a wrong key -> clear error, no stored secret. Enter a Public-level key -> access-level message.
2. **Dashboard accuracy**: compare Energy/Nerve/Happy/Life/Cooldowns/Status with the game side by side.
3. **Timers**: note a cooldown/bar-full time; confirm the notification fires within a few seconds of the displayed time (screen off). Reboot the phone before it fires: still fires.
4. **Own flight**: fly to Mexico; confirm arrival countdown and landing alert.
5. **Rate limiter**: open Developer > API Probe; spam Refresh on Dashboard; counters stay under caps; no error code 5.
6. **Invalid key**: revoke the key in Torn; next refresh shows the banner, calls stop, one `system` notification, no retry loop (watch probe counters).
7. **Stocks**: create an above/below alert slightly off the current price; confirm firing via foreground, then Watch Mode, then (after 15+ min) background.
8. **Offline**: airplane mode -> Stale badges, no crashes; reconnect -> recovers.
9. **Privacy**: grep logs (Logcat) for your key -> none. Screenshot attempt on key screens -> blocked.

## Phase 1B
1. Login manually; fully close the app; reopen -> still logged in? Record result (session cookie behavior).
2. Enable auto-login (fill only); log out on Torn; reopen -> form filled, no submit. With auto-submit on: logs in; with a wrong password: stops after 2 attempts.
3. Install a trivial script (adds a visible banner on torn.com); toggle it; kill switch; verify no injection when off.
4. `GM_xmlhttpRequest` to a non-allowed host -> blocked; to a non-API torn.com page -> blocked.
5. API from a script: spam calls -> limited at 20/min, app features unaffected.
6. Delete the account -> web session cleared, scripts removed.

## Phase 2
1. Enable FFScouter: cancel consent -> no network call; accept -> FF/BS appear; disable -> cache gone.
2. War: during a real ranked war, compare enemy statuses/timers with the game; stakeout a member; verify alerts.
3. Flight: observe 3 enemy flights; compare ETA windows with real landings; check confidence chips and the learning of the book flag; check Flight Debug mapping editor.
4. Targets: open the hospital page, Capture; confirm rows, timers and filters; try a second page capture; check that nothing loads automatically.
5. Attack shortcut: tap Attack from War and Targets -> attack page opens; confirm the app never clicks anything.
6. Budget: with war + targets + stocks + Watch Mode on for 10 minutes, API Probe counters stay under the caps; no code 5/8 errors.
