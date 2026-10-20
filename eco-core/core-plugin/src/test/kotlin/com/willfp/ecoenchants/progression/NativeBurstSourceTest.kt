package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class NativeBurstSourceTest {
 @Test fun `secondary protection blocks attack recursion but preserves native death XP drops and receivers`(){
  val dispatcher=org.mockito.Mockito.mock(com.willfp.libreforge.Dispatcher::class.java)
  for(id in listOf("melee_attack","bow_attack","trident_attack","headshot","kill","entity_xp_drop","entity_item_drop","take_entity_damage","take_damage")){
   val trigger=org.mockito.Mockito.mock(com.willfp.libreforge.triggers.Trigger::class.java)
   org.mockito.Mockito.`when`(trigger.id).thenReturn(id)
   val dispatch=org.mockito.Mockito.mock(com.willfp.libreforge.triggers.DispatchedTrigger::class.java)
   org.mockito.Mockito.`when`(dispatch.trigger).thenReturn(trigger)
   val event=com.willfp.libreforge.triggers.event.TriggerDispatchEvent(dispatcher,dispatch)
   OriginalRuntime.withSecondaryDamage{OriginalRuntime.noSecondaryProcs(event)}
   assertEquals(id in setOf("melee_attack","bow_attack","trident_attack","headshot"),event.isCancelled,id)
   val primary=com.willfp.libreforge.triggers.event.TriggerDispatchEvent(dispatcher,dispatch);OriginalRuntime.noSecondaryProcs(primary);assertFalse(primary.isCancelled,id)
  }
 }

 @Test fun `earned extra hit window runs once and restores protection even on failure`(){
  val target=org.mockito.Mockito.mock(org.bukkit.entity.LivingEntity::class.java)
  var ticks=17;var calls=0
  org.mockito.Mockito.`when`(target.noDamageTicks).thenAnswer{ticks}
  org.mockito.Mockito.doAnswer{ticks=it.getArgument(0);null}.`when`(target).setNoDamageTicks(org.mockito.ArgumentMatchers.anyInt())
  freshDamageWindow(target){assertEquals(0,ticks);calls++;ticks=20};assertEquals(20,ticks);assertEquals(1,calls)
  ticks=17
  assertThrows(IllegalStateException::class.java){freshDamageWindow(target){calls++;throw IllegalStateException("receiver callback")}}
  assertEquals(17,ticks);assertEquals(2,calls)
 }

 @Test fun `charged arrows underwater tridents kill gates and melee paths stay distinct`(){
  for(id in listOf("rebote_prismatico","cometa")){
   assertTrue(NativeBurstProgression.accepts(id,"arrow",true,false,false));assertFalse(NativeBurstProgression.accepts(id,"arrow",false,false,false));assertFalse(NativeBurstProgression.accepts(id,"melee",true,false,false))
  }
  assertTrue(NativeBurstProgression.accepts("pulso_abisal","trident",true,true,false));assertFalse(NativeBurstProgression.accepts("pulso_abisal","trident",true,false,false))
  assertTrue(NativeBurstProgression.accepts("brasa_errante","melee",true,false,true));assertFalse(NativeBurstProgression.accepts("brasa_errante","melee",true,false,false));assertFalse(NativeBurstProgression.accepts("brasa_errante","arrow",true,false,true))
  assertTrue(NativeBurstProgression.accepts("estela_cazador","ranged_kill",true,false,true));assertFalse(NativeBurstProgression.accepts("estela_cazador","ranged_kill",true,false,false))
  for(id in listOf("cicatriz_ignea","ruptura_sonica","tormenta_encadenada","legado_valerin")){assertTrue(NativeBurstProgression.accepts(id,"melee",true,false,false));assertFalse(NativeBurstProgression.accepts(id,"arrow",true,false,false))}
  assertFalse(NativeBurstProgression.accepts("unknown","melee",true,true,true))
 }
}
