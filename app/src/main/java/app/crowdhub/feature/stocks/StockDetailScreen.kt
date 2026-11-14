package app.crowdhub.feature.stocks

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
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.crowdhub.core.db.StockWatchEntity
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.components.EmptyView
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockId: Int,
    viewModel: StocksViewModel,
    onBack: () -> Unit
) {
    val allWatches by viewModel.watches.collectAsStateWithLifecycle()
    val portfolioState by viewModel.portfolioState.collectAsStateWithLifecycle()
    val stockWatches = remember(allWatches, stockId) { allWatches.filter { it.stockId == stockId } }
    val stock = viewModel.findStock(stockId)
    val position = remember(portfolioState, stockId) {
        (portfolioState as? PortfolioUiState.Content)
            ?.summary
            ?.positions
            ?.firstOrNull { it.stock.stockId == stockId }
            ?: viewModel.findPosition(stockId)
    }

    var selectedRange by remember { mutableStateOf(ChartRange.H24) }
    var history by remember { mutableStateOf<List<StockPricePoint>>(emptyList()) }
    var editingWatch by remember { mutableStateOf<StockWatchEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(stockId, selectedRange) {
        history = viewModel.loadHistory(stockId, selectedRange)
    }

    Scaffold(
        containerColor = CrowColors.CrowBlack,
        topBar = {
            TopAppBar(
                title = { Text(stock?.acronym ?: "Stock #$stockId") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CrowColors.Surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (stock != null) {
                        IconButton(
                            onClick = {
                                editingWatch = null
                                showSheet = true
                            },
                            modifier = Modifier.testTag("add_stock_alert_button")
                        ) {
                            Icon(Icons.Outlined.AddAlert, contentDescription = "Add price alert")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (stock == null) {
            EmptyView(
                title = "Stock not found",
                subtitle = "Pull to refresh the Stocks list first.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val numFmt = NumberFormat.getNumberInstance(Locale.US)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CrowCard {
                Text(
                    text = "${stock.name} (${stock.acronym})",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrowColors.TextMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$${"%.2f".format(stock.currentPrice)}",
                    style = MaterialTheme.typography.headlineMedium.merge(TabularNumberStyle),
                    fontWeight = FontWeight.Bold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PriceChart(points = history, modifier = Modifier.fillMaxWidth())
            }

            StockPositionDetailCard(
                stock = stock,
                position = position
            )

            CrowCard {
                Text(
                    text = "Market Stats",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CrowColors.Text
                )
                Spacer(Modifier.height(8.dp))
                stock.marketCap?.let { StatRow("Market Cap", "$${numFmt.format(it)}") }
                stock.totalShares?.let { StatRow("Total Shares", numFmt.format(it)) }
                stock.availableShares?.let { StatRow("Available Shares", numFmt.format(it)) }
                stock.forecast?.let { StatRow("Forecast", it) }
                stock.demand?.let { StatRow("Demand", it) }
                stock.benefitDescription?.let { desc ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Benefit (${stock.benefitRequirement?.let { numFmt.format(it) } ?: "?"} shares): $desc",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.SheenSoft
                    )
                }
            }

            CrowCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CrowColors.Text
                    )
                    Button(
                        onClick = {
                            editingWatch = null
                            showSheet = true
                        }
                    ) {
                        Text("Add alert")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (stockWatches.isEmpty()) {
                    Text(
                        text = "Nothing perched here yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stockWatches.forEach { w ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val summary = buildList {
                                        w.aboveTarget?.let { add("Above $${"%.2f".format(it)}") }
                                        w.belowTarget?.let { add("Below $${"%.2f".format(it)}") }
                                        if (w.pctMove != null && w.pctWindowMin != null) {
                                            add("${w.pctMove}% / ${w.pctWindowMin}m")
                                        }
                                    }.joinToString(" · ")
                                    Text(summary.ifEmpty { "Alert" }, color = CrowColors.Text, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (w.repeating) "Repeating (${w.cooldownMin}m cooldown)" else "One-shot",
                                        color = CrowColors.TextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = w.enabled,
                                        onCheckedChange = { viewModel.toggleWatch(w, it) }
                                    )
                                    IconButton(onClick = { viewModel.deleteWatch(w.id) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete alert", tint = CrowColors.Danger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        var aboveStr by remember(editingWatch) { mutableStateOf(editingWatch?.aboveTarget?.toString().orEmpty()) }
        var belowStr by remember(editingWatch) { mutableStateOf(editingWatch?.belowTarget?.toString().orEmpty()) }
        var pctStr by remember(editingWatch) { mutableStateOf(editingWatch?.pctMove?.toString().orEmpty()) }
        var windowStr by remember(editingWatch) { mutableStateOf((editingWatch?.pctWindowMin ?: 60).toString()) }
        var repeating by remember(editingWatch) { mutableStateOf(editingWatch?.repeating ?: true) }
        var cooldownStr by remember(editingWatch) { mutableStateOf((editingWatch?.cooldownMin ?: 60).toString()) }

        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = CrowColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Stock Alert Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = aboveStr,
                    onValueChange = { aboveStr = it },
                    label = { Text("Price above ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = belowStr,
                    onValueChange = { belowStr = it },
                    label = { Text("Price below ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pctStr,
                        onValueChange = { pctStr = it },
                        label = { Text("% move") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = windowStr,
                        onValueChange = { windowStr = it },
                        label = { Text("Window (5-1440m)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Repeating alert")
                    Switch(checked = repeating, onCheckedChange = { repeating = it })
                }
                if (repeating) {
                    OutlinedTextField(
                        value = cooldownStr,
                        onValueChange = { cooldownStr = it },
                        label = { Text("Cooldown (minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        val above = aboveStr.trim().toDoubleOrNull()
                        val below = belowStr.trim().toDoubleOrNull()
                        val pct = pctStr.trim().toDoubleOrNull()
                        val win = windowStr.trim().toIntOrNull()?.coerceIn(5, 1440)
                        val cd = cooldownStr.trim().toIntOrNull()?.coerceAtLeast(1) ?: 60
                        if (above != null || below != null || pct != null) {
                            viewModel.saveWatch(
                                id = editingWatch?.id,
                                stockId = stockId,
                                aboveTarget = above,
                                belowTarget = below,
                                pctMove = pct,
                                pctWindowMin = if (pct != null) win else null,
                                repeating = repeating,
                                cooldownMin = cd
                            )
                            showSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_stock_alert_button")
                ) {
                    Text("Save Alert")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CrowColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall.merge(TabularNumberStyle), color = CrowColors.Text)
    }
}
