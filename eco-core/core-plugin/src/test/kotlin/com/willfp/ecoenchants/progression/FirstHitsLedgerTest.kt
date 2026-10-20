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
}
