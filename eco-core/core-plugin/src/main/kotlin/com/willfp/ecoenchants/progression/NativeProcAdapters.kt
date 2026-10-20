package com.willfp.ecoenchants.progression

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/** Small native boundaries used by the live listener, directly testable without a Paper lifecycle. */
internal object NativeProcAdapters {
    data class FrozenItem(val id: UUID,val item: ItemStack,val stamped: Boolean)
    fun capture(item: ItemStack,key: NamespacedKey): FrozenItem? {
        if(item.amount!=1)return null
        val copy=item.clone();val meta=copy.itemMeta ?: return null
        val old=meta.persistentDataContainer.get(key,PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val id=old ?: UUID.randomUUID()
        if(old==null){meta.persistentDataContainer.set(key,PersistentDataType.STRING,id.toString());copy.itemMeta=meta}
        return FrozenItem(id,copy,old==null)
    }
    fun confirmed(event: EntityDamageByEntityEvent,victim: LivingEntity,before: Double): Double =
        ProcGuards.confirmedDamage(event.isCancelled,victim.lastDamageCause===event,event.finalDamage,before,victim.health)
    fun slow(attribute: AttributeInstance,key: NamespacedKey,fraction: Double): AttributeModifier {
        require(fraction.isFinite()&&fraction>0&&fraction<=0.20)
        attribute.modifiers.filter { it.key==key }.forEach { attribute.removeModifier(it) }
        return AttributeModifier(key,-fraction,AttributeModifier.Operation.MULTIPLY_SCALAR_1).also { attribute.addTransientModifier(it) }
    }
}
