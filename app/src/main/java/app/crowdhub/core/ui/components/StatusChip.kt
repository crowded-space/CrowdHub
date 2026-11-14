package app.crowdhub.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.theme.CrowColors

enum class StatusKind(val label: String) {
    OKAY("Okay"),
    HOSPITAL("Hospital"),
    TRAVELING("Traveling"),
    ABROAD("Abroad"),
    JAIL("Jail"),
    FEDERAL("Federal"),
    FALLEN("Fallen"),
    UNKNOWN("Unknown");

    companion object {
        fun fromRaw(raw: String?): StatusKind {
            return when (raw?.trim()?.uppercase()) {
                "OKAY", "OK" -> OKAY
                "HOSPITAL" -> HOSPITAL
                "TRAVELING" -> TRAVELING
                "ABROAD" -> ABROAD
                "JAIL" -> JAIL
                "FEDERAL" -> FEDERAL
                "FALLEN" -> FALLEN
                else -> UNKNOWN
            }
        }
    }
}

fun StatusKind.color(): Color = when (this) {
    StatusKind.OKAY -> CrowColors.Ok
    StatusKind.HOSPITAL, StatusKind.FALLEN -> CrowColors.Danger
    StatusKind.TRAVELING -> CrowColors.Info
    StatusKind.ABROAD -> CrowColors.Abroad
    StatusKind.JAIL, StatusKind.FEDERAL -> CrowColors.Jail
    StatusKind.UNKNOWN -> CrowColors.TextMuted
}

@Composable
fun StatusChip(
    status: StatusKind,
    modifier: Modifier = Modifier,
    overrideLabel: String? = null
) {
    val accent = status.color()
    val text = overrideLabel ?: status.label
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = text,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
