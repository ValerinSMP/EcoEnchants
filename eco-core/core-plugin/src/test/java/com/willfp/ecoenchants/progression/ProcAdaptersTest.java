package com.willfp.ecoenchants.progression;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.willfp.ecoenchants.progression.OriginalProcEngine.Family.*;
class ProcAdaptersTest {
 @Test void deadOrDisconnectedOwnersCannotDriveAnImpactEvenIfAProjectileSurvives(){
  assertTrue(ProcGuards.mayAct(true,false));assertFalse(ProcGuards.mayAct(false,false));
  assertFalse(ProcGuards.mayAct(true,true));assertFalse(ProcGuards.mayAct(false,true));
 }
 @Test void requiresRealHealthLossAndSameOriginalDamageEvent(){
  assertEquals(12,ProcGuards.confirmedDamage(false,true,12,30,18));
  assertEquals(0,ProcGuards.confirmedDamage(true,true,12,30,18));
  assertEquals(0,ProcGuards.confirmedDamage(false,false,12,30,18));
  assertEquals(0,ProcGuards.confirmedDamage(false,true,12,30,30));
  assertEquals(0,ProcGuards.confirmedDamage(false,true,12,30,31));
 }
 @Test void ignoresOverkillAndNeverCountsNonfiniteOrZeroFinalDamage(){
  assertEquals(5,ProcGuards.confirmedDamage(false,true,100,5,0));
  assertEquals(0,ProcGuards.confirmedDamage(false,true,0,10,0));
  assertEquals(0,ProcGuards.confirmedDamage(false,true,Double.NaN,10,0));
  assertEquals(0,ProcGuards.confirmedDamage(false,true,10,Double.NaN,0));
 }
 @Test void onlyUnmarkedNaturalOrdinaryMonstersAreEligible(){
  assertTrue(ProcGuards.eligible(true,"NATURAL",false,false,false,false,false,false));
  for(String spawn:List.of("SPAWNER","TRIAL_SPAWNER","CUSTOM","SPAWNER_EGG","DEFAULT","COMMAND"))
   assertFalse(ProcGuards.eligible(true,spawn,false,false,false,false,false,false),spawn);
  assertFalse(ProcGuards.eligible(false,"NATURAL",false,false,false,false,false,false));
 }
 @Test void authoritativeBossMythicMetadataAlliesInvulnerabilityAndProtectionEachDeny(){
  for(int i=0;i<6;i++){
   boolean[] v=new boolean[6];v[i]=true;
   assertFalse(ProcGuards.eligible(true,"NATURAL",v[0],v[1],v[2],v[3],v[4],v[5]));
  }
 }
 @Test void transferAndReconnectArrivalGuardCannotShortenEitherCooldown(){
  assertFalse(ProcGuards.arrivalReady(ECO_RESONANTE,7999));assertTrue(ProcGuards.arrivalReady(ECO_RESONANTE,8000));
  assertFalse(ProcGuards.arrivalReady(SELLO_GLACIAL,9999));assertTrue(ProcGuards.arrivalReady(SELLO_GLACIAL,10000));
 }
 @Test void arrowKeepsOriginalItemIdentityAndTierInsteadOfWeaponAtImpact(){
  var clock=new AtomicLong();var shots=new ShotLedger<String>(4,30000,clock::get);
  UUID arrow=UUID.randomUUID(),owner=UUID.randomUUID();
  assertTrue(shots.remember(arrow,owner,"bow-A:id1:tier3"));
  assertFalse(shots.remember(arrow,owner,"bow-B:id2:tier1"));
  assertEquals("bow-A:id1:tier3",shots.source(arrow));
  assertTrue(shots.claim(arrow,owner));assertFalse(shots.claim(arrow,owner));
 }
 @Test void arrowTtlAndCapacityFailClosedWithoutEvictingAnotherValidShot(){
  var clock=new AtomicLong();var shots=new ShotLedger<String>(1,30000,clock::get);
  UUID arrow=UUID.randomUUID(),owner=UUID.randomUUID();
  assertTrue(shots.remember(arrow,owner,"A"));assertFalse(shots.remember(UUID.randomUUID(),owner,"B"));
  clock.set(29999);assertEquals("A",shots.source(arrow));
  clock.set(30000);assertNull(shots.source(arrow));assertFalse(shots.claim(arrow,owner));assertEquals(0,shots.size());
 }
 @Test void disconnectAndWorldRetirementInvalidateAllOwnedShotContexts(){
  var shots=new ShotLedger<String>(4,30000,()->0);UUID a=UUID.randomUUID(),b=UUID.randomUUID(),arrow=UUID.randomUUID(),other=UUID.randomUUID();
  shots.remember(arrow,a,"A");shots.remember(other,b,"B");shots.removeOwner(a);
  assertNull(shots.source(arrow));assertEquals("B",shots.source(other));shots.clear();assertEquals(0,shots.size());
 }
 @Test void piercingArrowCanNeverFeedMoreThanEightDistinctTargets(){
  var shots=new ShotLedger<String>(4,30000,()->0);UUID arrow=UUID.randomUUID();shots.remember(arrow,UUID.randomUUID(),"A");
  for(int i=0;i<8;i++)assertTrue(shots.claim(arrow,UUID.randomUUID()));
  assertFalse(shots.claim(arrow,UUID.randomUUID()));
 }
 @Test void particleBudgetSeparatesActivationObserverAndGlobalLimits(){
  var clock=new AtomicLong();var budget=new ParticleBudget(clock::get);UUID a=UUID.randomUUID(),b=UUID.randomUUID();
  assertEquals(12,budget.take(a,16,12,20,30));
  assertEquals(8,budget.take(a,16,12,20,30));
  assertEquals(10,budget.take(b,16,16,20,30));
  assertEquals(0,budget.take(b,16,16,20,30));
  clock.set(1000);assertEquals(16,budget.take(a,16,16,20,30));
 }
 @Test void visualObserverTableIsBoundedAndZeroBudgetIsHarmless(){
  var budget=new ParticleBudget(()->0);
  for(int i=0;i<5000;i++)budget.take(UUID.randomUUID(),1,1,1,10000);
  assertEquals(256,budget.observers());assertEquals(0,budget.take(UUID.randomUUID(),16,16,0,0));
  budget.clear();assertEquals(0,budget.observers());
 }
 @Test void visualExhaustionDoesNotAlterMechanicalState(){
  var budget=new ParticleBudget(()->0);assertEquals(0,budget.take(UUID.randomUUID(),16,16,0,0));
  var engine=new OriginalProcEngine(8,()->0);UUID player=UUID.randomUUID(),item=UUID.randomUUID(),target=UUID.randomUUID(),world=UUID.randomUUID();
  for(int i=0;i<3;i++){
   var hit=new OriginalProcEngine.Hit(UUID.randomUUID(),player,item,target,world,1,100,true,true,true,false,false,false,false,false,true,false,false,false);
   assertEquals(i==2,engine.record(ECO_RESONANTE,hit).isPresent());
  }
 }
}
