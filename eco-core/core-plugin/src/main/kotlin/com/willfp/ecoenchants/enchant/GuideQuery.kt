package com.willfp.ecoenchants.enchant

import java.text.Normalizer
import java.util.Locale

/** Literal search only: user text is never interpreted as formatting or a regular expression. */
internal object GuideQuery {
    private val colorCodes = Regex("(?i)[§&][0-9a-fk-orx]")
    private val tags = Regex("<[^>]*>")
    private val accents = Regex("\\p{M}+")

    fun normalize(value: String): String = accents.replace(
        Normalizer.normalize(tags.replace(colorCodes.replace(value, ""), ""), Normalizer.Form.NFD), ""
    ).lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")

    fun input(value: String): String = value.filterNot { it.isISOControl() }.trim().take(64)
    fun matches(name: String, query: String): Boolean = normalize(name).contains(normalize(query))
    fun next(current: Int, delta: Int, count: Int): Int = Math.floorMod(current + delta, count.coerceAtLeast(1))
    fun level(current: Int, delta: Int, maximum: Int): Int =
        (current.toLong() + delta).coerceIn(1L, maximum.coerceAtLeast(1).toLong()).toInt()
    fun matchesTargets(actual: Set<String>, accepted: Set<String>): Boolean =
        accepted.isEmpty() || "all" in actual || actual.any { it in accepted }
}

internal data class GuideState(val query: String = "", val filter: Int = 0, val group: String? = null, val page: Int = 1)
