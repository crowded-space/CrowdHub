package app.crowdhub.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CrowDarkColorScheme = darkColorScheme(
    primary = CrowColors.Sheen,
    onPrimary = CrowColors.Text,
    primaryContainer = CrowColors.SurfaceHigh,
    onPrimaryContainer = CrowColors.SheenSoft,
    secondary = CrowColors.SheenSoft,
    onSecondary = CrowColors.CrowBlack,
    secondaryContainer = CrowColors.SurfaceHigh,
    onSecondaryContainer = CrowColors.SheenSoft,
    tertiary = CrowColors.Info,
    onTertiary = CrowColors.CrowBlack,
    background = CrowColors.CrowBlack,
    onBackground = CrowColors.Text,
    surface = CrowColors.Surface,
    onSurface = CrowColors.Text,
    surfaceVariant = CrowColors.SurfaceHigh,
    onSurfaceVariant = CrowColors.TextMuted,
    outline = CrowColors.Outline,
    outlineVariant = CrowColors.Outline,
    error = CrowColors.Danger,
    onError = CrowColors.CrowBlack
)

@Composable
fun CrowdHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CrowDarkColorScheme,
        typography = CrowTypography,
        shapes = CrowShapes,
        content = content
    )
}
