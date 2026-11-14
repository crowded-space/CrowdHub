package app.crowdhub.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.BuildConfig
import app.crowdhub.core.network.ResponseCache
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.AttackBrowserMode
import app.crowdhub.core.settings.FlightAlertTiming
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.feature.stocks.StockRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    profileRepository: ProfileRepository,
    responseCache: ResponseCache,
    stockRepository: StockRepository,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenReliability: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDeveloperProbe: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    val activeProfile by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        ActiveProfileState(null, null, emptyList())
    )

    var versionTapCount by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearStocksConfirm by remember { mutableStateOf(false) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    var ffLowStr by remember(prefs.ffLowThreshold) { mutableStateOf(prefs.ffLowThreshold.toString()) }
    var ffHighStr by remember(prefs.ffHighThreshold) { mutableStateOf(prefs.ffHighThreshold.toString()) }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            feedbackBanner?.let { msg ->
                CrowCard(borderColor = CrowColors.Sheen) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = CrowColors.SheenSoft)
                }
            }

            activeProfile.primaryAccount?.let { acct ->
                CrowCard(modifier = Modifier.clickable { onOpenAccount(acct.id) }) {
                    Text("Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${acct.displayName ?: "Torn"} · Status: ${acct.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrowColors.TextMuted
                    )
                    Text(
                        text = "Manage API key, Web session, and FFScouter",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.SheenSoft
                    )
                }
            }

            CrowCard {
                Text("Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ToggleRow("Bar full alerts", prefs.alertsBarsEnabled) {
                    scope.launch { settingsStore.setAlertToggle("bars", it) }
                }
                ToggleRow("Cooldown ready alerts", prefs.alertsCooldownsEnabled) {
                    scope.launch { settingsStore.setAlertToggle("cooldowns", it) }
                }
                ToggleRow("Hospital / Jail release alerts", prefs.alertsStatusEnabled) {
                    scope.launch { settingsStore.setAlertToggle("status", it) }
                }
                ToggleRow("Travel landing alerts", prefs.alertsTravelEnabled) {
                    scope.launch { settingsStore.setAlertToggle("travel", it) }
                }
                ToggleRow("Stock price alerts", prefs.alertsStocksEnabled) {
                    scope.launch { settingsStore.setAlertToggle("stocks", it) }
                }
                ToggleRow("War stakeout alerts", prefs.alertsWarEnabled) {
                    scope.launch { settingsStore.setAlertToggle("war", it) }
                }

                Spacer(Modifier.height(8.dp))
                Text("Landing lead time", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 5).forEach { m ->
                        FilterChip(
                            selected = prefs.landingLeadMin == m,
                            onClick = { scope.launch { settingsStore.setLandingLeadMin(m) } },
                            label = { Text(if (m == 0) "At land" else "${m}m") }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("War stakeout lead", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 10, 30, 60, 120).forEach { s ->
                        FilterChip(
                            selected = prefs.warStakeoutLeadSec == s,
                            onClick = { scope.launch { settingsStore.setWarStakeoutLeadSec(s) } },
                            label = { Text("${s}s") }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Flight alert timing & lead", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlightAlertTiming.entries.forEach { timing ->
                        FilterChip(
                            selected = prefs.flightAlertTiming == timing,
                            onClick = {
                                scope.launch { settingsStore.setFlightAlertConfig(prefs.flightAlertLeadMin, timing) }
                            },
                            label = { Text(timing.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 5).forEach { lead ->
                        FilterChip(
                            selected = prefs.flightAlertLeadMin == lead,
                            onClick = {
                                scope.launch { settingsStore.setFlightAlertConfig(lead, prefs.flightAlertTiming) }
                            },
                            label = { Text("${lead}m") }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("System Notification Channels")
                }
            }

            CrowCard {
                Text("Watch Mode & Browser", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Watch Mode auto-stop", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 6).forEach { hrs ->
                        FilterChip(
                            selected = prefs.watchModeDurationHours == hrs,
                            onClick = { scope.launch { settingsStore.setWatchModeDurationHours(hrs) } },
                            label = { Text("${hrs}h") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Open attacks in", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttackBrowserMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.openAttacksMode == mode,
                            onClick = { scope.launch { settingsStore.setOpenAttacksMode(mode) } },
                            label = { Text(if (mode == AttackBrowserMode.IN_APP) "In-app" else "External") }
                        )
                    }
                }
            }

            CrowCard {
                Text("FFScouter Thresholds", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ffLowStr,
                        onValueChange = { ffLowStr = it },
                        label = { Text("Low (Green <=)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ffHighStr,
                        onValueChange = { ffHighStr = it },
                        label = { Text("Mid (Amber <=)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val low = ffLowStr.toDoubleOrNull() ?: 2.0
                        val high = (ffHighStr.toDoubleOrNull() ?: 3.0).coerceAtLeast(low)
                        scope.launch {
                            settingsStore.setFfThresholds(low, high)
                            feedbackBanner = "FF thresholds updated."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save thresholds") }
            }

            CrowCard {
                Text("Data & Reliability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearCacheConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clear cached API responses") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showClearStocksConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clear stock history") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenReliability,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Background & Alarm Reliability") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Privacy & Data Disclosure") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("About Crowd Hub") }
            }

            // C-11: Profiles reachable ONLY via Settings > Advanced
            CrowCard {
                Text("Advanced", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenProfiles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_advanced_profiles_button")
                ) { Text("Profiles (Isolated Containers)") }

                if (prefs.developerUnlocked) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onOpenDeveloperProbe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_developer_probe_button")
                    ) { Text("Developer: API Probe & Debug") }
                }
            }

            CrowCard(
                modifier = Modifier
                    .clickable {
                        if (!prefs.developerUnlocked) {
                            versionTapCount++
                            if (versionTapCount >= 7) {
                                scope.launch { settingsStore.setDeveloperUnlocked(true) }
                                feedbackBanner = "Developer tools unlocked."
                            }
                        }
                    }
                    .testTag("settings_version_row")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Version", color = CrowColors.Text)
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                }
            }
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear API Cache?") },
            text = { Text("Deletes cached API responses; next refresh will fetch fresh data from Torn.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            responseCache.clearAll()
                            showClearCacheConfirm = false
                            feedbackBanner = "API cache cleared."
                        }
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearStocksConfirm) {
        AlertDialog(
            onDismissRequest = { showClearStocksConfirm = false },
            title = { Text("Clear Stock History?") },
            text = { Text("Deletes locally recorded price snapshots used for charts and % move alerts.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            stockRepository.clearAllHistory()
                            showClearStocksConfirm = false
                            feedbackBanner = "Stock history cleared."
                        }
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearStocksConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CrowColors.Text)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
