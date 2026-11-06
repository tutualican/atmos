package com.alican.atmos.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alican.atmos.core.AppLanguage
import java.util.Locale

/**
 * Runtime translation layer.
 *
 * Reads a snapshot-backed language so every composable that renders text recomposes
 * when the user changes the interface language, without recreating the activity.
 * English source strings are the keys; a missing key falls back to English rather
 * than showing an empty label.
 */
object Loc {

    var language: AppLanguage by mutableStateOf(AppLanguage.SYSTEM)
        private set

    private val deviceLanguage: AppLanguage =
        AppLanguage.fromTag(Locale.getDefault().language) ?: AppLanguage.ENGLISH

    val active: AppLanguage
        get() = if (language == AppLanguage.SYSTEM) deviceLanguage else language

    /** Locale used for dates, times and upper/lower casing. */
    val javaLocale: Locale
        get() = if (language == AppLanguage.SYSTEM) Locale.getDefault()
        else Locale.forLanguageTag(language.tag)

    /** Language tag passed to the Open-Meteo geocoder so place names match the UI. */
    val apiLanguage: String
        get() = if (language == AppLanguage.SYSTEM) Locale.getDefault().language.ifBlank { "en" }
        else language.tag

    fun apply(value: AppLanguage) {
        language = value
    }

    private data class Template(val regex: Regex, val parts: List<String>)

    private var cachedFor: AppLanguage? = null
    private var all: Map<String, String> = emptyMap()
    private var exact: Map<String, String> = emptyMap()
    private var templates: List<Template> = emptyList()

    private fun ensure() {
        val lang = active
        if (lang == cachedFor) return
        val table = when (lang) {
            AppLanguage.TURKISH -> StringsTr.TABLE
            AppLanguage.GERMAN -> StringsDe.TABLE
            AppLanguage.SPANISH -> StringsEs.TABLE
            AppLanguage.FRENCH -> StringsFr.TABLE
            else -> emptyMap()
        }
        all = table.entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
        exact = table.entries.filterNot { it.key.contains("{}") }
            .associate { it.key.lowercase(Locale.ROOT) to it.value }
        templates = table.entries.filter { it.key.contains("{}") }
            .sortedByDescending { it.key.length }
            .map { (key, value) ->
                val keyParts = key.split("{}")
                val pattern = keyParts.joinToString("(.*?)") { Regex.escape(it) }
                Template(Regex("^$pattern$", RegexOption.IGNORE_CASE), value.split("{}"))
            }
        cachedFor = lang
    }

    /** Translate a whole, already-composed English string. */
    fun tr(text: String): String = trInternal(text, 0)

    private fun trInternal(text: String, depth: Int): String {
        if (text.isEmpty() || active == AppLanguage.ENGLISH) return text
        ensure()
        if (exact.isEmpty() && templates.isEmpty()) return text
        val isUpper = text.any { it.isLetter() } && text == text.uppercase(Locale.ROOT)
        exact[text.lowercase(Locale.ROOT)]?.let { hit ->
            return if (isUpper) hit.uppercase(javaLocale) else hit
        }
        for (template in templates) {
            val match = template.regex.matchEntire(text) ?: continue
            val sb = StringBuilder()
            template.parts.forEachIndexed { i, part ->
                sb.append(part)
                if (i + 1 < template.parts.size) {
                    val capture = match.groupValues.getOrElse(i + 1) { "" }
                    // Translate nested fragments such as "5m ago" inside "Updated 5m ago".
                    sb.append(if (depth < 3) trInternal(capture, depth + 1) else capture)
                }
            }
            val out = sb.toString()
            return if (isUpper) out.uppercase(javaLocale) else out
        }
        return text
    }

    /**
     * Translate a template and fill `{}` placeholders in order.
     * The same templates power [tr], so dynamic strings stay localized in Compose
     * and in widgets without duplicating format logic.
     */
    fun t(template: String, vararg args: Any?): String {
        ensure()
        var out = all[template.lowercase(Locale.ROOT)] ?: template
        args.forEach { arg ->
            out = out.replaceFirst("{}", arg?.toString() ?: "")
        }
        return out
    }
}
