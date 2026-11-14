package app.crowdhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyOff
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.network.ApiError
import app.crowdhub.core.ui.theme.CrowColors

@Composable
fun CrowCard(
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    borderColor: Color = CrowColors.Outline,
    containerColor: Color = CrowColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.72f else 1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun LoadingView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = CrowColors.Sheen,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = message,
                color = CrowColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyView(
    title: String = "Nothing perched here yet.",
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = CrowColors.TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                color = CrowColors.Text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = CrowColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorView(
    error: ApiError,
    onRetry: (() -> Unit)? = null,
    onFixKey: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CrowCard(borderColor = CrowColors.Danger.copy(alpha = 0.5f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (error.isPermanentKeyFailure()) Icons.Outlined.KeyOff else Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = CrowColors.Danger
                )
                Text(
                    text = error.message,
                    color = CrowColors.Text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onFixKey != null && error.isPermanentKeyFailure()) {
                    Button(
                        onClick = onFixKey,
                        colors = ButtonDefaults.buttonColors(containerColor = CrowColors.AccentD, contentColor = CrowColors.CrowBlack),
                        modifier = Modifier.testTag("fix_api_key_button")
                    ) {
                        Text("Fix API key")
                    }
                }
                if (onRetry != null && !error.isPermanentKeyFailure()) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("retry_button")
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun KeyProblemBanner(
    message: String = "API key problem — tap to fix",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CrowColors.Danger.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("api_key_problem_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.KeyOff,
            contentDescription = "Key problem",
            tint = CrowColors.Danger
        )
        Text(
            text = message,
            color = CrowColors.Danger,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OfflineBanner(
    message: String = "Offline or service error — showing cached data",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CrowColors.AccentD.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = "Offline",
            tint = CrowColors.AccentD,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            color = CrowColors.AccentD,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
