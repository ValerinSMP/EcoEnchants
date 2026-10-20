package com.willfp.ecoenchants.progression;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.willfp.ecoenchants.progression.OriginalProcEngine.*;

class OriginalProcEngineTest {
 final UUID player=UUID.randomUUID(),item=UUID.randomUUID(),target=UUID.randomUUID(),world=UUID.randomUUID();
 final AtomicLong clock=new AtomicLong();
 Hit hit(UUID event,UUID object,UUID victim,int tier,double damage){
  return new Hit(event,player,object,victim,world,tier,damage,true,true,true,false,false,false,false,false,true,false,false,false);
 }
 Hit hit(){return hit(UUID.randomUUID(),item,target,1,100);}
 @Test void resonanceUsesThirdConfirmedFinalHitAndHasExactCaps(){
  var e=new OriginalProcEngine(64,clock::get);assertTrue(e.record(Family.ECO_RESONANTE,hit()).isEmpty());assertTrue(e.record(Family.ECO_RESONANTE,hit()).isEmpty());
  var a=e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),item,target,3,40)).orElseThrow();
  assertEquals(8,a.value());assertEquals(40,a.sourceFinalDamage());assertEquals(2,a.maxTargets());assertEquals(2,a.radius());assertEquals(16,a.maxParticles());
 }
 @Test void sealNeedsTwoHitsAndPreservesPercentRatherThanPotionRounding(){
  var e=new OriginalProcEngine(64,clock::get);e.record(Family.SELLO_GLACIAL,hit());
  var a=e.record(Family.SELLO_GLACIAL,hit(UUID.randomUUID(),item,target,2,10)).orElseThrow();
  assertEquals(15,a.value());assertEquals(40,a.durationTicks());assertEquals(12,a.maxParticles());
 }
 @Test void itemAndTargetCountersAreIndependent(){
  var e=new OriginalProcEngine(64,clock::get);e.record(Family.ECO_RESONANTE,hit());e.record(Family.ECO_RESONANTE,hit());
  assertTrue(e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),UUID.randomUUID(),target,1,10)).isEmpty());
  assertTrue(e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),item,UUID.randomUUID(),1,10)).isEmpty());
  assertTrue(e.record(Family.ECO_RESONANTE,hit()).isPresent());
 }
 @Test void exactWindowExpiryResetsCounterWithoutPolling(){
  var e=new OriginalProcEngine(64,clock::get);e.record(Family.SELLO_GLACIAL,hit());clock.set(4000);
  assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isEmpty());assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isPresent());
 }
 @Test void swappingWeaponOrClearingPlayerDoesNotBypassCooldown(){
  var e=new OriginalProcEngine(64,clock::get);e.record(Family.SELLO_GLACIAL,hit());assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isPresent());
  e.clearEntity(player);UUID other=UUID.randomUUID();
  for(int i=0;i<5;i++)assertTrue(e.record(Family.SELLO_GLACIAL,hit(UUID.randomUUID(),other,target,1,10)).isEmpty());
  clock.set(10000);assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isEmpty());assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isPresent());
 }
 @Test void duplicateEventCannotCountTwice(){
  var e=new OriginalProcEngine(64,clock::get);var same=hit();
  assertTrue(e.record(Family.SELLO_GLACIAL,same).isEmpty());assertTrue(e.record(Family.SELLO_GLACIAL,same).isEmpty());
  assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isPresent());
 }
 @Test void unsafeOrUnconfirmedHitsConsumeNeitherCounterNorCooldown()throws Exception{
  var components=Hit.class.getRecordComponents();var ctor=Hit.class.getDeclaredConstructors()[0];
  for(String field:List.of("damageConfirmed","direct","monster","cancelled","protectedTarget","npc","pet","ally","bossExcluded","spawner","invulnerable","secondary")){
   var base=hit();Object[] values=new Object[components.length];
   for(int i=0;i<values.length;i++){values[i]=components[i].getAccessor().invoke(base);if(components[i].getName().equals(field))values[i]=!(Boolean)values[i];}
   var e=new OriginalProcEngine(64,clock::get);
   assertTrue(e.record(Family.SELLO_GLACIAL,(Hit)ctor.newInstance(values)).isEmpty(),field);assertEquals(0,e.retainedEntries(),field);
  }
 }
 @Test void zeroNonfiniteAndInvalidTiersCannotProc(){
  var e=new OriginalProcEngine(64,clock::get);
  for(double d:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})assertTrue(e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),item,target,1,d)).isEmpty());
  for(int t:new int[]{0,4})assertTrue(e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),item,target,t,10)).isEmpty());
  assertEquals(0,e.retainedEntries());
 }
 @Test void entriesStayBoundedAndCooldownIsNeverEvictedToAdmitAnotherPlayer(){
  int cap=4;var e=new OriginalProcEngine(cap,clock::get);
  for(int i=0;i<10000;i++)e.record(Family.ECO_RESONANTE,hit(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,10));
  assertTrue(e.retainedEntries()<=cap*5);
  clock.set(6000);e.expire(clock.get());assertEquals(0,e.retainedEntries());
 }
 @Test void fullCooldownCapacityDeniesWithoutEvictingExistingCooldown(){
  var e=new OriginalProcEngine(1,clock::get);
  e.record(Family.SELLO_GLACIAL,hit());clock.set(1001);assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isPresent());
  clock.set(2002);var h=hit();var other=new Hit(h.event(),UUID.randomUUID(),item,target,world,1,10,true,true,true,false,false,false,false,false,true,false,false,false);
  assertTrue(e.record(Family.SELLO_GLACIAL,other).isEmpty());
  assertTrue(e.record(Family.SELLO_GLACIAL,hit()).isEmpty());
 }
 @Test void cleanupRemovesPlayerOrTargetCountersAndShutdownDropsAllState(){
  var e=new OriginalProcEngine(64,clock::get);e.record(Family.ECO_RESONANTE,hit());e.clearEntity(target);
  assertTrue(e.record(Family.ECO_RESONANTE,hit()).isEmpty());e.clear();assertEquals(0,e.retainedEntries());
 }
 @Test void stateIsMainThreadOnly()throws Exception{
  var e=new OriginalProcEngine(64,clock::get);var failure=new AtomicReference<Throwable>();
  var t=new Thread(()->{try{e.record(Family.SELLO_GLACIAL,hit());}catch(Throwable ex){failure.set(ex);}});t.start();t.join();
  assertInstanceOf(IllegalStateException.class,failure.get());
 }
}
