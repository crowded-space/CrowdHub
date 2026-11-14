package app.crowdhub.feature.scripts

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.scripts.ScriptInstallResult
import app.crowdhub.core.scripts.ScriptRepository
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.TornUrls
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: String,
    scriptRepository: ScriptRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val logs by scriptRepository.observeLogs(scriptId).collectAsStateWithLifecycle(emptyList())

    var accountId by remember { mutableStateOf("default") }
    var source by remember { mutableStateOf("") }
    var allowApiKey by remember { mutableStateOf(false) }
    var installUrl by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scriptId) {
        scriptRepository.getById(scriptId)?.let { entity ->
            accountId = entity.accountId
            source = entity.source
            allowApiKey = entity.allowApiKey
            installUrl = entity.installedFromUrl
        }
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Edit Script") },
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
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Source code") },
                minLines = 10,
                maxLines = 18,
                textStyle = MaterialTheme.typography.bodySmall.merge(MonospaceNumberStyle),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow ###PDA-APIKEY### substitution", color = CrowColors.Text)
                Switch(checked = allowApiKey, onCheckedChange = { allowApiKey = it })
            }
            if (statusMessage != null) {
                Text(statusMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = CrowColors.SheenSoft)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            when (val res = scriptRepository.installOrUpdate(
                                existingId = scriptId,
                                accountId = accountId,
                                source = source,
                                allowApiKey = allowApiKey,
                                installedFromUrl = installUrl,
                                allowedHosts = TornUrls.ALLOWED_WEB_HOSTS
                            )) {
                                is ScriptInstallResult.Success -> statusMessage = "Saved."
                                is ScriptInstallResult.Error -> statusMessage = res.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            scriptRepository.clearScriptStorage(scriptId)
                            statusMessage = "Script KV storage cleared."
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear storage") }
            }

            if (!installUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val fetched = scriptRepository.fetchSourceFromUrl(installUrl!!)
                            fetched.onSuccess {
                                source = it
                                statusMessage = "Fetched latest from URL. Tap Save to apply."
                            }.onFailure {
                                statusMessage = it.message ?: "Update check failed"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Check for update (manual)") }
            }

            CrowCard {
                Text("Script Logs (last 200)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (logs.isEmpty()) {
                    Text("No logs recorded.", style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                } else {
                    logs.forEach { log ->
                        Text(
                            text = "[${log.level}] ${log.message}",
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = if (log.level == "ERROR") CrowColors.Danger else CrowColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}
