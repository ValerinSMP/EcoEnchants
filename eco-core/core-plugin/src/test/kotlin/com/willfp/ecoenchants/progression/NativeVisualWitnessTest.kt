package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID
class NativeVisualWitnessTest {
 @Test fun `postdamage visual witness rejects stale cancelled immune and unrelated damage`() {
  val player=mock(org.bukkit.entity.Player::class.java);val target=mock(org.bukkit.entity.LivingEntity::class.java)
  val event=mock(org.bukkit.event.entity.EntityDamageByEntityEvent::class.java)
  `when`(target.lastDamageCause).thenReturn(event);`when`(target.health).thenReturn(10.0)
  `when`(event.damager).thenReturn(player);`when`(event.finalDamage).thenReturn(5.0)
  assertTrue(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,event))
  `when`(event.isCancelled).thenReturn(true);assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  `when`(event.isCancelled).thenReturn(false);`when`(event.finalDamage).thenReturn(0.0)
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  `when`(event.finalDamage).thenReturn(5.0);`when`(event.damager).thenReturn(mock(org.bukkit.entity.Player::class.java))
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
 }
 @Test fun `observation reports only native accepted hits and cannot alter activation on cosmetic failure`() {
  val engine=OriginalProcEngine(64){0};val owner=UUID.randomUUID();val target=UUID.randomUUID();val item=UUID.randomUUID();val world=UUID.randomUUID()
  fun hit(event:UUID,cancelled:Boolean=false)=OriginalProcEngine.Hit(event,owner,item,target,world,1,10.0,true,true,true,cancelled,false,false,false,false,true,false,false,false)
  val progress=mutableListOf<Int>();val first=hit(UUID.randomUUID())
  assertTrue(engine.record(OriginalProcEngine.Family.ECO_RESONANTE,first,progress::add).isEmpty)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,first,progress::add)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID(),true),progress::add)
  assertEquals(listOf(1),progress)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID()),progress::add)
  val result=engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID())){throw IllegalArgumentException("cosmetic")}
  assertTrue(result.isPresent);assertEquals(4.0,result.get().value());assertEquals(listOf(1,2),progress)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID()),progress::add)
  assertEquals(listOf(1,2),progress)
 }
 @Test fun legacyParticleBudgetRemainsBoundedForItsOtherConsumers() {
  val budget=ParticleBudget{0};val observer=UUID.randomUUID()
  assertEquals(16,budget.take(observer,16,16,20,32));assertEquals(4,budget.take(observer,16,16,20,32))
  assertEquals(12,budget.take(UUID.randomUUID(),16,16,20,32));assertEquals(0,budget.take(UUID.randomUUID(),16,16,20,32))
 }
}
