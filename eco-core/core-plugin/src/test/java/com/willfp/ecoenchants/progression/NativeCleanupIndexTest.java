package com.willfp.ecoenchants.progression;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.willfp.ecoenchants.progression.OriginalProcEngine.*;
class NativeCleanupIndexTest {
 Hit hit(UUID actor,UUID target,UUID item){return new Hit(UUID.randomUUID(),actor,item,target,UUID.randomUUID(),1,10,true,true,true,false,false,false,false,false,true,false,false,false);}
 @org.junit.jupiter.api.Test void combatModesAreExplicitAndInvalidSettingsFailClosed(){
  for(boolean player:new boolean[]{false,true}){
   org.junit.jupiter.api.Assertions.assertTrue(ProcGuards.combatModeAllows(null,player));
   org.junit.jupiter.api.Assertions.assertTrue(ProcGuards.combatModeAllows("both",player));
   org.junit.jupiter.api.Assertions.assertEquals(player,ProcGuards.combatModeAllows(" PvP ",player));
   org.junit.jupiter.api.Assertions.assertEquals(!player,ProcGuards.combatModeAllows("PvE",player));
   org.junit.jupiter.api.Assertions.assertFalse(ProcGuards.combatModeAllows("invalid",player));
   org.junit.jupiter.api.Assertions.assertFalse(ProcGuards.combatModeAllows("",player));
  }
 }
 @Test void unrelatedEntityCleanupPreservesCountersAndActorCleanupDropsAllItsLinks(){
  var clock=new AtomicLong();var engine=new OriginalProcEngine(1024,clock::get);UUID actor=UUID.randomUUID(),other=UUID.randomUUID();
  for(int i=0;i<500;i++)engine.record(Family.LEGADO_VALERIN,hit(actor,UUID.randomUUID(),UUID.randomUUID()));
  assertEquals(500,engine.windowReferences(actor));int before=engine.retainedEntries();
  for(int i=0;i<1000;i++)engine.clearEntity(UUID.randomUUID());assertEquals(before,engine.retainedEntries());assertEquals(500,engine.windowReferences(actor));
  var one=hit(other,UUID.randomUUID(),UUID.randomUUID());engine.record(Family.ECO_RESONANTE,one);engine.clearEntity(actor);assertEquals(0,engine.windowReferences(actor));assertEquals(1,engine.windowReferences(other));engine.clearEntity(one.target());assertEquals(0,engine.windowReferences(other));
 }
 @Test void expiryAndCompletedCounterRemoveReverseLinksIncludingSameActorTarget(){
  var clock=new AtomicLong();var engine=new OriginalProcEngine(64,clock::get);UUID actor=UUID.randomUUID(),target=UUID.randomUUID(),item=UUID.randomUUID(),world=UUID.randomUUID();
  for(int n=0;n<3;n++)engine.record(Family.ECO_RESONANTE,new Hit(UUID.randomUUID(),actor,item,target,world,1,10,true,true,true,false,false,false,false,false,true,false,false,false));
  assertEquals(0,engine.windowReferences(actor));assertEquals(0,engine.windowReferences(target));
  var same=hit(actor,actor,item);engine.record(Family.GUARDIA_CRISTAL,same);assertEquals(1,engine.windowReferences(actor));clock.set(4000);engine.expire(4000);assertEquals(0,engine.windowReferences(actor));engine.clear();assertEquals(0,engine.retainedEntries());
 }
 @Test void projectileOwnerIndexSurvivesDuplicateClaimsAndExpiresWithoutGhosts(){
  var clock=new AtomicLong();var shots=new ShotLedger<String>(1024,30000,clock::get);UUID actor=UUID.randomUUID(),other=UUID.randomUUID(),first=UUID.randomUUID(),target=UUID.randomUUID();
  assertTrue(shots.remember(first,actor,"original"));assertFalse(shots.remember(first,other,"replacement"));assertTrue(shots.claim(first,target));assertFalse(shots.claim(first,target));assertEquals(0,shots.ownerReferences(other));assertEquals("original",shots.source(first));
  for(int i=0;i<500;i++)shots.remember(UUID.randomUUID(),actor,"copy");assertEquals(501,shots.ownerReferences(actor));
  for(int i=0;i<1000;i++)shots.removeOwner(UUID.randomUUID());assertEquals(501,shots.size());shots.removeOwner(actor);assertEquals(0,shots.size());assertEquals(0,shots.ownerReferences(actor));
  shots.remember(first,other,"new");clock.set(30000);assertNull(shots.source(first));assertEquals(0,shots.ownerReferences(other));assertTrue(shots.remember(first,actor,"after expiry"));shots.clear();assertEquals(0,shots.ownerReferences(actor));
 }
}
