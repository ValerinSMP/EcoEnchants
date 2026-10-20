package com.willfp.ecoenchants.progression

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class NativeCombatProgressionTest {
 @Test fun `uuid cooldown survives duplicate new item and fresh session with bounded capacity`(){
  var now=100_000L;val gate=LastResortGate({1}){now};val a=java.util.UUID.randomUUID();val b=java.util.UUID.randomUUID()
  assertFalse(gate.claim(a,29_999));assertTrue(gate.claim(a,30_000));assertFalse(gate.claim(a,90_000));assertFalse(gate.claim(b,90_000))
  now+=29_999;assertFalse(gate.claim(a,30_000));now++;assertFalse(gate.claim(a,0));assertTrue(gate.claim(b,30_000));assertFalse(gate.claim(a,30_000))
  now+=30_000;gate.prune();assertTrue(gate.claim(a,30_000))
 }
 @Test fun `native command execution guard excludes command context and restores without stack polling`(){
  val context=ThreadLocal<Any>();val guard=CommandDamageGuard(context)
  assertTrue(guard.gameplay());context.set(Any());assertFalse(guard.gameplay())
  context.remove();assertTrue(guard.gameplay())
 }
 @Test fun `temporary armor reduction retains other modifiers and never writes base or heals`(){
  val attribute=mock(org.bukkit.attribute.AttributeInstance::class.java);val key=org.bukkit.NamespacedKey("ecoenchants","letalidad")
  val other=org.bukkit.attribute.AttributeModifier(org.bukkit.NamespacedKey("other","armor"),4.0,org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER)
  val old=org.bukkit.attribute.AttributeModifier(key,-.05,org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1)
  `when`(attribute.modifiers).thenReturn(listOf(other,old))
  for(tier in 1..5){val actual=NativeProcAdapters.temporaryReduction(attribute,key,.04*tier);assertEquals(-.04*tier,actual.amount);verify(attribute).addTransientModifier(actual)}
  verify(attribute,times(5)).removeModifier(old);verify(attribute,never()).removeModifier(other);verify(attribute,never()).setBaseValue(anyDouble())
  assertThrows(IllegalArgumentException::class.java){NativeProcAdapters.temporaryReduction(attribute,key,.21)}
 }
 @Test fun `all five tiers refresh four additive stacks and expire independently`(){
  for(tier in 1..5){
   val stack=ArmorStack(tier)
   assertFalse(stack.live(0))
   repeat(4){i->stack.hit(i*100L);assertEquals((i+1)*tier/100.0,stack.fraction(),1e-12)}
   repeat(12){stack.hit(400L+it)};assertEquals(4,stack.count);assertEquals(0.04*tier,stack.fraction(),1e-12)
   assertTrue(stack.live(4410));assertFalse(stack.live(4411));stack.hit(4411);assertEquals(1,stack.count)
  }
 }
 @Test fun `strongest attacker contribution is used without adding players`(){
  val first=ArmorStack(5);repeat(4){first.hit(0)}
  val second=ArmorStack(3);repeat(4){second.hit(1000)}
  val third=ArmorStack(4);third.hit(2000)
  val all=listOf(first,second,third)
  assertEquals(.20,all.filter{it.live(3000)}.maxOf{it.fraction()},1e-12)
  assertEquals(.12,all.filter{it.live(4500)}.maxOf{it.fraction()},1e-12)
  assertEquals(.04,all.filter{it.live(5500)}.maxOf{it.fraction()},1e-12)
  assertTrue(all.none{it.live(6000)})
 }
 @Test fun `last resort requires genuine living downward crossing and cannot rearm below forty`(){
  assertTrue(NativeCombatRules.crossed(40.0,39.0,100.0))
  assertTrue(NativeCombatRules.crossed(100.0,1.0,100.0))
  for((before,after,max) in listOf(Triple(39.0,38.0,100.0),Triple(60.0,40.0,100.0),Triple(50.0,0.0,100.0),Triple(50.0,60.0,100.0),Triple(50.0,30.0,0.0),Triple(Double.NaN,30.0,100.0),Triple(50.0,Double.NaN,100.0)))assertFalse(NativeCombatRules.crossed(before,after,max))
 }
 @Test fun `native combat excludes irreversible environmental custom and command damage causes`(){
  val allowed=setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK,EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,EntityDamageEvent.DamageCause.PROJECTILE)
  for(cause in EntityDamageEvent.DamageCause.entries)assertEquals(cause in allowed,NativeCombatRules.physical(cause))
 }
 @Test fun `general confirmation overload preserves accepted actual health loss and rejects cancelled or replaced cause`(){
  val event=mock(EntityDamageEvent::class.java);val victim=mock(LivingEntity::class.java)
  `when`(event.finalDamage).thenReturn(50.0);`when`(victim.health).thenReturn(5.0);`when`(victim.lastDamageCause).thenReturn(event)
  assertEquals(15.0,NativeProcAdapters.confirmed(event,victim,20.0))
  `when`(event.isCancelled).thenReturn(true);assertEquals(0.0,NativeProcAdapters.confirmed(event,victim,20.0))
  `when`(event.isCancelled).thenReturn(false);`when`(victim.lastDamageCause).thenReturn(mock(EntityDamageEvent::class.java));assertEquals(0.0,NativeProcAdapters.confirmed(event,victim,20.0))
 }
}
