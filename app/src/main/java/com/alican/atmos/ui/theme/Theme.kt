package com.alican.atmos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alican.atmos.R

/** ATMOS palette — restrained, cinematic, expensive. */
object Atmos {
    val Midnight = Color(0xFF121824)
    val DeepNavy = Color(0xFF151E2C)
    val Charcoal = Color(0xFF1B2231)
    val Surface = Color(0xFF222A3A)
    val SurfaceHigh = Color(0xFF2A3345)
    val SurfaceLift = Color(0xFF303A4E)

    val White = Color(0xFFFFFFFF)
    val Text = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFA6B2C4)
    val Muted = Color(0xFF748198)

    val Ice = Color(0xFF9CC2E0)
    val IceDim = Color(0xFF7FA6C6)
    val Gold = Color(0xFFE3BE7C)
    val GoldSoft = Color(0xFFC9A66B)
    val Storm = Color(0xFF8DAEF5)
    val Danger = Color(0xFFD97A72)
    val Good = Color(0xFF9BC4A8)

    val Hairline = Color(0x14FFFFFF)
    val HairlineStrong = Color(0x24FFFFFF)

    /** Scene backdrop, slightly different from UI black so the object floats. */
    val SkyTop = Color(0xFF1A2437)
    val SkyBottom = Color(0xFF0B1017)
}

@OptIn(ExperimentalTextApi::class)
private val Geist = FontFamily(
    Font(R.font.geist, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.geist, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.geist, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.geist, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@OptIn(ExperimentalTextApi::class)
val GeistMono = FontFamily(
    Font(R.font.geist_mono, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.geist_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist_mono, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
)

private val AtmosTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        letterSpacing = (-5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GeistMono,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.9.sp
    )
)

private val AtmosScheme = darkColorScheme(
    primary = Atmos.Ice,
    onPrimary = Atmos.Midnight,
    primaryContainer = Atmos.SurfaceHigh,
    onPrimaryContainer = Atmos.Text,
    secondary = Atmos.Gold,
    onSecondary = Atmos.Midnight,
    background = Atmos.Midnight,
    onBackground = Atmos.Text,
    surface = Atmos.Surface,
    onSurface = Atmos.Text,
    surfaceVariant = Atmos.SurfaceHigh,
    onSurfaceVariant = Atmos.Secondary,
    outline = Atmos.HairlineStrong,
    error = Atmos.Danger,
    scrim = Color(0xCC000000)
)

@Composable
fun AtmosTheme(content: @Composable () -> Unit) {
    // ATMOS is a deliberate dark experience in every system mode.
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = AtmosScheme,
        typography = AtmosTypography,
        content = content
    )
}
