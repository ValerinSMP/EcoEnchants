package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.libreforge.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.effects.impl.AdditiveDamageGroups
import com.willfp.libreforge.effects.impl.DamageMultiplierBypass
import com.willfp.libreforge.triggers.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID

internal object EffectFirstHitsMultiplier : Effect<NoCompileData>("first_hits_multiplier") {
 override val supportsDelay=false
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.EVENT)
 override val arguments=arguments{
  require("multiplier","Specify the damage multiplier.",description="Additive native first-hit multiplier.",type=ArgType.EXPRESSION)
  optional("hits",description="Approved budget: first_strike=1, triple_strike=3 (legacy default).",type=ArgType.INT)
 }
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean = NativeFirstHits.offer(data,config.getDoubleFromExpression("multiplier",data),config.getIntOrNull("hits")?:3)
}
internal object EffectThirdHitMultiplier : Effect<NoCompileData>("third_hit_multiplier") {
 override val supportsDelay=false
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.EVENT)
 override val arguments=arguments{require("multiplier","Specify the third-hit multiplier.",description="Additive bonus on the third hit.",type=ArgType.EXPRESSION)}
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData)=NativeFirstHits.offerThird(data,config.getDoubleFromExpression("multiplier",data))
}
internal object EffectRecentKillsMultiplier : Effect<NoCompileData>("recent_kills_multiplier") {
 override val supportsDelay=false
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.EVENT)
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData)=NativeBurstProgression.offerCombo(data)
}
/** Uses the existing OriginalRuntime damage ticket, confirmation and session; no extra damage task. */
internal object NativeFirstHits {
 private val ledger=FirstHitsLedger()
 private val thirds=FirstHitsLedger(cyclic=true)
 private data class ThirdLease(val actor:UUID,val target:UUID,val lease:OriginalProcEngine.Lease)
 private val leases=mutableMapOf<UUID,ThirdLease>()
 fun register(){Effects.register(EffectFirstHitsMultiplier);Effects.register(EffectThirdHitMultiplier);Effects.register(EffectRecentKillsMultiplier)}
 fun clear(){ledger.clear();thirds.clear();leases.clear()}
 fun retire(uuid:UUID){ledger.retire(uuid);thirds.retire(uuid);leases.entries.removeIf{it.value.actor==uuid||it.value.target==uuid}}
 fun finish(attempt:UUID,damage:Double,event:EntityDamageEvent){
  leases.remove(attempt)?.let{if(explicitlyRejectedDamage(event))OriginalRuntime.rollbackNative(OriginalProcEngine.Family.THUNDERLORD,it.actor,it.lease)}
  val resolution=when{
   explicitlyRejectedDamage(event)->FirstHitResolution.REJECTED
   damage>0->FirstHitResolution.CONFIRMED
   else->FirstHitResolution.UNCERTAIN
  }
  ledger.resolve(attempt,resolution);thirds.resolve(attempt,resolution)
 }
 fun offerThird(data:TriggerData,multiplier:Double):Boolean {
  val player=data.player?:return false;val victim=data.victim?:return false
  val event=data.event as? EntityDamageByEntityEvent?:return false
  val level=data.holder.holder as? EcoEnchantLevel?:return false
  if(level.enchant.id!="thunderlord"||level.level !in 1..5||event.damager!==player||event.entity!==victim||event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK||event.isCancelled||DamageMultiplierBypass.current!=null||!NativeCombatProgression.gameplay()||!multiplier.isFinite()||multiplier<1||!OriginalRuntime.modeAllows("thunderlord",player,victim))return false
  val held=player.inventory.itemInMainHand
  if(held.getEnchantmentLevel(level.enchant.enchantment)!=level.level||!level.enchant.targets.any{it.matches(held)})return false
  val attempt=OriginalRuntime.track(event)?:return false;val session=OriginalRuntime.session(player)?:return false
  if(!thirds.reserve(attempt,FirstHitsLedger.Key(player.uniqueId,session,player.world.uid,victim.uniqueId)))return false
  if(thirds.position(attempt)!=3)return true
  if(!ProcGuards.arrivalReady(OriginalProcEngine.Family.THUNDERLORD,OriginalRuntime.age(player)))return true
  val lease=OriginalRuntime.reserveNative(OriginalProcEngine.Family.THUNDERLORD,player.uniqueId,attempt).orElse(null)?:return true
  if(!AdditiveDamageGroups.offer("ecoenchants_normal",data,multiplier)){OriginalRuntime.rollbackNative(OriginalProcEngine.Family.THUNDERLORD,player.uniqueId,lease);thirds.resolve(attempt,FirstHitResolution.REJECTED);return false}
  leases[attempt]=ThirdLease(player.uniqueId,victim.uniqueId,lease)
  return true
 }
 fun offer(data:TriggerData,multiplier:Double):Boolean = offer(data,multiplier,3)
 fun offer(data:TriggerData,multiplier:Double,hits:Int):Boolean {
  val player=data.player?:return false;val victim=data.victim?:return false
  val event=data.event as? EntityDamageByEntityEvent?:return false
  val level=data.holder.holder as? EcoEnchantLevel?:return false
  if(!firstHitBudgetAllowed(level.enchant.id,hits)||level.enchant.type.id!="normal"||level.level !in 1..5||
   event.damager!==player||event.entity!==victim||victim !is LivingEntity||player===victim||
   event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK||event.isCancelled||!NativeCombatProgression.gameplay()||
   DamageMultiplierBypass.current!=null||!multiplier.isFinite()||multiplier<1||!OriginalRuntime.modeAllows(level.enchant.id,player,victim))return false
  val held=player.inventory.itemInMainHand
  if(held.getEnchantmentLevel(level.enchant.enchantment)!=level.level||!level.enchant.targets.any{it.matches(held)})return false
  val attempt=OriginalRuntime.track(event)?:return false
  val session=OriginalRuntime.session(player)?:return false
  val key=FirstHitsLedger.Key(player.uniqueId,session,player.world.uid,victim.uniqueId)
  if(!ledger.reserve(attempt,key,hits))return false
  val accepted=AdditiveDamageGroups.offer("ecoenchants_normal",data,multiplier)
  if(!accepted)ledger.resolve(attempt,FirstHitResolution.REJECTED)
  return accepted
 }
}

internal fun firstHitBudgetAllowed(id:String,hits:Int):Boolean =
 (id=="first_strike"&&hits==1)||(id=="triple_strike"&&hits==3)
