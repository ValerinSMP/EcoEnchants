package com.willfp.ecoenchants.enchant

import com.willfp.ecoenchants.plugin
import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.EffectList
import java.util.Objects

class EcoEnchantLevel(
    val enchant: EcoEnchant,
    val level: Int,
    override val effects: EffectList,
    override val conditions: ConditionList
) : Holder, com.willfp.libreforge.CombatTargetHolder {
    override fun combatTargetSession(actor: org.bukkit.entity.Player): Any? =
        com.willfp.ecoenchants.progression.OriginalRuntime.observedSession(actor)

    override fun allowsCombatTarget(actor: org.bukkit.entity.Player, target: org.bukkit.entity.LivingEntity, allowDead: Boolean, defensive: Boolean): Boolean =
        EcoEnchants.getByID(enchant.id)?.getLevel(level) === this && enchant.plugin.isEnabled &&
            (enchant as? com.willfp.ecoenchants.enchant.impl.EcoEnchantBase)?.combatTargetPolicy?.allows(actor, target, allowDead, defensive) == true

    override fun allowsPosthumousCombatTarget(actor: org.bukkit.entity.Player, target: org.bukkit.entity.LivingEntity): Boolean =
        EcoEnchants.getByID(enchant.id)?.getLevel(level) === this && enchant.plugin.isEnabled &&
            (enchant as? com.willfp.ecoenchants.enchant.impl.EcoEnchantBase)?.combatTargetPolicy?.allows(actor, target, posthumous = true) == true

    override val id = plugin.createNamespacedKey("${enchant.id}_$level")

    override fun equals(other: Any?): Boolean {
        if (other !is EcoEnchantLevel) {
            return false
        }

        return this.id == other.id
    }

    override fun toString(): String {
        return id.toString()
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }
}
