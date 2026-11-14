package app.crowdhub.feature.scripts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.profile.ActiveProfileState
import app.crowdhub.core.profile.ProfileRepository
import app.crowdhub.core.scripts.ParsedUserscriptMeta
import app.crowdhub.core.scripts.ScriptInstallResult
import app.crowdhub.core.scripts.ScriptRepository
import app.crowdhub.core.scripts.UserscriptMetaParser
import app.crowdhub.core.settings.AppPreferences
import app.crowdhub.core.settings.SettingsStore
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(
    profileRepository: ProfileRepository,
    scriptRepository: ScriptRepository,
    settingsStore: SettingsStore,
    onBack: () -> Unit,
    onEditScript: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val activeProfile by profileRepository.observeActiveState().collectAsStateWithLifecycle(
        ActiveProfileState(null, null, emptyList())
    )
    val prefs by settingsStore.preferencesFlow.collectAsStateWithLifecycle(AppPreferences())
    val accountId = activeProfile.primaryAccount?.id ?: "default"
    val scripts by scriptRepository.observeScripts(accountId).collectAsStateWithLifecycle(emptyList())

    var showInstallDialog by remember { mutableStateOf(false) }
    var installUrl by remember { mutableStateOf("") }
    var installSource by remember { mutableStateOf("") }
    var allowApiKey by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }
    var previewMeta by remember { mutableStateOf<ParsedUserscriptMeta?>(null) }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Userscripts") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    installUrl = ""
                    installSource = ""
                    allowApiKey = false
                    installError = null
                    previewMeta = null
                    showInstallDialog = true
                },
                containerColor = CrowColors.Sheen,
                contentColor = CrowColors.Text,
                modifier = Modifier.testTag("add_userscript_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Install userscript")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CrowCard(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Global Kill Switch", fontWeight = FontWeight.SemiBold, color = CrowColors.Text)
                        Text(
                            text = if (prefs.scriptsKillSwitch) "All script injection disabled" else "Enabled scripts inject on allowed hosts",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (prefs.scriptsKillSwitch) CrowColors.Danger else CrowColors.TextMuted
                        )
                    }
                    Switch(
                        checked = prefs.scriptsKillSwitch,
                        onCheckedChange = { off -> scope.launch { settingsStore.setScriptsKillSwitch(off) } }
                    )
                }
            }

            if (scripts.isEmpty()) {
                EmptyView(
                    title = "Nothing perched here yet.",
                    subtitle = "Tap + to install a PDA-compatible userscript from source or HTTPS URL."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        val matches = scriptRepository.decodeList(script.matches)
                        val connects = scriptRepository.decodeList(script.connects)
                        CrowCard(modifier = Modifier.clickable { onEditScript(script.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${script.name}${script.version?.let { " v$it" } ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CrowColors.Text
                                    )
                                    Text(
                                        text = matches.joinToString(", ").ifEmpty { "No matches" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrowColors.TextMuted,
                                        maxLines = 1
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (script.allowApiKey) {
                                            Text("API key", style = MaterialTheme.typography.labelSmall, color = CrowColors.AccentD)
                                        }
                                        if (connects.isNotEmpty()) {
                                            Text("network", style = MaterialTheme.typography.labelSmall, color = CrowColors.Info)
                                        }
                                    }
                                    script.lastError?.let { err ->
                                        Text(
                                            text = "Last error: $err",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrowColors.Danger,
                                            maxLines = 2
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = script.enabled,
                                        onCheckedChange = { en -> scope.launch { scriptRepository.setEnabled(script.id, en) } }
                                    )
                                    IconButton(onClick = { scope.launch { scriptRepository.deleteScript(script.id) } }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete script", tint = CrowColors.Danger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Install Userscript") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are responsible for scripts you install. Torn forbids automation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.AccentD
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = installUrl,
                            onValueChange = { installUrl = it },
                            label = { Text("HTTPS URL (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    installError = null
                                    val fetched = scriptRepository.fetchSourceFromUrl(installUrl)
                                    fetched.onSuccess {
                                        installSource = it
                                        previewMeta = UserscriptMetaParser.parse(it, TornUrls.ALLOWED_WEB_HOSTS)
                                    }.onFailure {
                                        installError = it.message ?: "Fetch failed"
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Link, contentDescription = "Fetch URL")
                        }
                    }
                    OutlinedTextField(
                        value = installSource,
                        onValueChange = {
                            installSource = it
                            previewMeta = UserscriptMetaParser.parse(it, TornUrls.ALLOWED_WEB_HOSTS)
                        },
                        label = { Text("Script source") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    previewMeta?.let { meta ->
                        Text(
                            text = "Name: ${meta.name} · Run: ${meta.runAt.name} · Matches: ${meta.matches.size} · Connects: ${meta.connects.joinToString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrowColors.SheenSoft
                        )
                        meta.warnings.forEach { w ->
                            Text(w, style = MaterialTheme.typography.labelSmall, color = CrowColors.AccentD)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow API key placeholder", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Substitutes ###PDA-APIKEY###; script can read your key.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrowColors.Danger
                            )
                        }
                        Switch(checked = allowApiKey, onCheckedChange = { allowApiKey = it })
                    }
                    if (installError != null) {
                        Text(installError.orEmpty(), color = CrowColors.Danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            when (val res = scriptRepository.installOrUpdate(
                                existingId = null,
                                accountId = accountId,
                                source = installSource,
                                allowApiKey = allowApiKey,
                                installedFromUrl = installUrl.trim().takeIf { it.isNotEmpty() },
                                allowedHosts = TornUrls.ALLOWED_WEB_HOSTS
                            )) {
                                is ScriptInstallResult.Success -> showInstallDialog = false
                                is ScriptInstallResult.Error -> installError = res.message
                            }
                        }
                    },
                    enabled = installSource.isNotBlank()
                ) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Cancel") }
            }
        )
    }
}
