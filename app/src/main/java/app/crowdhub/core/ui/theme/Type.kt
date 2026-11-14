package app.crowdhub.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val CrowTypography = Typography()

val TabularNumberStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontFeatureSettings = "tnum"
)

val MonospaceNumberStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontFeatureSettings = "tnum"
)
