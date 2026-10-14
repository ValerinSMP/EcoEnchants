package com.willfp.ecoenchants.enchant

import com.willfp.ecoenchants.plugin
import com.willfp.ecoenchants.display.getFormattedName
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.formatEco
import org.bukkit.entity.Player
import java.util.UUID

/** Observational only. No condition evaluation, profile creation, SQL or scheduled task. */
internal object GuideRequirements {
    data class Gate(val level: Int? = null, val disabled: Boolean = false, val unsupported: Boolean = false)
    private val gates = mutableMapOf<EcoEnchant, Gate>()
    private val descriptions = mutableMapOf<Pair<EcoEnchant, Int>, List<String>>()
    fun clear() {
        gates.clear()
        descriptions.clear()
    }

    fun parse(levels: List<String>, disabled: Boolean): Gate {
        if (levels.isEmpty()) return Gate()
        val parsed = levels.map { it.trim().toIntOrNull()?.takeIf { value -> value >= 0 } }
        if (parsed.any { it == null }) return Gate(unsupported = true)
        val required = parsed.filterNotNull().maxOrNull()
        return Gate(required, disabled && required == 999)
    }

    private fun gate(enchant: EcoEnchant): Gate = gates.getOrPut(enchant) {
        val conditions = enchant.config.getSubsections("conditions").filter {
            it.getString("id") == "has_skill_level" && it.getString("args.skill").equals("enchanting", true)
        }
        parse(conditions.map { it.getString("args.level") }, conditions.any {
            it.getStrings("args.not-met-lines").any { line -> GuideQuery.normalize(line).contains("temporalmente desactivado") }
        })
    }

    fun snapshotLevel(snapshot: Map<*, *>?, uuid: UUID): Int? {
        if (snapshot?.get("schemaVersion") != 1 || snapshot["playerUuid"] != uuid.toString() ||
            snapshot["status"] !in setOf("available", "partial")) return null
        val row = (snapshot["skills"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.singleOrNull { it["id"] == "enchanting" } ?: return null
        val level = row["level"] as? Number ?: return null
        val value = level.toDouble()
        return if (value.isFinite() && value >= 0 && value <= Int.MAX_VALUE && value == value.toInt().toDouble()) value.toInt() else null
    }

    /** Resolve optional public API against the enabled plugin, so absence/reload cannot retain a stale loader. */
    fun level(player: Player): Int? {
        val skills = plugin.server.pluginManager.getPlugin("EcoSkills")?.takeIf { it.isEnabled } ?: return null
        return try {
            val api = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI", false, skills.javaClass.classLoader)
            snapshotLevel(api.getMethod("getCachedSnapshot", UUID::class.java).invoke(null, player.uniqueId) as? Map<*, *>, player.uniqueId)
        } catch (_: ReflectiveOperationException) { null } catch (_: LinkageError) { null }
    }

    fun status(gate: Gate, current: Int?): String = when {
        gate.disabled -> "disabled"
        gate.unsupported -> "unsupported"
        gate.level == null -> "none"
        current == null -> "unavailable"
        current >= gate.level -> "met"
        else -> "missing"
    }

    private val manualRequirement = Regex("^req(?:uisitos?)?\\s*:.*encantador.*nv\\s*[0-9]+.*$")

    fun stripManualRequirement(lines: List<String>, supported: Boolean): List<String> =
        if (!supported) lines else lines.filterNot { manualRequirement.matches(GuideQuery.normalize(it)) }

    fun description(enchant: EcoEnchant, lines: List<String>): List<String> {
        val gate = gate(enchant)
        return stripManualRequirement(lines, gate.level != null && !gate.unsupported)
    }

    /** Filter before native wrapping. Mirror the native description cache, never cache player status here. */
    fun formattedDescription(enchant: EcoEnchant, level: Int, player: Player): List<String> =
        descriptions.getOrPut(enchant to level) {
            val format = plugin.configYml.getString("display.descriptions.format")
            val wrap = plugin.configYml.getInt("display.descriptions.word-wrap")
            description(enchant, enchant.getRawDescription(level, player)).flatMap { line ->
                var formatted = format + line
                for (tag in arrayOf("<reset>", "&r", "§r")) formatted = formatted.replace(tag, tag + format)
                StringUtils.lineWrap(formatted.formatEco(placeholderContext(injectable = enchant.config)), wrap)
            }
        }

    /** Empty blocks disappear; unknown cache levels never imply the player meets a requirement. */
    fun lines(gate: Gate, current: Int?, previous: String?, text: (String) -> List<String>): List<String> {
        val content = mutableListOf<String>()
        when (status(gate, current)) {
            "missing" -> content += text("missing")
            "unavailable" -> content += text("unavailable")
            "unsupported" -> content += text("unsupported")
            "disabled" -> content += text("disabled")
        }
        if (!previous.isNullOrBlank()) content += text("previous").map { it.replace("%previous%", previous) }
        if (content.isEmpty()) return emptyList()
        return listOf("") + text("heading") + content.map {
            it.replace("%required_level%", gate.level?.toString() ?: "")
                .replace("%current_level%", current?.toString() ?: "")
        }
    }

    fun lore(enchant: EcoEnchant, current: Int?): List<String> = lines(
        gate(enchant), current, enchant.required.takeIf { it.isNotEmpty() }?.joinToString(", ") {
            it.wrap().getFormattedName(0)
        }
    ) { plugin.configYml.getFormattedStrings("enchant-guide.requirements.$it") }
}
