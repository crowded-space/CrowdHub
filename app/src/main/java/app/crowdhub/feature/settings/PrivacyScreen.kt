package app.crowdhub.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Data Disclosure") },
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
                Text("Torn API Terms Disclosure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TornRules.API_DISCLOSURE.forEach { row ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(row.label, style = MaterialTheme.typography.labelMedium, color = CrowColors.SheenSoft)
                        Text(row.value, style = MaterialTheme.typography.bodyMedium, color = CrowColors.Text)
                    }
                }
            }

            CrowCard {
                Text("Where Your Data Lives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Torn API Key" to "Android Keystore AES-256-GCM encrypted in local database. Sent only to api.torn.com (and ffscouter.com if you explicitly opt in).",
                    "Web Credentials (optional)" to "Encrypted on device only. Typed into torn.com login form on the login page only if enabled.",
                    "Stock & Flight History" to "Stored locally in Room on this device. No analytics, telemetry, crash reporting, or developer server.",
                    "Userscripts & KV Storage" to "Stored locally on device. Scripts cannot access raw Keystore keys unless you enable ###PDA-APIKEY### per script."
                ).forEach { (title, desc) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(title, style = MaterialTheme.typography.labelMedium, color = CrowColors.Text)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                    }
                }
            }
        }
    }
}
