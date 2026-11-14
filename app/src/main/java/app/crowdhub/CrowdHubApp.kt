package app.crowdhub

import android.app.Application
import android.content.Context
import app.crowdhub.connector.ffscouter.FfApi
import app.crowdhub.connector.ffscouter.FfRateLimiter
import app.crowdhub.connector.ffscouter.FfRepository
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.AlertHost
import app.crowdhub.core.alerts.NotificationChannels
import app.crowdhub.core.alerts.PollWorker
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.browser.BrowserNavigator
import app.crowdhub.core.browser.SessionMonitor
import app.crowdhub.core.browser.UrlGuard
import app.crowdhub.core.browser.WebProfileManager
import app.crowdhub.core.db.CacheEntryEntity
import app.crowdhub.core.db.CrowdDatabase
import app.crowdhub.core.db.ScheduledAlertDao
import app.crowdhub.core.network.CachedResponse
import app.crowdhub.core.network.HttpClientFactory
import app.crowdhub.core.network.PersistentCacheStore
import app.crowdhub.core.network.RateLimiter
import app.crowdhub.core.network.ResponseCache
import app.crowdhub.core.network.ServerClock
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.scripts.GmBridge
import app.crowdhub.core.scripts.ScriptRepository
import app.crowdhub.core.scripts.UserscriptEngine
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.vault.KeystoreCipher
import app.crowdhub.core.vault.SecretVault
import app.crowdhub.feature.browser.AttackNavigator
import app.crowdhub.feature.chain.TargetsViewModel
import app.crowdhub.feature.flight.FlightRepository
import app.crowdhub.feature.stocks.StockRepository
import app.crowdhub.feature.war.StakeoutManager
import app.crowdhub.provider.ProviderRegistry
import app.crowdhub.provider.torn.TornApi
import app.crowdhub.provider.torn.TornProvider
import app.crowdhub.provider.torn.TornTimerAlertSource
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: CrowdDatabase = CrowdDatabase.build(context)
    val clock: ServerClock = ServerClock()
    val settingsStore: SettingsStore = SettingsStore(context)
    val secretVault: SecretVault = SecretVault(db.secretBlobDao(), KeystoreCipher(), clock)
    val okHttpClient: OkHttpClient = HttpClientFactory.create()
    val rateLimiter: RateLimiter = RateLimiter(clock)

    private val persistentCacheStore = object : PersistentCacheStore {
        override suspend fun read(key: String): CachedResponse? {
            return db.cacheDao().get(key)?.let { CachedResponse(it.body, it.fetchedAt) }
        }

        override suspend fun write(key: String, body: String, fetchedAtMs: Long, ttlMs: Long) {
            db.cacheDao().put(CacheEntryEntity(key, body, fetchedAtMs, ttlMs))
        }

        override suspend fun clearAll() {
            db.cacheDao().clearAll()
        }
    }

    val responseCache: ResponseCache = ResponseCache(
        clock = clock,
        defaultTtlMs = ResponseCache.DEFAULT_TTL_MS,
        persistentStore = persistentCacheStore
    )

    val scheduler: Scheduler = Scheduler(context)
    val alertEngine: AlertEngine = AlertEngine(
        profileDao = db.profileDao(),
        accountDao = db.providerAccountDao(),
        scheduledAlertDao = db.scheduledAlertDao(),
        scheduler = scheduler,
        clock = clock
    )

    val webProfileManager: WebProfileManager = WebProfileManager()
    val urlGuard: UrlGuard = UrlGuard(TornUrls.ALLOWED_WEB_HOSTS)
    val sessionMonitor: SessionMonitor = SessionMonitor(TornUrls::isLoginUrl)
    val browserNavigator: BrowserNavigator = BrowserNavigator()
    val attackNavigator: AttackNavigator = AttackNavigator(browserNavigator, settingsStore)

    val profileRepository: ProfileRepository = ProfileRepository(
        db = db,
        vault = secretVault,
        alertEngine = alertEngine,
        scheduler = scheduler,
        responseCache = responseCache,
        settingsStore = settingsStore,
        clock = clock,
        onWipeWebProfile = { accountId -> webProfileManager.wipe(accountId) }
    )

    val tornApi: TornApi = TornApi(
        okHttpClient = okHttpClient,
        rateLimiter = rateLimiter,
        responseCache = responseCache,
        clock = clock,
        vault = secretVault,
        onKeyRejected = { accountId, status, reason ->
            profileRepository.markAccountKeyStatus(accountId, status, reason)
        }
    )

    val ffRateLimiter: FfRateLimiter = FfRateLimiter(clock)
    val ffApi: FfApi = FfApi(okHttpClient, ffRateLimiter, secretVault, clock)
    val ffRepository: FfRepository = FfRepository(db.connectorLinkDao(), db.ffCacheDao(), ffApi, clock)

    val stockRepository: StockRepository = StockRepository(db.stockDao(), tornApi, settingsStore, clock)
    val flightRepository: FlightRepository = FlightRepository(db.warAndFlightDao(), clock)
    val stakeoutManager: StakeoutManager = StakeoutManager(
        warAndFlightDao = db.warAndFlightDao(),
        flightRepository = flightRepository,
        tornApi = tornApi,
        settingsStore = settingsStore,
        clock = clock
    )

    val tornProvider: TornProvider = TornProvider(
        api = tornApi,
        sources = listOf(
            TornTimerAlertSource(tornApi, settingsStore),
            stockRepository.alertSource(),
            stakeoutManager.watchModeAlertSource()
        )
    )

    val providerRegistry: ProviderRegistry = ProviderRegistry(listOf(tornProvider))

    val scriptRepository: ScriptRepository = ScriptRepository(db.userscriptDao(), okHttpClient, clock)
    val userscriptEngine: UserscriptEngine = UserscriptEngine(
        scriptRepository = scriptRepository,
        settingsStore = settingsStore,
        secretVault = secretVault,
        allowedWebHosts = TornUrls.ALLOWED_WEB_HOSTS,
        scope = appScope
    )
    val gmBridge: GmBridge = GmBridge(
        scriptRepository = scriptRepository,
        rateLimiter = rateLimiter,
        okHttpClient = okHttpClient,
        blockedWebHosts = TornUrls.ALLOWED_WEB_HOSTS,
        apiHost = "api.torn.com",
        scope = appScope
    )

    val targetsViewModel: TargetsViewModel = TargetsViewModel(
        profileRepository = profileRepository,
        targetDao = db.targetDao(),
        tornApi = tornApi,
        ffRepository = ffRepository,
        alertEngine = alertEngine,
        clock = clock,
        scope = appScope
    )

    init {
        alertEngine.registerSources(tornProvider.alertSources())
    }
}

class CrowdHubApp : Application(), AlertHost {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        container = AppContainer(this)
        container.appScope.launch {
            if (container.settingsStore.current().onboardingCompleted) {
                PollWorker.enqueue(this@CrowdHubApp)
            }
        }
    }

    override val alertEngine: AlertEngine get() = container.alertEngine
    override val scheduler: Scheduler get() = container.scheduler
    override val scheduledAlertDao: ScheduledAlertDao get() = container.db.scheduledAlertDao()
    override val settingsStore: SettingsStore get() = container.settingsStore
    override val clock: ServerClock get() = container.clock
    override val appScope: CoroutineScope get() = container.appScope

    override suspend fun runStockCleanup() {
        container.stockRepository.runRetentionCleanup()
    }
}
