package com.alican.atmos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.alican.atmos.core.WeatherKind
import java.time.LocalTime

/**
 * One atmosphere drives the whole product: the animated scene, the page background,
 * card surfaces and the cool accent. It always follows the real conditions and the
 * local time of the place on screen — never a random theme.
 */
data class AtmosAtmosphere(
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceLift: Color,
    val accent: Color,
    val warm: Color,
    /** True when the top of the sky is light enough that a header scrim is required. */
    val brightSky: Boolean
)

fun defaultAtmosphere(): AtmosAtmosphere =
    atmosphereFor(WeatherKind.PARTLY_CLOUDY, true, LocalTime.now().hour)

/**
 * Daypart first, weather second. Morning leans toward a soft baby blue, evening stays a
 * deep navy, night is near-black blue, and storms pull the whole sky toward violet slate.
 */
fun atmosphereFor(kind: WeatherKind, isDay: Boolean, hour: Int): AtmosAtmosphere {
    val dawn = hour in 5..7
    val dusk = hour in 18..21
    val night = !isDay || hour >= 22 || hour <= 4

    var top: Color
    var mid: Color
    var bottom: Color

    when {
        night -> {
            top = Color(0xFF101C2E)
            mid = Color(0xFF0A1220)
            bottom = Color(0xFF070B12)
        }
        dawn -> {
            top = Color(0xFF93BBD8)
            mid = Color(0xFF5B81A6)
            bottom = Color(0xFF1C2C3D)
        }
        dusk -> {
            top = Color(0xFF2F476B)
            mid = Color(0xFF1C2C47)
            bottom = Color(0xFF0E1622)
        }
        else -> {
            top = Color(0xFF6E9CC2)
            mid = Color(0xFF3F6388)
            bottom = Color(0xFF182636)
        }
    }

    when (kind) {
        WeatherKind.THUNDER, WeatherKind.HAIL -> {
            if (night) {
                top = Color(0xFF1D1833); mid = Color(0xFF131021); bottom = Color(0xFF08070E)
            } else if (dusk || dawn) {
                top = Color(0xFF4A416D); mid = Color(0xFF2A2544); bottom = Color(0xFF100E1A)
            } else {
                top = Color(0xFF3D3760); mid = Color(0xFF252140); bottom = Color(0xFF0E0D17)
            }
        }
        WeatherKind.RAIN, WeatherKind.HEAVY_RAIN, WeatherKind.DRIZZLE,
        WeatherKind.FREEZING_RAIN, WeatherKind.SLEET -> {
            if (night) {
                top = Color(0xFF14202D); mid = Color(0xFF0D1620); bottom = Color(0xFF080D14)
            } else if (dusk || dawn) {
                top = Color(0xFF45617A); mid = Color(0xFF294357); bottom = Color(0xFF101D29)
            } else {
                top = Color(0xFF456073); mid = Color(0xFF274155); bottom = Color(0xFF0F1B26)
            }
        }
        WeatherKind.SNOW -> {
            if (night) {
                top = Color(0xFF1B2836); mid = Color(0xFF121C27); bottom = Color(0xFF090F17)
            } else if (dusk || dawn) {
                top = Color(0xFFA3BFD1); mid = Color(0xFF64839A); bottom = Color(0xFF22303D)
            } else {
                top = Color(0xFF95B1C4); mid = Color(0xFF58758A); bottom = Color(0xFF1C2A36)
            }
        }
        WeatherKind.FOG -> {
            if (night) {
                top = Color(0xFF1A2430); mid = Color(0xFF111923); bottom = Color(0xFF090E15)
            } else if (dusk || dawn) {
                top = Color(0xFF8294A5); mid = Color(0xFF536271); bottom = Color(0xFF1B2430)
            } else {
                top = Color(0xFF758595); mid = Color(0xFF4A5968); bottom = Color(0xFF18212B)
            }
        }
        WeatherKind.OVERCAST -> {
            if (night) {
                top = Color(0xFF17202E); mid = Color(0xFF0F1722); bottom = Color(0xFF080D14)
            } else if (dusk || dawn) {
                top = Color(0xFF61758B); mid = Color(0xFF3B5066); bottom = Color(0xFF18222E)
            } else {
                top = Color(0xFF5C7085); mid = Color(0xFF384C61); bottom = Color(0xFF16202B)
            }
        }
        WeatherKind.PARTLY_CLOUDY -> {
            if (!night) {
                top = blend(top, Color(0xFFB4C6D6), 0.10f)
            }
        }
        WeatherKind.MOSTLY_CLEAR -> {
            if (!night) {
                top = blend(top, Color(0xFFC9DAE6), 0.14f)
            }
        }
        WeatherKind.CLEAR -> Unit
    }

    val background = when (kind) {
        WeatherKind.THUNDER, WeatherKind.HAIL -> Color(0xFF0F0D18)
        WeatherKind.RAIN, WeatherKind.HEAVY_RAIN, WeatherKind.DRIZZLE,
        WeatherKind.FREEZING_RAIN, WeatherKind.SLEET -> Color(0xFF0D1721)
        WeatherKind.SNOW -> Color(0xFF101923)
        WeatherKind.FOG -> Color(0xFF111A23)
        WeatherKind.OVERCAST -> Color(0xFF111924)
        else -> when {
            night -> Color(0xFF080D15)
            dawn -> Color(0xFF14202D)
            dusk -> Color(0xFF0D1420)
            else -> Color(0xFF101B28)
        }
    }
    val backgroundAlt = blend(background, mid, 0.16f)
    val tinted = blend(background, mid, 0.10f)
    val surface = blend(tinted, Color.White, 0.070f)
    val surfaceAlt = blend(tinted, Color.White, 0.105f)
    val surfaceLift = blend(tinted, Color.White, 0.145f)

    val accent = when (kind) {
        WeatherKind.THUNDER, WeatherKind.HAIL -> Color(0xFFA9B8FF)
        WeatherKind.SNOW -> Color(0xFFD3E6F4)
        WeatherKind.FOG -> Color(0xFFB9C6D4)
        WeatherKind.RAIN, WeatherKind.HEAVY_RAIN, WeatherKind.DRIZZLE,
        WeatherKind.FREEZING_RAIN, WeatherKind.SLEET -> Color(0xFF9CC2E0)
        else -> blend(Color(0xFF9CC2E0), top, 0.28f)
    }
    val warm = if (dawn || dusk) Color(0xFFF0C99A) else Color(0xFFE3BE7C)

    return AtmosAtmosphere(
        skyTop = top,
        skyMid = mid,
        skyBottom = bottom,
        background = background,
        backgroundAlt = backgroundAlt,
        surface = surface,
        surfaceAlt = surfaceAlt,
        surfaceLift = surfaceLift,
        accent = accent,
        warm = warm,
        brightSky = top.luminance() > 0.42f
    )
}

internal fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
