# Anti-Scope-Creep

AI coding agents tend to "help" by adding things. Do not.

## Phase fence
| Phase | Allowed features | Forbidden until later |
|---|---|---|
| 1A | F00 F01 F02 F03 F06 F07 F08 | browser, scripts, FFScouter, war, flight, chain |
| 1B | F04 F05 | FFScouter, war, flight, chain |
| 2 | F09 F10 F11 F12 F13 | anything in F14 |
| 3 | F14 items the user names | everything else |

## Never add (any phase) unless the user explicitly asks
- Extra screens, tabs, onboarding carousels (onboarding is exactly the steps in F01).
- Animations beyond default Compose transitions; custom fonts; light theme; localization.
- New settings that are not in a feature file; "advanced" toggles; feature flags.
- Extra libraries (charts, image loaders, DI, networking, analytics...). The chart is custom Compose Canvas.
- Architecture "improvements": extra interfaces, base classes, generic repositories, use-case layers, multi-module splits.
- Behavioral "improvements" to Torn flows: macros, auto-refresh of game pages, auto-fill that clicks, any automation.
- Telemetry, crash reporting, remote config, update checkers.

## When you have an idea
Append to `BACKLOG_SUGGESTIONS.md`: `- [phase?] idea — why — risk`. Do not implement.

## When the spec is wrong or incomplete
Stop. Ask in Step 0 (or as a numbered question in the report). Do not patch the spec in code.

## Size guard
Prefer the smallest implementation that satisfies the acceptance criteria. If a file grows beyond ~400 lines, split by responsibility (inside the same package) — do not introduce new layers.
