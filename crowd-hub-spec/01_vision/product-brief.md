# Product Brief — Crowd Hub

## One-liner
A privacy-first Android companion hub. Torn (torn.com) is the first Provider; the architecture must allow other services later without rewriting the core.

## Who it is for
A single Torn player (the author) who wants a Torn PDA-like experience with extra war/chain/stock tooling. Personal-use APK first; Play Store is out of scope.

## Principles
1. **Local-only.** No backend. API keys, cookies and credentials never leave the device (except where the spec explicitly allows: Torn API, consented FFScouter, pages the user opens).
2. **Compliance first.** Respect Torn's API limits and scripting rules (see `03_providers/torn/torn-rules-and-compliance.md`). Anything that could be seen as automation is out.
3. **Honest data.** Estimates (flight ETA, battle stats) are always labeled as estimates with a confidence indicator.
4. **Preparation, not pinpoint.** ETAs are for planning and calculation, not exact-second guarantees.
5. **Small abstractions.** Only three interfaces (`Provider`, `Connector`, `AlertSource`). No framework-building.
6. **Flavor, not friction.** Crow theme in visuals; plain English in labels.

## Scope by phase
- **1A** Profile container + vault, API manager (limiter/cache/clock), alert engine (timers + Watch Mode), dashboard, own-travel card, stock market (chart + alerts), settings/about.
- **1B** In-app browser, persistent web session, optional auto-login, userscript engine (PDA-compatible), script manager.
- **2** FFScouter connector, ranked war stakeout, flight tracker (own ETA estimator), chain target assist, attack shortcut.
- **3** Backlog (see F14).

## Non-goals (never build unless a later spec says so)
- Multi-service UIs other than Torn. Light theme (until Phase 3). Localization. Cloud sync. Accounts/login to any Crowd Hub service. Push via FCM. Auto-play/automation of any kind. In-app purchases/ads. Social features.

## Success criteria
- Phase 1A: key onboarding works; dashboard correct vs. the game; timers fire on time; stock alerts fire on threshold; app survives restart/offline.
- Phase 1B: login persists across restarts; scripts run on torn.com pages; kill switch works.
- Phase 2: enemy faction list updates at most every 30 s, statuses/timers correct; flight ETA windows labeled with confidence; hospital-based target list filters by FF and release time; one tap opens the attack page.
