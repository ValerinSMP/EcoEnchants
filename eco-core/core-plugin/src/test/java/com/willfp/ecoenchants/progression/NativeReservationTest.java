package com.willfp.ecoenchants.progression;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.willfp.ecoenchants.progression.OriginalProcEngine.*;
class NativeReservationTest {
 @Test void reservationDoesNotFabricateConfirmedHitsAndRollbackIsFenced(){
  var clock=new AtomicLong();var engine=new OriginalProcEngine(4,clock::get);UUID player=UUID.randomUUID();
  var old=engine.reserve(Family.AGUJA_AURORA,player,UUID.randomUUID()).orElseThrow();assertEquals(1,engine.retainedEntries());assertTrue(engine.reserve(Family.AGUJA_AURORA,player,UUID.randomUUID()).isEmpty());
  assertTrue(engine.rollback(Family.AGUJA_AURORA,player,old));assertFalse(engine.rollback(Family.AGUJA_AURORA,player,old));
  var fresh=engine.reserve(Family.AGUJA_AURORA,player,UUID.randomUUID()).orElseThrow();assertFalse(engine.rollback(Family.AGUJA_AURORA,player,old));assertEquals(1,engine.retainedEntries());assertTrue(engine.rollback(Family.AGUJA_AURORA,player,fresh));
 }
 @Test void oldTimeoutCannotUndoNewCooldownAndLogoutDoesNotEraseIt(){
  var clock=new AtomicLong();var engine=new OriginalProcEngine(4,clock::get);UUID player=UUID.randomUUID();var old=engine.reserve(Family.VIGILIA,player,UUID.randomUUID()).orElseThrow();
  engine.clearEntity(player);assertTrue(engine.reserve(Family.VIGILIA,player,UUID.randomUUID()).isEmpty());clock.set(20000);var fresh=engine.reserve(Family.VIGILIA,player,UUID.randomUUID()).orElseThrow();assertFalse(engine.rollback(Family.VIGILIA,player,old));assertTrue(engine.rollback(Family.VIGILIA,player,fresh));
 }
 @Test void reservationCapacityDoesNotEvictAndFamiliesStayIndependent(){
  var engine=new OriginalProcEngine(1,()->0);UUID player=UUID.randomUUID();var lease=engine.reserve(Family.ANCLA,player,UUID.randomUUID()).orElseThrow();assertTrue(engine.reserve(Family.ANCLA,UUID.randomUUID(),UUID.randomUUID()).isEmpty());assertTrue(engine.reserve(Family.VIGILIA,player,UUID.randomUUID()).isPresent());assertTrue(engine.rollback(Family.ANCLA,player,lease));
 }
}
