package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.triggers.*
import com.willfp.libreforge.triggers.event.EditableXpDropEvent
import com.willfp.libreforge.triggers.event.XpDropCause
import org.bukkit.entity.Player

internal object EffectDoubleMobXp : Effect<NoCompileData>("double_mob_xp") {
 override val supportsDelay=false
 override val parameters=setOf(TriggerParameter.EVENT,TriggerParameter.PLAYER)
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean =
  doubleMobXp(data.event as? EditableXpDropEvent,data.player)
}
/** Mutates the native orb/drop amount only; never awards skill XP or money. */
internal fun doubleMobXp(event:EditableXpDropEvent?,player:Player?):Boolean {
 if(event==null||player==null||event.cause!=XpDropCause.ENTITY||event.entity is Player||
  event.player?.uniqueId!=player.uniqueId||event.isCancelled()||event.xp<=0||event.originalXp<=0)return false
 if(!event.claimAdjustment("ecoenchants:experience"))return false
 event.xp=(event.xp.toLong()*2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
 return true
}
internal object NativeUtilityProgression {
 fun register(){Effects.register(EffectDoubleMobXp)}
}
