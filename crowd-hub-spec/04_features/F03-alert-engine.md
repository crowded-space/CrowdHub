# F03 — Alert Engine (timers, polling, Watch Mode)
Phase: 1A | Depends on: F01, F02 | Package: `core/alerts`

## Purpose
Reliable notifications from two kinds of alerts: TIMER (known future time) and CONDITION (detected from polled data).

## Model
```kotlin
data class AlertCandidate(
  val dedupeKey: String, val accountId: String, val channelId: String,
  val title: String, val body: String,
  val fireAtEpochMs: Long?,         // null = fire now
  val deepLink: String?             // internal route
)
```
`AlertSource.collect(ctx)` returns candidates; `AlertDeduper` decides new/changed/cancelled; `Scheduler` schedules or posts.

## Behavior
1. **Channels** created at app start: `timers` (high), `war` (high), `stocks` (default), `watch_mode` (low, ongoing), `system` (low). Users can mute per channel via system settings; in-app toggles also exist (F00).
2. **TIMER alerts (Phase 1A sources: `torn.bars`, `torn.status`, `torn.travel`, `torn.cooldowns`)**
   - Bar full time for energy/nerve/happy/life (from `fulltime`/timestamps, VERIFY).
   - Cooldown ready: drug, medical, booster.
   - Hospital release and jail release (from `status.until`).
   - Own landing (from travel arrival timestamp) with optional "N minutes before" (default 0; options 0/1/2/5).
   - Scheduling: `AlarmManager.setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, else `setAndAllowWhileIdle` + visible notice "Timers may be delayed" in Settings > Reliability. All fire times use `Clock`.
   - `AlarmReceiver` posts the notification; `BootReceiver` re-schedules from `ScheduledAlertEntity` after reboot/app update.
3. **Refreshing timers**: whenever Dashboard data refreshes (foreground, background poll, Watch Mode) the sources re-run; the deduper re-schedules when a timestamp changed by > 10 s and cancels timers whose condition vanished (e.g., cooldown used earlier than expected).
4. **CONDITION alerts**: evaluated by `AlertSource`s during polls (Phase 1A: `torn.stocks` — see F08; Phase 2 adds war sources).
5. **Background polling**: `PollWorker` (WorkManager periodic, 15 min, network required) runs the enabled sources at BACKGROUND priority; it must be cheap (Torn: bars+cooldowns+status+travel in <= 2 calls, stocks in 1 call).
6. **Watch Mode** (foreground service, type `dataSync`): started manually (switch on Dashboard/Stocks toolbar). Polls enabled WATCH sources every 30 s (aligned with API cache). Ongoing notification on channel `watch_mode` with action "Stop". Auto-stops after the configured duration (default 4 h) and when the OS timeout callback fires (VERIFY Android 15): stop cleanly and post "Watch Mode stopped by the system". Must stop itself if no watch sources are active.
7. **Deduplication**: `fired_alert` semantics via `ScheduledAlertEntity` (`dedupeKey`, state). The same `dedupeKey` never fires twice unless its `fireAt` moved forward by > 10 s and state was reset by the source.
8. **Deep links**: tapping a notification opens the relevant screen (Dashboard, Stock detail...). `PendingIntent.FLAG_IMMUTABLE`.
9. **Permission flows** are in-context (F01 onboarding + Settings > Reliability).

## Edge cases
- App updated/killed/rebooted: timers restored.
- Two sources emit the same logical alert: dedupe by key (include accountId and logical id, e.g., `torn:energy_full:<accountId>`).
- No network at fire time: timers still fire (they are local); data refresh later.
- Alerts for an INVALID account are suppressed (except the one `system` notice).

## Acceptance criteria
- [ ] F03-AC-01 Channels `timers`, `war`, `stocks`, `watch_mode`, `system` exist with the specified importance.
- [ ] F03-AC-02 Energy/nerve/happy/life full, cooldown ready, hospital/jail release and own landing timers are scheduled from API timestamps via `Clock` and fire at the right time.
- [ ] F03-AC-03 Exact alarms are used when permitted; otherwise a visible fallback notice exists.
- [ ] F03-AC-04 Timers survive app kill and device reboot (BootReceiver re-schedules from the DB).
- [ ] F03-AC-05 Changed timestamps (> 10 s) re-schedule; vanished conditions cancel the timer (unit tested via `AlertDeduper`).
- [ ] F03-AC-06 `PollWorker` runs every 15 min at BACKGROUND priority and never exceeds its limiter class cap.
- [ ] F03-AC-07 Watch Mode runs as a `dataSync` foreground service with a Stop action, 30 s polling, auto-stop duration, and graceful OS-timeout handling.
- [ ] F03-AC-08 The same `dedupeKey` never fires twice for the same fire time (unit tested).
- [ ] F03-AC-09 Notification taps deep-link to the right screen with `FLAG_IMMUTABLE` PendingIntents.
- [ ] F03-AC-10 Alerts are suppressed for INVALID/PAUSED accounts.

## Out of scope
Quiet hours, snooze, custom sounds, FCM push, per-alert history screen (Phase 3).
