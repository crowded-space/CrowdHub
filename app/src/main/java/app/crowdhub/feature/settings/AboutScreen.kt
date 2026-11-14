package app.crowdhub.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import app.crowdhub.BuildConfig
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.CrowdWordmark
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.provider.torn.TornRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
            CrowCard {
                CrowdWordmark()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Watch the flock.",
                    style = MaterialTheme.typography.titleSmall,
                    color = CrowColors.SheenSoft
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
            }

            CrowCard {
                Text("Disclaimer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(TornRules.UNOFFICIAL_DISCLAIMER, style = MaterialTheme.typography.bodyMedium, color = CrowColors.Text)
                Spacer(Modifier.height(8.dp))
                Text(TornRules.COMPLIANCE_REMINDER, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
            }

            CrowCard {
                Text("License", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Client-side personal companion app. No ads, no trackers, no third-party telemetry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
            }
        }
    }
}
