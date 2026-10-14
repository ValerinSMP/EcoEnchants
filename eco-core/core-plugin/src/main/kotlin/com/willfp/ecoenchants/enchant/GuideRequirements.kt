package com.willfp.ecoenchants.enchant

import com.willfp.ecoenchants.plugin
import org.bukkit.entity.Player
import java.util.UUID

/** Observational only. No condition evaluation, profile creation, SQL or scheduled task. */
internal object GuideRequirements {
    data class Gate(val level: Int? = null, val disabled: Boolean = false, val unsupported: Boolean = false)
    private val gates = mutableMapOf<EcoEnchant, Gate>()
    fun clear() = gates.clear()

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

    fun description(enchant: EcoEnchant, lines: List<String>): List<String> {
        val gate = gate(enchant)
        if (gate.level == null || gate.unsupported) return lines
        // Replace only the known manually duplicated Encantador line in this guide; item lore stays native.
        return lines.filterNot { Regex("^req:.*encantador.*nv[0-9]+.*$").matches(GuideQuery.normalize(it)) }
    }

    fun lore(enchant: EcoEnchant, current: Int?): List<String> {
        val gate = gate(enchant)
        val state = status(gate, current)
        val path = "enchant-guide.requirements"
        val lines = mutableListOf("")
        if (gate.level != null && !gate.disabled && !gate.unsupported) {
            lines += plugin.configYml.getFormattedStrings("$path.progress")
        }
        lines += plugin.configYml.getFormattedStrings("$path.$state")
        return lines.map {
            it.replace("%required_level%", gate.level?.toString() ?: "")
                .replace("%current_level%", current?.toString() ?: plugin.configYml.getFormattedString("$path.unknown-level"))
                .replace("%missing_levels%", if (current != null && gate.level != null) (gate.level - current).coerceAtLeast(0).toString() else "")
        }
    }
}
