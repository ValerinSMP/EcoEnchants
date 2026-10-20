package com.willfp.ecoenchants.progression

import org.bukkit.event.entity.EntityDamageEvent

/** Parameters used by live native adapters, not a parallel damage model. */
internal object NativeCombatRules {
 fun physical(cause:EntityDamageEvent.DamageCause)=when(cause){EntityDamageEvent.DamageCause.ENTITY_ATTACK,EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,EntityDamageEvent.DamageCause.PROJECTILE->true;else->false}
 fun crossed(before:Double,after:Double,maximum:Double)=before.isFinite()&&after.isFinite()&&maximum.isFinite()&&maximum>0&&after>0&&before>=0.4*maximum&&after<0.4*maximum&&after<before
}
internal class ArmorStack(val tier:Int) {
 init{require(tier in 1..5)}
 var count=0;private set
 var until=0L;private set
 fun live(time:Long)=count>0&&time<until
 fun hit(time:Long){if(!live(time))count=0;count=(count+1).coerceAtMost(4);until=time+4_000}
 fun fraction()=tier*count/100.0
}

/** UUID cooldown outlives item and session retirement; session arrival separately fences restarts. */
internal class LastResortGate(private val limit:()->Int,private val clock:()->Long) {
 private val deadlines=mutableMapOf<java.util.UUID,Long>()
 fun prune(){val now=clock();deadlines.entries.removeIf{it.value<=now}}
 fun claim(uuid:java.util.UUID,sessionAge:Long):Boolean {
  val now=clock()
  if(sessionAge<30_000||(deadlines[uuid]?:Long.MIN_VALUE)>now)return false
  prune()
  if(uuid !in deadlines&&deadlines.size>=limit())return false
  deadlines[uuid]=now+30_000;return true
 }
}
