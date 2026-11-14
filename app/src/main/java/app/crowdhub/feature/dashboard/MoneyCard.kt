package app.crowdhub.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.components.CrowCard
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import app.crowdhub.provider.torn.TornMoney
import app.crowdhub.provider.torn.TornNotifications
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MoneyCard(
    money: TornMoney,
    dimmed: Boolean
) {
    if (money.cashOnHand == null && money.points == null) return
    val fmt = NumberFormat.getNumberInstance(Locale.US)

    CrowCard(dimmed = dimmed) {
        Text(
            text = "Money",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = CrowColors.Text
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            money.cashOnHand?.let { cash ->
                Column {
                    Text("Cash on hand", style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                    Text(
                        text = "$${fmt.format(cash)}",
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = CrowColors.Ok,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            money.points?.let { pts ->
                Column {
                    Text("Points", style = MaterialTheme.typography.labelSmall, color = CrowColors.TextMuted)
                    Text(
                        text = fmt.format(pts),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumberStyle),
                        color = CrowColors.AccentD,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EventsCard(
    notifications: TornNotifications,
    dimmed: Boolean,
    onOpenTorn: () -> Unit
) {
    CrowCard(
        dimmed = dimmed,
        modifier = Modifier.clickable(onClick = onOpenTorn)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Notifications & Events",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CrowColors.Text
            )
            Text(
                text = "Open in Browser",
                style = MaterialTheme.typography.labelSmall,
                color = CrowColors.SheenSoft
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Msgs: ${notifications.unreadMessages}", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
            Text("Events: ${notifications.unreadEvents}", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
            Text("Awards: ${notifications.unreadAwards}", style = MaterialTheme.typography.labelMedium, color = CrowColors.TextMuted)
        }
        if (notifications.recentEvents.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                notifications.recentEvents.forEach { ev ->
                    Text(
                        text = "• ${ev.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrowColors.Text,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
