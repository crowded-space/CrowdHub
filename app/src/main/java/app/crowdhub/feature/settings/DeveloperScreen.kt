package app.crowdhub.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.network.RateLimitConfig
import app.crowdhub.core.network.RateLimiter
import app.crowdhub.core.network.RateLimiterSnapshot
import app.crowdhub.core.network.ServerClock
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.ProbeResponse
import app.crowdhub.provider.torn.TornApi
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    profileRepository: ProfileRepository,
    tornApi: TornApi,
    rateLimiter: RateLimiter,
    clock: ServerClock,
    onBack: () -> Unit,
    onOpenFlightDebug: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeProfile by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        ActiveProfileState(null, null, emptyList())
    )
    val accountId = activeProfile.primaryAccount?.id ?: "default"

    val selections = remember {
        listOf(
            "user/basic",
            "user/bars",
            "user/cooldowns",
            "user/travel",
            "user/money",
            "user/notifications",
            "user/stocks",
            "torn/stocks",
            "faction/{id}/rankedwars",
            "faction/{id}/members",
            "user/{id}/profile"
        )
    }

    var selectedEndpoint by remember { mutableStateOf(selections.first()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var paramId by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var probeResult by remember { mutableStateOf<ProbeResponse?>(null) }
    var rateSnapshot by remember {
        mutableStateOf(RateLimiterSnapshot(0, 0, 0, 0, 0, 0L, 0L))
    }
    var copiedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        rateSnapshot = rateLimiter.snapshot(accountId)
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Developer · API Probe") },
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
            CrowCard {
                Text("Rate Limiter & Server Clock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                val appClasses = rateSnapshot.userLastMinute + rateSnapshot.watchLastMinute + rateSnapshot.backgroundLastMinute
                Text(
                    text = "60s window: Total ${rateSnapshot.totalLastMinute}/${RateLimitConfig.GLOBAL_CAP_PER_MIN} · " +
                        "App $appClasses/${RateLimitConfig.APP_TOTAL_CAP_PER_MIN} · " +
                        "USER ${rateSnapshot.userLastMinute} · WATCH ${rateSnapshot.watchLastMinute} · " +
                        "BG ${rateSnapshot.backgroundLastMinute} · SCRIPT ${rateSnapshot.scriptLastMinute}",
                    style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                    color = CrowColors.Text
                )
                val offsetMs = clock.offsetMs()
                Text(
                    text = "Server Date: ${clock.lastServerDateHeader ?: "—"} · Clock offset: ${offsetMs}ms",
                    style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                    color = CrowColors.TextMuted
                )
                if (abs(offsetMs) > 5 * 60_000L) {
                    Text(
                        text = "Warning: device clock differs from Torn server by > 5 minutes. Countdown math uses ServerClock.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrowColors.AccentD
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenFlightDebug,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Flight Estimator Debug")
                }
            }

            CrowCard {
                Text("API Probe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEndpoint,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Endpoint") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        selections.forEach { sel ->
                            DropdownMenuItem(
                                text = { Text(sel) },
                                onClick = {
                                    selectedEndpoint = sel
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedEndpoint.contains("{id}")) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = paramId,
                        onValueChange = { paramId = it.filter(Char::isDigit) },
                        label = { Text("ID parameter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isRunning = true
                            copiedNotice = false
                            val res = tornApi.probeSelection(accountId, selectedEndpoint, paramId)
                            probeResult = res
                            rateSnapshot = rateLimiter.snapshot(accountId)
                            isRunning = false
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_run_probe_button")
                ) {
                    Text(if (isRunning) "Probing..." else "Run Probe")
                }
            }

            probeResult?.let { res ->
                val raw = res.rawRedactedJson
                val truncated = if (raw.length > 200_000) raw.take(200_000) + "\n... [truncated > 200 KB]" else raw
                CrowCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Response (${res.path})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("CrowdHub Probe", truncated))
                                copiedNotice = true
                            }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy redacted JSON")
                        }
                    }
                    if (copiedNotice) {
                        Text("Copied redacted JSON.", style = MaterialTheme.typography.labelSmall, color = CrowColors.Ok)
                    }
                    res.error?.let { err ->
                        Text("Error: ${err.message}", style = MaterialTheme.typography.bodySmall, color = CrowColors.Danger)
                    }
                    if (truncated.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = truncated,
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = CrowColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}
