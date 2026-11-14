package app.crowdhub.feature.flight

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.MonospaceNumberStyle
import app.crowdhub.provider.torn.FlightTable
import app.crowdhub.provider.torn.PlaneCategory
import app.crowdhub.provider.torn.TravelPlace
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDebugScreen(
    flightRepository: FlightRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mappings by flightRepository.observePlaneTypeMap().collectAsStateWithLifecycle(emptyList())
    val observations by flightRepository.observeRecentObservations().collectAsStateWithLifecycle(emptyList())
    val observedCounts = remember(observations) { flightRepository.getObservedPlaneTypeCounts() }

    var newApiValue by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaneCategory.STANDARD) }
    var catMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text("Flight Debug") },
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
                Text("Plane Type Mapping", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Built-in defaults from API samples: " +
                        FlightTable.defaultPlaneTypeMap.entries.joinToString(", ") { "${it.key} -> ${it.value.label}" } +
                        ". Add overrides below if needed (e.g., private_jet -> Business).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newApiValue,
                    onValueChange = { newApiValue = it },
                    label = { Text("API plane_image_type value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = catMenuExpanded,
                    onExpandedChange = { catMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plane Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        PlaneCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    selectedCategory = cat
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            flightRepository.setPlaneTypeMapping(newApiValue, selectedCategory)
                            newApiValue = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save mapping")
                }

                if (mappings.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    mappings.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${m.apiValue} -> ${m.category}", color = CrowColors.Text)
                            IconButton(
                                onClick = { scope.launch { flightRepository.deletePlaneTypeMapping(m.apiValue) } }
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete mapping", tint = CrowColors.Danger)
                            }
                        }
                    }
                }
            }

            if (observedCounts.isNotEmpty()) {
                CrowCard {
                    Text("Observed plane_image_type values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    observedCounts.forEach { (value, count) ->
                        Text("$value (seen $count times)", style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                    }
                }
            }

            CrowCard {
                Text("FlightTable (min, without book)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FlightTable.tableMinutes.forEach { (place, row) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(place.displayName, style = MaterialTheme.typography.labelSmall, color = CrowColors.Text)
                        Text(
                            text = "${row[0]} / ${row[1]} / ${row[2]} / ${row[3]}",
                            style = MaterialTheme.typography.labelSmall.merge(MonospaceNumberStyle),
                            color = CrowColors.TextMuted
                        )
                    }
                }
            }

            CrowCard {
                Text("Recent Observations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (observations.isEmpty()) {
                    Text("Nothing perched here yet.", style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
                } else {
                    observations.take(20).forEach { obs ->
                        val minMin = obs.observedMinMs?.div(60_000L)
                        val maxMin = obs.observedMaxMs?.div(60_000L)
                        val suggestion = if (obs.observedMinMs != null && obs.observedMaxMs != null) {
                            val placeEnum = runCatching { TravelPlace.valueOf(obs.place) }.getOrDefault(TravelPlace.UNKNOWN)
                            flightRepository.estimator.suggestCategoryFromObservation(
                                placeEnum,
                                obs.inferredBook,
                                obs.observedMinMs,
                                obs.observedMaxMs
                            )
                        } else null
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Player ${obs.playerId} · ${obs.place} (${obs.direction}) · plane=${obs.planeType ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CrowColors.Text
                            )
                            Text(
                                text = "Observed: ${minMin ?: "?"}-${maxMin ?: "?"}m · book=${obs.inferredBook ?: "?"}${suggestion?.let { " · suggested=${it.name}" } ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrowColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
