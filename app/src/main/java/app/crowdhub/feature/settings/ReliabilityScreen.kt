package app.crowdhub.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.alerts.AlertEngine
import app.crowdhub.core.alerts.AlertTriggerMode
import app.crowdhub.core.alerts.PollWorker
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.alerts.WatchModeService
import app.crowdhub.core.network.Clock
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.formatRelativeAge
import app.crowdhub.core.ui.theme.CrowColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityScreen(
    scheduler: Scheduler,
    settingsStore: SettingsStore,
    alertEngine: AlertEngine,
    clock: Clock,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    var exactAlarmsOk by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var notificationsOk by remember { mutableStateOf(scheduler.hasNotificationPermission()) }
    var syncingNow by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactAlarmsOk = scheduler.canScheduleExactAlarms()
        notificationsOk = scheduler.hasNotificationPermission()
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Reliability & Battery") },
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
            CrowCard(borderColor = if (prefs.watchModeActive) CrowColors.Ok.copy(alpha = 0.5f) else CrowColors.Outline) {
                Text("Background Sync Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (prefs.lastBackgroundSyncEpochMs > 0L) {
                        val age = formatRelativeAge(clock.nowEpochMs() - prefs.lastBackgroundSyncEpochMs)
                        "${prefs.lastBackgroundSyncSummary.ifBlank { "Sync" }} · $age"
                    } else {
                        "Periodic 15-minute sync runs via WorkManager. Enable Watch Mode for 30s live polling."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
                Spacer(Modifier.height(8.dp))
                ReliabilityToggleRow(
                    label = "Watch Mode (30s foreground service)",
                    checked = prefs.watchModeActive,
                    testTag = "reliability_watch_switch"
                ) { enable ->
                    if (enable) WatchModeService.start(context) else WatchModeService.stop(context)
                }
                ReliabilityToggleRow(
                    label = "Background stock snapshot sync",
                    checked = prefs.backgroundStockSyncEnabled,
                    testTag = "reliability_bg_stocks_switch"
                ) { enable ->
                    scope.launch { settingsStore.setBackgroundStockSync(enable) }
                }
                ReliabilityToggleRow(
                    label = "Background flight ETA sync",
                    checked = prefs.backgroundFlightSyncEnabled,
                    testTag = "reliability_bg_flight_switch"
                ) { enable ->
                    scope.launch { settingsStore.setBackgroundFlightSync(enable) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (!syncingNow) {
                            syncingNow = true
                            scope.launch {
                                PollWorker.enqueue(context)
                                runCatching {
                                    alertEngine.runCycle(AlertTriggerMode.BACKGROUND)
                                    settingsStore.recordSyncStatus(
                                        clock.nowEpochMs(),
                                        "Manual background sync completed"
                                    )
                                }
                                syncingNow = false
                            }
                        }
                    },
                    enabled = !syncingNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reliability_sync_now_button")
                ) {
                    Text(if (syncingNow) "Syncing..." else "Run background sync now")
                }
            }

            CrowCard(borderColor = if (exactAlarmsOk) CrowColors.Ok.copy(alpha = 0.4f) else CrowColors.AccentD) {
                Text("Exact Alarms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (exactAlarmsOk) {
                        "Exact alarms permitted — bar, cooldown, hospital, and landing timers fire on time."
                    } else {
                        "Timers may be delayed because exact alarm access is currently off (falling back to inexact alarms)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exactAlarmsOk) CrowColors.Ok else CrowColors.AccentD
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmsOk) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Exact Alarm Settings")
                    }
                }
            }

            CrowCard {
                Text("Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (notificationsOk) "Notifications enabled." else "Notifications blocked — alerts cannot appear until enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notificationsOk) CrowColors.Ok else CrowColors.Danger
                )
                Spacer(Modifier.height(10.dp))
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
                    Text("Open App Notification Settings")
                }
            }

            CrowCard {
                Text("Battery Optimization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "If your phone aggressively kills background work or delays 15-minute polls, set Crowd Hub to Unrestricted in Android Battery settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Battery Optimization Settings")
                }
            }
        }
    }
}

@Composable
private fun ReliabilityToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CrowColors.Text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

