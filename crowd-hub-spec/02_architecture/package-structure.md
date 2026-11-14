# Package Structure (DO NOT rename or move)

Base package: `app.crowdhub` (use the template's package only if AI Studio forces it; then keep the same sub-structure).

```
app.crowdhub
|- CrowdHubApp.kt                 Application + AppContainer creation
|- MainActivity.kt                single Activity, NavHost
|- core/
|  |- ui/            theme (Color.kt, Theme.kt, Type.kt), components (StatusChip, CountdownText, StaleBadge, CrowdWordmark), nav
|  |- db/            CrowdDatabase, Daos, Entities (see data-model.md), Migrations
|  |- settings/      SettingsStore (DataStore, non-secret)
|  |- vault/         SecretVault, KeystoreCipher
|  |- profile/       ProfileRepository, ActiveProfile, onboarding state
|  |- network/       HttpClientFactory, RateLimiter, ResponseCache, Clock, ApiResult, ApiError, CrowLog
|  |- alerts/        AlertEngine, AlertCandidate, AlertDeduper, Scheduler, NotificationChannels,
|  |                 AlarmReceiver, BootReceiver, PollWorker, WatchModeService
|  |- browser/       CrowWebView (Phase 1B), WebProfileManager, UrlGuard, SessionMonitor
|  |- scripts/       UserscriptEngine, UserscriptMetaParser, GmBridge, ScriptRepository (Phase 1B)
|- provider/
|  |- Provider.kt, ProviderRegistry.kt
|  |- torn/          TornProvider, TornApi (endpoints), TornDto (models), TornMappers, TornUrls, TornRules,
|                    TornSelectors (login/hospital DOM selectors, VERIFY), FlightTable (Phase 2)
|- connector/
|  |- Connector.kt
|  |- ffscouter/     FfScouterConnector, FfApi, FfDto, FfRepository (Phase 2)
|- feature/
   |- dashboard/     DashboardScreen, DashboardViewModel, cards (Bars, Cooldowns, Status, Travel, Money, Events)
   |- stocks/        StocksScreen, StockDetailScreen, StocksViewModel, PriceChart (Canvas), StockAlertEvaluator, StockRepository
   |- profiles/      ProfilesScreen (advanced), OnboardingScreens, AddAccountScreen
   |- settings/      SettingsScreen, AboutScreen, PrivacyScreen, DeveloperScreen (API Probe)
   |- browser/       BrowserScreen, QuickLinks (Phase 1B)
   |- scripts/       ScriptsScreen, ScriptEditor/Installer (Phase 1B)
   |- war/           WarScreen, WarViewModel, MemberRow, MemberDiffer, StakeoutManager (Phase 2)
   |- flight/        FlightEstimator, FlightRepository, FlightChip, FlightDebugScreen (Phase 2)
   |- chain/         TargetsScreen, TargetsViewModel, HospitalPageParser, TargetFilters (Phase 2)
```

## Naming rules
- Files named after the main class. One public class per file (small private helpers allowed).
- ViewModels: `<Feature>ViewModel`. Screens: `<Feature>Screen`. Repositories: `<Feature>Repository`. DTOs end with `Dto`. Room entities end with `Entity`.
- Strings in `strings.xml` for user-visible text. Constants in `object <Name>Defaults`.
- Tests mirror the package under `src/test` (pure Kotlin only).

## Files that MUST exist at the repo root (besides Gradle files)
`BACKLOG_SUGGESTIONS.md` (create empty on first build), the spec folders from this pack (unchanged).
