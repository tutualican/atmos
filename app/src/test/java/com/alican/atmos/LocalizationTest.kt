package com.alican.atmos

import com.alican.atmos.core.AppLanguage
import com.alican.atmos.i18n.Loc
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LocalizationTest {

    @Before
    fun setUp() {
        Loc.apply(AppLanguage.ENGLISH)
    }

    @After
    fun tearDown() {
        Loc.apply(AppLanguage.ENGLISH)
    }

    @Test
    fun englishPassesThroughUnchanged() {
        assertEquals("Partly cloudy", Loc.tr("Partly cloudy"))
        assertEquals("Humidity 64%", Loc.t("Humidity {}%", 64))
    }

    @Test
    fun turkishTranslatesExactStrings() {
        Loc.apply(AppLanguage.TURKISH)
        assertEquals("Parçalı bulutlu", Loc.tr("Partly cloudy"))
        assertEquals("Gök gürültülü fırtına", Loc.tr("Thunderstorm"))
        assertEquals("Ana sayfa", Loc.tr("Home"))
    }

    @Test
    fun turkishTranslatesTemplates() {
        Loc.apply(AppLanguage.TURKISH)
        assertEquals("Nem %64", Loc.tr("Humidity 64%"))
        assertEquals("Hissedilen 12°C", Loc.tr("Feels like 12°C"))
        assertEquals("Model analizi · güncellendi 5 dk önce", Loc.tr("Model analysis · updated 5m ago"))
        assertEquals("Nem %64", Loc.t("Humidity {}%", 64))
    }

    @Test
    fun uppercaseSectionHeadersAreLocalized() {
        Loc.apply(AppLanguage.TURKISH)
        assertEquals("UV İNDEKSİ", Loc.tr("UV INDEX"))
        assertEquals("ŞU ANDA", Loc.tr("RIGHT NOW"))
    }

    @Test
    fun germanSpanishFrenchHaveCoreLabels() {
        Loc.apply(AppLanguage.GERMAN)
        assertEquals("Vorhersage", Loc.tr("Forecast"))
        assertEquals("Luftfeuchtigkeit 64 %", Loc.tr("Humidity 64%"))

        Loc.apply(AppLanguage.SPANISH)
        assertEquals("Pronóstico", Loc.tr("Forecast"))
        assertEquals("Humedad 64 %", Loc.tr("Humidity 64%"))

        Loc.apply(AppLanguage.FRENCH)
        assertEquals("Prévisions", Loc.tr("Forecast"))
        assertEquals("Humidité 64 %", Loc.tr("Humidity 64%"))
    }

    @Test
    fun everyLanguageCoversPrimarySurfaces() {
        val keys = listOf(
            "Home", "Map", "Forecast", "More", "Right now", "Next 24 hours",
            "7-day forecast", "Air quality", "Temperature", "Precipitation",
            "Weather map", "Language", "System default", "Sunrise", "Sunset",
            "Clear sky", "Thunderstorm", "Good", "Moderate",
            "Rain chance", "Full forecast", "Larger text"
        )
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM && it != AppLanguage.ENGLISH }.forEach { language ->
            Loc.apply(language)
            keys.forEach { key ->
                val translated = Loc.tr(key)
                assertEquals("$language is missing a translation for '$key'", false, translated == key)
            }
        }
    }

    @Test
    fun everyLanguageHasSameKeyCount() {
        // Guards against a partially copied table.
        val counts = AppLanguage.entries
            .filter { it != AppLanguage.SYSTEM && it != AppLanguage.ENGLISH }
            .associateWith { language ->
                Loc.apply(language)
                listOf(
                    "Home", "Map", "Forecast", "More", "Right now", "Sunrise", "Sunset",
                    "Humidity 1%", "Feels like 1°C", "Updated 1m ago", "UV index",
                    "Air quality", "Wind", "Pressure", "Visibility", "Language"
                ).count { Loc.tr(it) != it }
            }
        // "Wind" is identical in German and English, so 15/16 is valid.
        assertEquals(true, counts.values.all { it >= 15 })
    }

    @Test
    fun readabilityAndRainLabelsAreLocalized() {
        Loc.apply(AppLanguage.TURKISH)
        assertEquals("Yağış %40", Loc.t("Rain {}%", 40))
        assertEquals("Yağış olasılığı", Loc.tr("Rain chance"))
        assertEquals("Daha büyük yazı", Loc.tr("Larger text"))
        assertEquals("Tüm tahmin", Loc.tr("Full forecast"))

        Loc.apply(AppLanguage.GERMAN)
        assertEquals("Regen 40 %", Loc.t("Rain {}%", 40))
        assertEquals("Größere Schrift", Loc.tr("Larger text"))

        Loc.apply(AppLanguage.SPANISH)
        assertEquals("Lluvia 40 %", Loc.t("Rain {}%", 40))
        assertEquals("Texto más grande", Loc.tr("Larger text"))

        Loc.apply(AppLanguage.FRENCH)
        assertEquals("Pluie 40 %", Loc.t("Rain {}%", 40))
        assertEquals("Texte plus grand", Loc.tr("Larger text"))
    }

    @Test
    fun nowcastAndOfficialAlertCopyIsLocalized() {
        val keys = listOf(
            "Rain nowcast alerts", "Rain is falling now",
            "Official warning", "Issued by authority", "What to do", "Open official notice",
            "Appearance", "System accent (Material You)", "Five widget families"
        )
        AppLanguage.entries.filter { it != AppLanguage.SYSTEM && it != AppLanguage.ENGLISH }.forEach { language ->
            Loc.apply(language)
            keys.forEach { key ->
                assertEquals("$language is missing '$key'", false, Loc.tr(key) == key)
            }
        }
        Loc.apply(AppLanguage.TURKISH)
        assertEquals("Yaklaşık 20 dk içinde yağış olası", Loc.t("Rain likely in about {} min", 20))
        assertEquals("Resmî uyarı", Loc.tr("Official warning"))
    }
}
