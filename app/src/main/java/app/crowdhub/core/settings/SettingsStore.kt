package app.crowdhub.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "crowdhub_settings")

@Serializable
data class QuickLinkItem(
    val label: String,
    val url: String
)

@Serializable
data class PersistedBrowserTab(
    val id: String,
    val url: String,
    val title: String = "Torn"
)

enum class FlightAlertTiming { EARLIEST, MIDPOINT }
enum class AttackBrowserMode { IN_APP, EXTERNAL }

data class AppPreferences(
    val alertsBarsEnabled: Boolean = true,
    val alertsCooldownsEnabled: Boolean = true,
    val alertsStatusEnabled: Boolean = true,
    val alertsTravelEnabled: Boolean = true,
    val alertsStocksEnabled: Boolean = true,
    val alertsWarEnabled: Boolean = true,
    val landingLeadMin: Int = 0, // 0, 1, 2, 5
    val watchModeDurationHours: Int = 4, // 1, 2, 4, 6
    val watchModeActive: Boolean = false,
    val developerUnlocked: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val scriptsKillSwitch: Boolean = false,
    val autoLoginEnabled: Boolean = false,
    val autoLoginAutoSubmit: Boolean = false,
    val openAttacksMode: AttackBrowserMode = AttackBrowserMode.IN_APP,
    val warStakeoutLeadSec: Int = 0, // 0, 10, 30, 60, 120
    val flightAlertLeadMin: Int = 2,
    val flightAlertTiming: FlightAlertTiming = FlightAlertTiming.EARLIEST,
    val ffLowThreshold: Double = 2.0,
    val ffHighThreshold: Double = 3.0,
    val manualWarFactionIdByProfile: Map<String, String> = emptyMap(),
    val quickLinksByProfile: Map<String, List<QuickLinkItem>> = emptyMap(),
    val backgroundStockSyncEnabled: Boolean = true,
    val backgroundFlightSyncEnabled: Boolean = true,
    val lastBackgroundSyncEpochMs: Long = 0L,
    val lastBackgroundSyncSummary: String = "",
    val browserTabsByProfile: Map<String, List<PersistedBrowserTab>> = emptyMap(),
    val activeBrowserTabIdByProfile: Map<String, String> = emptyMap()
) {
    fun manualWarFactionIdFor(profileId: String): String =
        if (profileId.isBlank()) "" else manualWarFactionIdByProfile[profileId].orEmpty()

    fun quickLinksFor(profileId: String): List<QuickLinkItem> =
        if (profileId.isBlank()) emptyList() else quickLinksByProfile[profileId].orEmpty()

    fun browserTabsFor(profileId: String): List<PersistedBrowserTab> =
        if (profileId.isBlank()) emptyList() else browserTabsByProfile[profileId].orEmpty()

    fun activeBrowserTabIdFor(profileId: String): String =
        if (profileId.isBlank()) "" else activeBrowserTabIdByProfile[profileId].orEmpty()
}

class SettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private object Keys {
        val ALERTS_BARS = booleanPreferencesKey("alerts_bars")
        val ALERTS_COOLDOWNS = booleanPreferencesKey("alerts_cooldowns")
        val ALERTS_STATUS = booleanPreferencesKey("alerts_status")
        val ALERTS_TRAVEL = booleanPreferencesKey("alerts_travel")
        val ALERTS_STOCKS = booleanPreferencesKey("alerts_stocks")
        val ALERTS_WAR = booleanPreferencesKey("alerts_war")
        val LANDING_LEAD_MIN = intPreferencesKey("landing_lead_min")
        val WATCH_DURATION_H = intPreferencesKey("watch_duration_h")
        val WATCH_ACTIVE = booleanPreferencesKey("watch_active")
        val DEV_UNLOCKED = booleanPreferencesKey("dev_unlocked")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SCRIPTS_KILL_SWITCH = booleanPreferencesKey("scripts_kill_switch")
        val AUTO_LOGIN_ENABLED = booleanPreferencesKey("auto_login_enabled")
        val AUTO_LOGIN_SUBMIT = booleanPreferencesKey("auto_login_submit")
        val OPEN_ATTACKS_MODE = stringPreferencesKey("open_attacks_mode")
        val WAR_STAKEOUT_LEAD_SEC = intPreferencesKey("war_stakeout_lead_sec")
        val FLIGHT_ALERT_LEAD_MIN = intPreferencesKey("flight_alert_lead_min")
        val FLIGHT_ALERT_TIMING = stringPreferencesKey("flight_alert_timing")
        val FF_LOW_THRESHOLD = doublePreferencesKey("ff_low_threshold")
        val FF_HIGH_THRESHOLD = doublePreferencesKey("ff_high_threshold")
        val MANUAL_WAR_FACTION_ID = stringPreferencesKey("manual_war_faction_id")
        val QUICK_LINKS_JSON = stringPreferencesKey("quick_links_json")
        val BG_STOCK_SYNC = booleanPreferencesKey("bg_stock_sync")
        val BG_FLIGHT_SYNC = booleanPreferencesKey("bg_flight_sync")
        val LAST_BG_SYNC_MS = longPreferencesKey("last_bg_sync_ms")
        val LAST_BG_SYNC_SUMMARY = stringPreferencesKey("last_bg_sync_summary")
        val BROWSER_TABS_JSON = stringPreferencesKey("browser_tabs_json")
        val ACTIVE_BROWSER_TAB_ID = stringPreferencesKey("active_browser_tab_id")

        val MANUAL_WAR_FACTION_BY_PROFILE_JSON = stringPreferencesKey("manual_war_faction_by_profile_json")
        val QUICK_LINKS_BY_PROFILE_JSON = stringPreferencesKey("quick_links_by_profile_json")
        val BROWSER_TABS_BY_PROFILE_JSON = stringPreferencesKey("browser_tabs_by_profile_json")
        val ACTIVE_BROWSER_TAB_BY_PROFILE_JSON = stringPreferencesKey("active_browser_tab_by_profile_json")
        val LEGACY_PROFILE_PREFS_MIGRATED = booleanPreferencesKey("legacy_profile_prefs_migrated")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val manualWarMap = decodeMap<String>(prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON])
        val quickLinksMap = decodeMap<List<QuickLinkItem>>(prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON])
        val browserTabsMap = decodeMap<List<PersistedBrowserTab>>(prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON])
        val activeTabMap = decodeMap<String>(prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON])

        AppPreferences(
            alertsBarsEnabled = prefs[Keys.ALERTS_BARS] ?: true,
            alertsCooldownsEnabled = prefs[Keys.ALERTS_COOLDOWNS] ?: true,
            alertsStatusEnabled = prefs[Keys.ALERTS_STATUS] ?: true,
            alertsTravelEnabled = prefs[Keys.ALERTS_TRAVEL] ?: true,
            alertsStocksEnabled = prefs[Keys.ALERTS_STOCKS] ?: true,
            alertsWarEnabled = prefs[Keys.ALERTS_WAR] ?: true,
            landingLeadMin = prefs[Keys.LANDING_LEAD_MIN] ?: 0,
            watchModeDurationHours = prefs[Keys.WATCH_DURATION_H] ?: 4,
            watchModeActive = prefs[Keys.WATCH_ACTIVE] ?: false,
            developerUnlocked = prefs[Keys.DEV_UNLOCKED] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            scriptsKillSwitch = prefs[Keys.SCRIPTS_KILL_SWITCH] ?: false,
            autoLoginEnabled = prefs[Keys.AUTO_LOGIN_ENABLED] ?: false,
            autoLoginAutoSubmit = prefs[Keys.AUTO_LOGIN_SUBMIT] ?: false,
            openAttacksMode = prefs[Keys.OPEN_ATTACKS_MODE]?.let {
                runCatching { AttackBrowserMode.valueOf(it) }.getOrNull()
            } ?: AttackBrowserMode.IN_APP,
            warStakeoutLeadSec = prefs[Keys.WAR_STAKEOUT_LEAD_SEC] ?: 0,
            flightAlertLeadMin = prefs[Keys.FLIGHT_ALERT_LEAD_MIN] ?: 2,
            flightAlertTiming = prefs[Keys.FLIGHT_ALERT_TIMING]?.let {
                runCatching { FlightAlertTiming.valueOf(it) }.getOrNull()
            } ?: FlightAlertTiming.EARLIEST,
            ffLowThreshold = prefs[Keys.FF_LOW_THRESHOLD] ?: 2.0,
            ffHighThreshold = prefs[Keys.FF_HIGH_THRESHOLD] ?: 3.0,
            manualWarFactionIdByProfile = manualWarMap,
            quickLinksByProfile = quickLinksMap,
            backgroundStockSyncEnabled = prefs[Keys.BG_STOCK_SYNC] ?: true,
            backgroundFlightSyncEnabled = prefs[Keys.BG_FLIGHT_SYNC] ?: true,
            lastBackgroundSyncEpochMs = prefs[Keys.LAST_BG_SYNC_MS] ?: 0L,
            lastBackgroundSyncSummary = prefs[Keys.LAST_BG_SYNC_SUMMARY] ?: "",
            browserTabsByProfile = browserTabsMap,
            activeBrowserTabIdByProfile = activeTabMap
        )
    }

    private inline fun <reified T> decodeMap(raw: String?): Map<String, T> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, T>>(raw) }.getOrDefault(emptyMap())
    }

    suspend fun current(): AppPreferences = preferencesFlow.first()

    suspend fun migrateLegacyProfileScopedPrefsIfNeeded(activeProfileId: String?) {
        if (activeProfileId.isNullOrBlank()) return
        context.dataStore.edit { prefs ->
            if (prefs[Keys.LEGACY_PROFILE_PREFS_MIGRATED] == true) return@edit

            val legacyTabs = prefs[Keys.BROWSER_TABS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<PersistedBrowserTab>>(raw) }.getOrNull()
            }.orEmpty()
            val legacyActiveTab = prefs[Keys.ACTIVE_BROWSER_TAB_ID].orEmpty()
            val legacyQuickLinks = prefs[Keys.QUICK_LINKS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<QuickLinkItem>>(raw) }.getOrNull()
            }.orEmpty()
            val legacyManualWar = prefs[Keys.MANUAL_WAR_FACTION_ID].orEmpty()

            if (legacyTabs.isNotEmpty()) {
                val map = decodeMap<List<PersistedBrowserTab>>(prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON]).toMutableMap()
                if (!map.containsKey(activeProfileId)) {
                    map[activeProfileId] = legacyTabs
                    prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON] = json.encodeToString(map)
                }
            }
            if (legacyActiveTab.isNotBlank()) {
                val map = decodeMap<String>(prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON]).toMutableMap()
                if (!map.containsKey(activeProfileId)) {
                    map[activeProfileId] = legacyActiveTab
                    prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON] = json.encodeToString(map)
                }
            }
            if (legacyQuickLinks.isNotEmpty()) {
                val map = decodeMap<List<QuickLinkItem>>(prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON]).toMutableMap()
                if (!map.containsKey(activeProfileId)) {
                    map[activeProfileId] = legacyQuickLinks
                    prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON] = json.encodeToString(map)
                }
            }
            if (legacyManualWar.isNotBlank()) {
                val map = decodeMap<String>(prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON]).toMutableMap()
                if (!map.containsKey(activeProfileId)) {
                    map[activeProfileId] = legacyManualWar
                    prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON] = json.encodeToString(map)
                }
            }

            prefs.remove(Keys.BROWSER_TABS_JSON)
            prefs.remove(Keys.ACTIVE_BROWSER_TAB_ID)
            prefs.remove(Keys.QUICK_LINKS_JSON)
            prefs.remove(Keys.MANUAL_WAR_FACTION_ID)
            prefs[Keys.LEGACY_PROFILE_PREFS_MIGRATED] = true
        }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setDeveloperUnlocked(unlocked: Boolean) {
        context.dataStore.edit { it[Keys.DEV_UNLOCKED] = unlocked }
    }

    suspend fun setWatchModeActive(active: Boolean) {
        context.dataStore.edit { it[Keys.WATCH_ACTIVE] = active }
    }

    suspend fun setWatchModeDurationHours(hours: Int) {
        context.dataStore.edit { it[Keys.WATCH_DURATION_H] = hours }
    }

    suspend fun setLandingLeadMin(min: Int) {
        context.dataStore.edit { it[Keys.LANDING_LEAD_MIN] = min }
    }

    suspend fun setAlertToggle(key: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (key) {
                "bars" -> prefs[Keys.ALERTS_BARS] = enabled
                "cooldowns" -> prefs[Keys.ALERTS_COOLDOWNS] = enabled
                "status" -> prefs[Keys.ALERTS_STATUS] = enabled
                "travel" -> prefs[Keys.ALERTS_TRAVEL] = enabled
                "stocks" -> prefs[Keys.ALERTS_STOCKS] = enabled
                "war" -> prefs[Keys.ALERTS_WAR] = enabled
            }
        }
    }

    suspend fun setScriptsKillSwitch(disabled: Boolean) {
        context.dataStore.edit { it[Keys.SCRIPTS_KILL_SWITCH] = disabled }
    }

    suspend fun setAutoLogin(enabled: Boolean, autoSubmit: Boolean) {
        context.dataStore.edit {
            it[Keys.AUTO_LOGIN_ENABLED] = enabled
            it[Keys.AUTO_LOGIN_SUBMIT] = autoSubmit
        }
    }

    suspend fun setOpenAttacksMode(mode: AttackBrowserMode) {
        context.dataStore.edit { it[Keys.OPEN_ATTACKS_MODE] = mode.name }
    }

    suspend fun setWarStakeoutLeadSec(sec: Int) {
        context.dataStore.edit { it[Keys.WAR_STAKEOUT_LEAD_SEC] = sec }
    }

    suspend fun setFlightAlertConfig(leadMin: Int, timing: FlightAlertTiming) {
        context.dataStore.edit {
            it[Keys.FLIGHT_ALERT_LEAD_MIN] = leadMin
            it[Keys.FLIGHT_ALERT_TIMING] = timing.name
        }
    }

    suspend fun setFfThresholds(low: Double, high: Double) {
        context.dataStore.edit {
            it[Keys.FF_LOW_THRESHOLD] = low
            it[Keys.FF_HIGH_THRESHOLD] = high
        }
    }

    suspend fun setManualWarFactionId(profileId: String, factionId: String) {
        if (profileId.isBlank()) return
        context.dataStore.edit { prefs ->
            val map = decodeMap<String>(prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON]).toMutableMap()
            if (factionId.isBlank()) map.remove(profileId) else map[profileId] = factionId
            prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON] = json.encodeToString(map)
        }
    }

    suspend fun setQuickLinks(profileId: String, links: List<QuickLinkItem>) {
        if (profileId.isBlank()) return
        context.dataStore.edit { prefs ->
            val map = decodeMap<List<QuickLinkItem>>(prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON]).toMutableMap()
            map[profileId] = links
            prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON] = json.encodeToString(map)
        }
    }

    suspend fun setBackgroundStockSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BG_STOCK_SYNC] = enabled }
    }

    suspend fun setBackgroundFlightSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BG_FLIGHT_SYNC] = enabled }
    }

    suspend fun recordSyncStatus(epochMs: Long, summary: String) {
        context.dataStore.edit {
            it[Keys.LAST_BG_SYNC_MS] = epochMs
            it[Keys.LAST_BG_SYNC_SUMMARY] = summary
        }
    }

    suspend fun setBrowserTabs(profileId: String, tabs: List<PersistedBrowserTab>, activeTabId: String) {
        if (profileId.isBlank()) return
        context.dataStore.edit { prefs ->
            val tabsMap = decodeMap<List<PersistedBrowserTab>>(prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON]).toMutableMap()
            tabsMap[profileId] = tabs
            prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON] = json.encodeToString(tabsMap)

            val activeMap = decodeMap<String>(prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON]).toMutableMap()
            activeMap[profileId] = activeTabId
            prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON] = json.encodeToString(activeMap)
        }
    }

    suspend fun clearProfilePrefs(profileId: String) {
        if (profileId.isBlank()) return
        context.dataStore.edit { prefs ->
            val tabsMap = decodeMap<List<PersistedBrowserTab>>(prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON]).toMutableMap()
            if (tabsMap.remove(profileId) != null) {
                prefs[Keys.BROWSER_TABS_BY_PROFILE_JSON] = json.encodeToString(tabsMap)
            }
            val activeMap = decodeMap<String>(prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON]).toMutableMap()
            if (activeMap.remove(profileId) != null) {
                prefs[Keys.ACTIVE_BROWSER_TAB_BY_PROFILE_JSON] = json.encodeToString(activeMap)
            }
            val linksMap = decodeMap<List<QuickLinkItem>>(prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON]).toMutableMap()
            if (linksMap.remove(profileId) != null) {
                prefs[Keys.QUICK_LINKS_BY_PROFILE_JSON] = json.encodeToString(linksMap)
            }
            val warMap = decodeMap<String>(prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON]).toMutableMap()
            if (warMap.remove(profileId) != null) {
                prefs[Keys.MANUAL_WAR_FACTION_BY_PROFILE_JSON] = json.encodeToString(warMap)
            }
        }
    }
}
