package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.persistence.PersistentDataType

/** Prepared once with the enchant. Intrinsic mechanic filters remain additional restrictions. */
class CombatTargetPolicy private constructor(
    private val pvp: Boolean, private val pve: Boolean,
    private val included: Set<EntityType>?, private val excluded: Set<EntityType>,
    private val valid: Boolean
) {
    fun selects(type: EntityType, player: Boolean): Boolean = valid && (if (player) pvp else pve) &&
        (included == null || type in included) && type !in excluded

    fun allows(actor: Player, target: LivingEntity, allowDead: Boolean = false, defensive: Boolean = false, posthumous: Boolean = false): Boolean {
        if (!Bukkit.isPrimaryThread()) return false
        if (actor.uniqueId == target.uniqueId || !selects(target.type, target is Player) ||
            !actor.isOnline || (actor.isDead && !posthumous) || Bukkit.getPlayer(actor.uniqueId) !== actor ||
            actor.world != target.world || target.isInvulnerable || target is ArmorStand ||
            target.type.name == "MANNEQUIN" || actor.hasMetadata("NPC") || target.hasMetadata("NPC") ||
            (!allowDead && (!target.isValid || target.isDead))) return false
        if (target is Tameable && (target.isTamed || target.owner != null)) return false
        // Verified vPets ownership key, not arbitrary PDC or tags.
        if (target.persistentDataContainer.has(PET_OWNER, PersistentDataType.STRING)) return false
        if (target is Player && (!target.isOnline || target.gameMode == GameMode.CREATIVE ||
                target.gameMode == GameMode.SPECTATOR || !target.world.pvp)) return false
        if (sequenceOf(actor.scoreboard, Bukkit.getScoreboardManager()?.mainScoreboard).filterNotNull().any { board ->
                val team = board.getEntryTeam(actor.name)
                team != null && team == board.getEntryTeam(if (target is Player) target.name else target.uniqueId.toString())
            }) return false
        return if (defensive && target is Player) AntigriefManager.canInjure(target, actor)
            else AntigriefManager.canInjure(actor, target)
    }

    companion object {
        private val PET_OWNER = NamespacedKey("vpets", "pet-owner")
        fun compile(root: Config, diagnostic: (String) -> Unit): CombatTargetPolicy {
            var valid = true
            fun reject(message: String) { valid = false; diagnostic("combat-targets: $message; combat effects disabled for this enchant") }
            val mode = root.getStringOrNull("combat-mode")?.trim()?.lowercase()
            if (root.has("combat-mode") && mode == null) reject("combat-mode must be a string")
            if (mode != null && mode !in setOf("both", "pvp", "pve")) reject("invalid combat-mode")
            val section = root.getSubsectionOrNull("combat-targets")
            if (root.has("combat-targets") && section == null) reject("expected a section")
            val keys = setOf("pvp", "pve", "include-types", "exclude-types")
            section?.getKeys(false)?.filterNot { it in keys }?.forEach { reject("unknown key $it") }
            fun flag(key: String): Boolean {
                if (section == null || !section.has(key)) return true
                return section.getBoolOrNull(key) ?: run { reject("$key must be a boolean"); false }
            }
            val pvp = flag("pvp") && mode != "pve"
            val pve = flag("pve") && mode != "pvp"
            if (mode == "pve" && section?.getBoolOrNull("pvp") == true) diagnostic("combat-targets: pvp=true is restricted by combat-mode=pve")
            if (mode == "pvp" && section?.getBoolOrNull("pve") == true) diagnostic("combat-targets: pve=true is restricted by combat-mode=pvp")
            if (!pvp && !pve) reject("combat-mode and pvp/pve leave no allowed target")
            fun types(key: String): Set<EntityType>? {
                if (section == null || !section.has(key)) return null
                val values = section.getStringsOrNull(key) ?: run { reject("$key must be a list"); return emptySet() }
                return values.mapNotNull { value ->
                    val type = runCatching { EntityType.valueOf(value.trim().uppercase().removePrefix("MINECRAFT:")) }.getOrNull()
                    if (type == null || !type.isAlive || type == EntityType.ARMOR_STAND || type.name == "MANNEQUIN") {
                        reject("invalid living entity type $value"); null
                    } else type
                }.toSet()
            }
            val include = types("include-types")
            val exclude = types("exclude-types") ?: emptySet()
            if (include != null && include.none { it !in exclude && (if (it == EntityType.PLAYER) pvp else pve) }) reject("include/exclude-types leave no allowed target")
            return CombatTargetPolicy(pvp, pve, include?.toSet(), exclude.toSet(), valid)
        }
    }
}
