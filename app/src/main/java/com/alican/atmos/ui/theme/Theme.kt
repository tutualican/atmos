package com.alican.atmos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alican.atmos.R

/** ATMOS palette — restrained, cinematic, expensive. Dynamic where the sky is. */
object Atmos {
    private var atmosphere: AtmosAtmosphere by mutableStateOf(defaultAtmosphere())

    /** Applies the palette that matches the current weather and the local time of the place. */
    fun applyAtmosphere(kind: com.alican.atmos.core.WeatherKind, isDay: Boolean, hour: Int) {
        val next = atmosphereFor(kind, isDay, hour)
        if (next != atmosphere) atmosphere = next
    }

    val Midnight: Color get() = atmosphere.background
    val DeepNavy: Color get() = atmosphere.backgroundAlt
    val Charcoal: Color get() = atmosphere.surface
    val Surface: Color get() = atmosphere.surface
    val SurfaceHigh: Color get() = atmosphere.surfaceAlt
    val SurfaceLift: Color get() = atmosphere.surfaceLift

    val White = Color(0xFFFFFFFF)
    val Text = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFA6B2C4)
    val Muted = Color(0xFF748198)

    val IceBase = Color(0xFF9CC2E0)
    val IceDimBase = Color(0xFF7FA6C6)

    /**
     * Optional Material You accent. Only the accent shifts; the atmosphere palette stays
     * intact so the identity of the app does not dissolve into wallpaper colours.
     */
    var accentOverride: Color? by mutableStateOf(null)
        private set

    val Ice: Color get() = accentOverride ?: atmosphere.accent
    val IceDim: Color get() = accentOverride?.copy(alpha = 0.82f) ?: atmosphere.accent.copy(alpha = 0.82f)

    fun applySystemAccent(context: android.content.Context, enabled: Boolean) {
        accentOverride = if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            runCatching { Color(context.getColor(android.R.color.system_accent1_200)) }.getOrNull()
        } else {
            null
        }
    }

    /**
     * Readable mode: enlarges every text style and the few hard-coded display sizes.
     * It never changes the palette or hides data — only scale.
     */
    var largeText: Boolean by mutableStateOf(false)
        private set

    val textScale: Float
        get() = if (largeText) 1.18f else 1f

    fun applyReadable(enabled: Boolean) {
        largeText = enabled
    }

    val Gold = Color(0xFFE3BE7C)
    val GoldSoft = Color(0xFFC9A66B)
    val Storm = Color(0xFF8DAEF5)
    val Danger = Color(0xFFD97A72)
    val Good = Color(0xFF9BC4A8)

    val Hairline = Color(0x14FFFFFF)
    val HairlineStrong = Color(0x24FFFFFF)

    /** Warm accent for the sun, dawn and dusk. */
    val Warm: Color get() = atmosphere.warm

    /** Scene backdrop, kept in sync with the page background. */
    val SkyTop: Color get() = atmosphere.skyTop
    val SkyMid: Color get() = atmosphere.skyMid
    val SkyBottom: Color get() = atmosphere.skyBottom
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
    val typography = if (Atmos.largeText) rememberReadableTypography() else AtmosTypography
    MaterialTheme(
        colorScheme = AtmosScheme,
        typography = typography,
        content = content
    )
}

/** Every text style gets proportionally larger when readable mode is on. */
@Composable
private fun rememberReadableTypography(): Typography = androidx.compose.runtime.remember {
    val f = Atmos.textScale
    fun TextStyle.bigger() = copy(
        fontSize = fontSize * f,
        lineHeight = lineHeight * f,
        letterSpacing = letterSpacing
    )
    Typography(
        displayLarge = AtmosTypography.displayLarge.bigger(),
        displayMedium = AtmosTypography.displayMedium.bigger(),
        displaySmall = AtmosTypography.displaySmall.bigger(),
        headlineLarge = AtmosTypography.headlineLarge.bigger(),
        headlineMedium = AtmosTypography.headlineMedium.bigger(),
        headlineSmall = AtmosTypography.headlineSmall.bigger(),
        titleLarge = AtmosTypography.titleLarge.bigger(),
        titleMedium = AtmosTypography.titleMedium.bigger(),
        titleSmall = AtmosTypography.titleSmall.bigger(),
        bodyLarge = AtmosTypography.bodyLarge.bigger(),
        bodyMedium = AtmosTypography.bodyMedium.bigger(),
        bodySmall = AtmosTypography.bodySmall.bigger(),
        labelLarge = AtmosTypography.labelLarge.bigger(),
        labelMedium = AtmosTypography.labelMedium.bigger(),
        labelSmall = AtmosTypography.labelSmall.bigger()
    )
}
