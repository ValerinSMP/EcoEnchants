package com.willfp.ecoenchants.progression

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class FirstHitsLedgerTest {
 private fun key()=FirstHitsLedger.Key(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID())
 @Test fun `three reservations bound same tick attacks and confirmed duplicates never spend twice`(){
  val ledger=FirstHitsLedger();val key=key();val ids=List(3){UUID.randomUUID()}
  ids.forEach{assertTrue(ledger.reserve(it,key));assertTrue(ledger.reserve(it,key))}
  assertFalse(ledger.reserve(UUID.randomUUID(),key))
  ids.forEach{ledger.resolve(it,FirstHitResolution.CONFIRMED);ledger.resolve(it,FirstHitResolution.CONFIRMED)}
  assertFalse(ledger.reserve(UUID.randomUUID(),key))
 }
 @Test fun `explicit rejection releases while uncertainty never authorizes another bonus`(){
  val ledger=FirstHitsLedger();val key=key();val rejected=UUID.randomUUID()
  assertTrue(ledger.reserve(rejected,key));ledger.resolve(rejected,FirstHitResolution.REJECTED)
  repeat(3){val id=UUID.randomUUID();assertTrue(ledger.reserve(id,key));ledger.resolve(id,FirstHitResolution.UNCERTAIN)}
  assertFalse(ledger.reserve(UUID.randomUUID(),key))
 }
 @Test fun `capacity does not evict an exhausted target to reset its three hit budget`(){
  val ledger=FirstHitsLedger(1,1);val key=key()
  repeat(3){val id=UUID.randomUUID();assertTrue(ledger.reserve(id,key));ledger.resolve(id,FirstHitResolution.CONFIRMED)}
  assertFalse(ledger.reserve(UUID.randomUUID(),key.copy(target=UUID.randomUUID())))
  assertFalse(ledger.reserve(UUID.randomUUID(),key))
  ledger.retire(key.target)
  assertTrue(ledger.reserve(UUID.randomUUID(),key.copy(target=UUID.randomUUID())))
 }
 @Test fun `session retirement fences old confirmations and actor and target indexes clean both directions`(){
  val ledger=FirstHitsLedger();val key=key();val old=UUID.randomUUID()
  assertTrue(ledger.reserve(old,key));ledger.retire(key.actor)
  val fresh=key.copy(session=UUID.randomUUID());val next=UUID.randomUUID();assertTrue(ledger.reserve(next,fresh))
  ledger.resolve(old,FirstHitResolution.CONFIRMED);ledger.resolve(next,FirstHitResolution.CONFIRMED)
  repeat(2){val id=UUID.randomUUID();assertTrue(ledger.reserve(id,fresh));ledger.resolve(id,FirstHitResolution.CONFIRMED)}
  assertFalse(ledger.reserve(UUID.randomUUID(),fresh));ledger.clear();assertTrue(ledger.reserve(UUID.randomUUID(),fresh))
 }
 @org.junit.jupiter.api.Test fun `cyclic ledger requires two confirmed hits and rejects overlapping pending attempts`() {
  val ledger=FirstHitsLedger(cyclic=true)
  val key=FirstHitsLedger.Key(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID())
  repeat(3){cycle->
   for(position in 1..3){
    val attempt=java.util.UUID.randomUUID()
    org.junit.jupiter.api.Assertions.assertTrue(ledger.reserve(attempt,key))
    org.junit.jupiter.api.Assertions.assertEquals(position,ledger.position(attempt))
    org.junit.jupiter.api.Assertions.assertFalse(ledger.reserve(java.util.UUID.randomUUID(),key))
    ledger.resolve(attempt,FirstHitResolution.CONFIRMED)
   }
  }
 }
 @org.junit.jupiter.api.Test fun `uncertain cycle consumes the attempt but never authorizes a third hit bonus`() {
  val ledger=FirstHitsLedger(cyclic=true)
  val key=FirstHitsLedger.Key(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID())
  val first=java.util.UUID.randomUUID();org.junit.jupiter.api.Assertions.assertTrue(ledger.reserve(first,key));ledger.resolve(first,FirstHitResolution.UNCERTAIN)
  repeat(2){val id=java.util.UUID.randomUUID();org.junit.jupiter.api.Assertions.assertTrue(ledger.reserve(id,key));org.junit.jupiter.api.Assertions.assertNull(ledger.position(id));ledger.resolve(id,FirstHitResolution.CONFIRMED)}
  val next=java.util.UUID.randomUUID();org.junit.jupiter.api.Assertions.assertTrue(ledger.reserve(next,key));org.junit.jupiter.api.Assertions.assertEquals(1,ledger.position(next))
 }
 @org.junit.jupiter.api.Test fun `rejected cycle preserves position and retirement fences late outcomes`() {
  val ledger=FirstHitsLedger(cyclic=true)
  val key=FirstHitsLedger.Key(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.util.UUID.randomUUID())
  val first=java.util.UUID.randomUUID();ledger.reserve(first,key);ledger.resolve(first,FirstHitResolution.CONFIRMED)
  val rejected=java.util.UUID.randomUUID();ledger.reserve(rejected,key);ledger.resolve(rejected,FirstHitResolution.REJECTED)
  val next=java.util.UUID.randomUUID();ledger.reserve(next,key);org.junit.jupiter.api.Assertions.assertEquals(2,ledger.position(next))
  ledger.retire(key.actor);ledger.resolve(next,FirstHitResolution.CONFIRMED)
  val fresh=java.util.UUID.randomUUID();ledger.reserve(fresh,key);org.junit.jupiter.api.Assertions.assertEquals(1,ledger.position(fresh))
 }
}
