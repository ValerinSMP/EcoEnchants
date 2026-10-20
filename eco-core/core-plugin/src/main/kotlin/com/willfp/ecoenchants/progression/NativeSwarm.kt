package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.entities.BoundedLivingQueries
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.libreforge.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.effects.impl.AdditiveDamageGroups
import com.willfp.libreforge.effects.impl.DamageMultiplierBypass
import com.willfp.libreforge.triggers.*
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID

internal object EffectSwarmMultiplier : Effect<NoCompileData>("swarm_multiplier") {
 override val supportsDelay=false
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.EVENT)
 override val arguments=arguments{require("percent_per_monster","Specify the percentage per monster.",description="Additive bonus per eligible nearby monster, up to ten.",type=ArgType.EXPRESSION)}
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean = NativeSwarm.offer(data,config.getDoubleFromExpression("percent_per_monster",data))
}
internal class SwarmQueryBudget {
 private var tick=Int.MIN_VALUE;private var used=0
 fun take(current:Int,maximum:Int):Boolean{if(tick!=current){tick=current;used=0};if(used>=maximum)return false;used++;return true}
 fun reset(){tick=Int.MIN_VALUE;used=0}
}
internal object NativeSwarm {
 private lateinit var owner:EcoEnchantsPlugin
 private var capacity:()->Int={512}
 private data class Witnesses(val token:UUID,val world:UUID,val entities:List<LivingEntity>)
 private val cache=mutableMapOf<UUID,Witnesses>()
 private val budget=SwarmQueryBudget()
 fun register(){Effects.register(EffectSwarmMultiplier)}
 fun enable(plugin:EcoEnchantsPlugin,limit:()->Int){owner=plugin;capacity=limit}
 fun clear(){cache.clear();budget.reset()}
 fun retire(uuid:UUID){cache.remove(uuid)}
 private fun eligible(player:Player,origin:org.bukkit.Location,entity:LivingEntity)=entity.world==origin.world&&entity.location.distanceSquared(origin)<=100.0&&OriginalRuntime.eligible(player,entity)
 private fun count(player:Player):Int? {
  val token=OriginalRuntime.session(player)?:return null
  val origin=player.location
  val old=cache[player.uniqueId]
  // Revalidate all ten current witnesses: a full capped count is exact without another world query.
  if(old!=null&&old.token==token&&old.world==player.world.uid&&old.entities.size==10&&old.entities.all{eligible(player,origin,it)})return 10
  cache.remove(player.uniqueId)
  val allowed=(owner.configYml.getIntOrNull("native-swarm.queries-per-tick")?:8).coerceIn(0,32)
  if(!budget.take(Bukkit.getCurrentTick(),allowed))return null
  val maximum=(owner.configYml.getIntOrNull("native-swarm.max-candidates")?:128).coerceIn(16,256)
  val result=BoundedLivingQueries.nearby(origin,10.0,maximum)
  if(result.availability()!=BoundedLivingQueries.Availability.AVAILABLE)return null
  val found=result.candidates().asSequence().filter{eligible(player,origin,it)}.take(10).toList()
  if(found.size==10){if(cache.size<capacity()||player.uniqueId in cache)cache[player.uniqueId]=Witnesses(token,player.world.uid,found);return 10}
  // A truncated sample cannot prove there are zero/five/etc: absence stays unknown.
  return if(result.truncated())null else found.size
 }

 fun offer(data:TriggerData,percent:Double):Boolean {
  val player=data.player?:return false;val victim=data.victim?:return false
  val event=data.event as? EntityDamageByEntityEvent?:return false
  val level=data.holder.holder as? EcoEnchantLevel?:return false
  if(level.enchant.id!="swarm"||level.enchant.type.id!="normal"||level.level !in 1..5||
   event.damager!==player||event.entity!==victim||event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK||
   event.isCancelled||!player.isOnline||player.isDead||!NativeCombatProgression.gameplay()||DamageMultiplierBypass.current!=null||!percent.isFinite()||percent<0)return false
  val held=player.inventory.itemInMainHand
  if(held.getEnchantmentLevel(level.enchant.enchantment)!=level.level||!level.enchant.targets.any{it.matches(held)})return false
  val nearby=count(player)?:return false
  return AdditiveDamageGroups.offer("ecoenchants_normal",data,1+percent*nearby/100)
 }
}
