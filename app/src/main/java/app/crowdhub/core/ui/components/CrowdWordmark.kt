package app.crowdhub.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import app.crowdhub.core.ui.theme.CrowColors

@Composable
fun CrowdWordmark(
    modifier: Modifier = Modifier
) {
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = CrowColors.Text, fontWeight = FontWeight.Bold)) {
            append("Crow")
        }
        withStyle(SpanStyle(color = CrowColors.AccentD, fontWeight = FontWeight.ExtraBold)) {
            append("d")
        }
        withStyle(SpanStyle(color = CrowColors.TextMuted, fontWeight = FontWeight.SemiBold)) {
            append("Hub")
        }
    }
    Text(
        text = wordmark,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}
