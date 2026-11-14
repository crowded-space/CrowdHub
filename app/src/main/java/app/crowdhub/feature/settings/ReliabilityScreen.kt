package app.crowdhub.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.crowdhub.core.alerts.Scheduler
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityScreen(
    scheduler: Scheduler,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var exactAlarmsOk by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var notificationsOk by remember { mutableStateOf(scheduler.hasNotificationPermission()) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
