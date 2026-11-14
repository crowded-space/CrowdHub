# Branding — "Crow" theme

## Name
**Crowd Hub**. Wordmark: "Crow" in primary text color, "d" in the Accent color, "Hub" in muted text. The "d" is the semantic play: Crow -> Crowd (a flock). Build it with `buildAnnotatedString` (three spans) in a `CrowdWordmark()` composable. Never render the wordmark as an image.

## Palette (dark only for Phase 1-2)
| Token | Hex | Use |
|---|---|---|
| `CrowBlack` | `#0E0F13` | app background |
| `Surface` | `#171A21` | cards |
| `SurfaceHigh` | `#1F2330` | dialogs, sheets, selected rows |
| `Outline` | `#2C3242` | dividers/borders |
| `Sheen` (primary) | `#7C5CFF` | primary actions, active nav, chart line |
| `SheenSoft` | `#A58BFF` | secondary emphasis |
| `AccentD` (the "d") | `#FFB020` | wordmark "d", highlights, attention |
| `Text` | `#E6E8EE` | primary text |
| `TextMuted` | `#9AA3B5` | secondary text |
| `Ok` | `#3DDC97` | Okay / success |
| `Danger` | `#FF5C6C` | Hospital / error |
| `Info` | `#4DA3FF` | Traveling |
| `Abroad` | `#B48CFF` | Abroad |
| `Jail` | `#FF8A3D` | Jail / Federal |

Torn status chips: Okay=`Ok`, Hospital=`Danger`, Traveling=`Info`, Abroad=`Abroad`, Jail/Federal=`Jail`, Unknown=`TextMuted`. Status is ALWAYS text + color (never color only).

## Typography
System default sans for UI (no custom font files). Numbers/timers/prices: tabular figures where available, otherwise monospace. Material 3 type scale; no custom scale.

## Shape & feel
Rounded 12dp cards, 8dp chips. Subtle 1dp `Outline` borders. Dense but readable (HUD-like). No gradients except an optional faint `Sheen` glow on the splash/header.

## Icon & splash
Simple geometric crow-head vector (adaptive icon: `CrowBlack` background, crow silhouette with a thin `Sheen` highlight, small `AccentD` dot as the eye). Placeholder art is acceptable; final art comes later. Do not generate bitmap images.

## Microcopy
Plain English, short, tactical. Crow flavor allowed ONLY in: splash tagline ("Watch the flock."), empty states (e.g., "Nothing perched here yet."), and About. Never in labels, errors, or settings.

## Navigation labels (plain)
Dashboard, Stocks, War, Targets, Browser. Overflow menu: Profiles, Scripts, Settings, About. Tabs appear only when their phase ships.
