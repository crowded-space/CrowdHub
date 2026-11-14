# F08 — Stock Market: prices, chart, alerts
Phase: 1A | Depends on: F02, F03 | Package: `feature/stocks`

## Purpose
Clean stock list with charts and price warnings.

## Data
- Source: Torn stocks list (`torn/stocks`, fields per `_samples/torn-stocks.json`): id, name, acronym, current price, market cap, shares, forecast/demand/benefit (show only those present).
- History: Torn does not give history in this endpoint (VERIFY; if `_samples` shows a history field, use it). Crowd Hub records its own snapshots: each successful fetch inserts `StockSnapshotEntity` rows (max 1 per stock per 60 s). Retention per `data-model.md`.

## Behavior
1. **List screen**: search by name/acronym; sort by price / % change (selected window) / name; each row: acronym, name, price, change % (default window 24 h; if insufficient history show "—"), sparkline (last 24 h, Canvas), bell icon if an alert exists.
2. **Detail screen**: price header, line chart (custom Compose Canvas, `Sheen` line, grid in `Outline`, touch-to-inspect crosshair showing time+price), ranges 1 h / 6 h / 24 h / 7 d / 30 d (disable ranges without data; show "Collecting data — only available since <date>"), key stats (market cap, shares, benefit text if present).
3. **Alerts** (per stock, editor bottom sheet): conditions (any combo): price **above** X, price **below** Y, **% move** of at least P% within a window W minutes (5-1440). Options: repeating (with cooldown minutes, default 60) or one-shot. Hysteresis: after firing, the rule re-arms only when the price crosses back past the threshold (or the cooldown elapsed for % moves). Alerts show on channel `stocks`; deep-link opens the detail.
4. **Evaluation**: `StockAlertEvaluator` (pure Kotlin) runs on every new snapshot batch from: foreground refresh, `PollWorker` (BACKGROUND, 15 min), Watch Mode (30 s). Source id `torn.stocks`.
5. **Refresh**: list auto-refreshes every 30 s while visible (USER-class budget is not consumed by auto refresh: use WATCH/foreground class `UI_AUTO` mapped to USER caps; keep total calls tiny — one request returns all stocks).
6. A banner on the Stocks tab explains the background limits: "Background checks run about every 15 minutes. Turn on Watch Mode for 30-second checks."
7. Cleanup job (WorkManager daily or on app start): prune/downsample per retention rules.

## Edge cases
- Price null/zero/missing stock: skip that stock for the cycle; keep last snapshot; never crash charts (guard empty/1-point series).
- Stock removed/renamed: alert remains but shows "Stock not found" and is disabled.
- Large number of snapshots: queries by index; chart downsamples to <= 300 points per render.

## Acceptance criteria
- [ ] F08-AC-01 Stock list renders from the API with search and sorting; sparkline and 24 h change use local history (shows "—" when insufficient).
- [ ] F08-AC-02 Detail chart is a custom Canvas line chart with ranges 1h/6h/24h/7d/30d, touch inspect, and graceful "collecting data" state.
- [ ] F08-AC-03 Price above/below and %-move-in-window alerts can be created, edited, enabled/disabled, deleted.
- [ ] F08-AC-04 `StockAlertEvaluator` is pure Kotlin with unit tests covering above/below, % window, hysteresis, cooldown, one-shot vs repeating.
- [ ] F08-AC-05 Alerts fire from foreground refresh, PollWorker (15 min) and Watch Mode (30 s) and are deduped.
- [ ] F08-AC-06 Snapshots are recorded at most once per minute per stock and pruned/downsampled per retention rules.
- [ ] F08-AC-07 Missing/invalid prices never crash UI or evaluator.
- [ ] F08-AC-08 The background-limits banner is shown; Watch Mode can be toggled from this tab.
- [ ] F08-AC-09 No third-party chart/network libraries added.

## Out of scope
Portfolio/holdings, buy/sell, benefit calculators, forecasting (Phase 3 for portfolio).
