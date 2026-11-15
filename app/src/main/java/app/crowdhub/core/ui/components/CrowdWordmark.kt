package app.crowdhub.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.CrowdDisplayFontFamily

@Composable
fun CrowdWordmark(
    modifier: Modifier = Modifier,
    isHero: Boolean = false
) {
    val violetGradientBrush = Brush.verticalGradient(
        colors = listOf(
            CrowColors.BrandVioletStart,
            CrowColors.BrandVioletMid,
            CrowColors.BrandVioletEnd
        )
    )

    val fontSize = if (isHero) 34.sp else 21.sp
    val letterSpacing = if (isHero) 0.8.sp else 0.4.sp
    val glowBlur = if (isHero) 18f else 10f

    val wordmark = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = CrowColors.BrandWhite,
                fontFamily = CrowdDisplayFontFamily,
                fontWeight = FontWeight.Normal
            )
        ) {
            append("Crow")
        }
        withStyle(
            SpanStyle(
                brush = violetGradientBrush,
                fontFamily = CrowdDisplayFontFamily,
                fontWeight = FontWeight.Normal,
                shadow = Shadow(
                    color = CrowColors.Sheen.copy(alpha = 0.55f),
                    offset = Offset(0f, 0f),
                    blurRadius = glowBlur
                )
            )
        ) {
            append("d")
        }
        withStyle(
            SpanStyle(
                color = CrowColors.BrandHubMuted,
                fontFamily = CrowdDisplayFontFamily,
                fontWeight = FontWeight.Normal
            )
        ) {
            append("Hub")
        }
    }

    Text(
        text = wordmark,
        style = TextStyle(
            fontFamily = CrowdDisplayFontFamily,
            fontSize = fontSize,
            letterSpacing = letterSpacing
        ),
        maxLines = 1,
        modifier = modifier
    )
}
