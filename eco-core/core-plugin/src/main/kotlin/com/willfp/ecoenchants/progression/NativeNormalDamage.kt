package com.willfp.ecoenchants.progression

import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.libreforge.effects.impl.AdditiveDamageGroups
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent

/** Holder ownership is verified here, not guessed by LibreForge from a YAML label. */
internal object NativeNormalDamage {
 private const val GROUP="ecoenchants_normal"
 fun enable(owner:EcoEnchantsPlugin){
  AdditiveDamageGroups.bind(GROUP,owner){data->
   val level=data.holder.holder as? EcoEnchantLevel
   val event=data.event as? EntityDamageByEntityEvent
   val player=data.player
   level!=null&&level.enchant.type.id=="normal"&&event!=null&&player!=null&&player.isOnline&&!player.isDead&&player.world==event.entity.world&&data.victim===event.entity&&
    NativeCombatRules.physical(event.cause)&&NativeCombatProgression.gameplay()&&
    (event.damager===player||(event.damager as? Projectile)?.shooter===player)
  }
 }
 fun close(owner:EcoEnchantsPlugin){AdditiveDamageGroups.unbind(GROUP,owner)}
}
