package com.willfp.ecoenchants.progression;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.willfp.ecoenchants.progression.OriginalProcEngine.*;
class NativeBurstEngineTest {
 final UUID player=UUID.randomUUID(),item=UUID.randomUUID(),target=UUID.randomUUID(),world=UUID.randomUUID();
 Hit hit(UUID event,int tier){return new Hit(event,player,item,target,world,tier,100,true,true,true,false,false,false,false,false,true,false,false,false);}
 @Test void everyNativeFamilyTierNeedsItsExactHitCountAndPlayerCooldown(){
  for(var f:Family.values())for(int tier=1;tier<=f.values.length;tier++){
   var time=new AtomicLong();var engine=new OriginalProcEngine(64,time::get);
   for(int n=1;n<f.hits;n++)assertTrue(engine.record(f,hit(UUID.randomUUID(),tier)).isEmpty(),f.name());
   var result=engine.record(f,hit(UUID.randomUUID(),tier)).orElseThrow();assertEquals(f.values[tier-1],result.value());assertEquals(100,result.sourceFinalDamage());
   engine.clearEntity(player);
   if(f.cooldown==0){assertTrue(engine.record(f,hit(UUID.randomUUID(),tier)).isPresent(),f.name());continue;}
   assertTrue(engine.record(f,hit(UUID.randomUUID(),tier)).isEmpty());time.set(f.cooldown);
   for(int n=1;n<f.hits;n++)assertTrue(engine.record(f,hit(UUID.randomUUID(),tier)).isEmpty());assertTrue(engine.record(f,hit(UUID.randomUUID(),tier)).isPresent());
  }
 }
 @Test void sameConfirmedHitCanCountOnceForEachCoexistingFamily(){
  var engine=new OriginalProcEngine(128,()->0);UUID event=UUID.randomUUID();
  assertTrue(engine.record(Family.CICATRIZ_IGNEA,hit(event,1)).isEmpty());assertTrue(engine.record(Family.RUPTURA_SONICA,hit(event,1)).isEmpty());
  assertEquals(4,engine.retainedEntries());
  assertTrue(engine.record(Family.CICATRIZ_IGNEA,hit(event,1)).isEmpty());assertEquals(4,engine.retainedEntries());
  assertTrue(engine.record(Family.CICATRIZ_IGNEA,hit(UUID.randomUUID(),1)).isEmpty());assertTrue(engine.record(Family.CICATRIZ_IGNEA,hit(UUID.randomUUID(),1)).isPresent());
 }
 @Test void legacyOneTierAndBoundedActivationMetadataAreExact(){
  var engine=new OriginalProcEngine(64,()->0);assertTrue(engine.record(Family.LEGADO_VALERIN,hit(UUID.randomUUID(),2)).isEmpty());assertEquals(0,engine.retainedEntries());
  for(var f:List.of(Family.BRASA_ERRANTE,Family.PULSO_ABISAL,Family.REBOTE_PRISMATICO)){
   var result=engine.record(f,hit(UUID.randomUUID(),1)).orElseThrow();assertTrue(result.maxTargets()>=1&&result.maxTargets()<=2);assertTrue(result.radius()>=2&&result.radius()<=3);
  }
 }
 @Test void arrivalFenceCoversEveryFamilyWithoutChangingOldBoundaries(){
  for(var f:Family.values()){assertFalse(ProcGuards.arrivalReady(f,f.cooldown-1));assertTrue(ProcGuards.arrivalReady(f,f.cooldown));}
  assertFalse(ProcGuards.arrivalReady(null,Long.MAX_VALUE));
 }
 @Test void fixedWindowExpiryDoesNotPermitStaleFinishingHit(){
  var time=new AtomicLong();var engine=new OriginalProcEngine(64,time::get);
  for(int n=0;n<7;n++)assertTrue(engine.record(Family.LEGADO_VALERIN,hit(UUID.randomUUID(),1)).isEmpty());
  time.set(10000);assertTrue(engine.record(Family.LEGADO_VALERIN,hit(UUID.randomUUID(),1)).isEmpty());
 }
}
