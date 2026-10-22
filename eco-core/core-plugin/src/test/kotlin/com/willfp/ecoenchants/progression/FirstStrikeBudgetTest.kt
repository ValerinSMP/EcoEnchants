package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class FirstStrikeBudgetTest {
 private fun key()=FirstHitsLedger.Key(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID())
 @Test fun oneHitBudgetDoesNotOfferTwoPendingBonusesAndRejectedDamageDoesNotSpendIt(){
  val ledger=FirstHitsLedger();val key=key();val first=UUID.randomUUID()
  assertTrue(ledger.reserve(first,key,1));assertTrue(ledger.reserve(first,key,1))
  assertFalse(ledger.reserve(UUID.randomUUID(),key,1))
  ledger.resolve(first,FirstHitResolution.REJECTED)
  val accepted=UUID.randomUUID();assertTrue(ledger.reserve(accepted,key,1))
  ledger.resolve(accepted,FirstHitResolution.CONFIRMED)
  assertFalse(ledger.reserve(UUID.randomUUID(),key,1))
  ledger.resolve(accepted,FirstHitResolution.REJECTED)
  assertFalse(ledger.reserve(UUID.randomUUID(),key,1))
 }
 @Test fun weaponSwitchSharesExistingThreeHitHistoryAndDoesNotResetTheFirstHit(){
  val ledger=FirstHitsLedger();val key=key();val first=UUID.randomUUID()
  assertTrue(ledger.reserve(first,key,1));ledger.resolve(first,FirstHitResolution.CONFIRMED)
  repeat(2){val id=UUID.randomUUID();assertTrue(ledger.reserve(id,key));ledger.resolve(id,FirstHitResolution.CONFIRMED)}
  assertFalse(ledger.reserve(UUID.randomUUID(),key,1));assertFalse(ledger.reserve(UUID.randomUUID(),key))
 }
 @Test fun uncertaintyFailsClosedAndSessionOrTargetRetirementFencesLateConfirmations(){
  val ledger=FirstHitsLedger();val key=key();val first=UUID.randomUUID()
  ledger.reserve(first,key,1);ledger.resolve(first,FirstHitResolution.UNCERTAIN)
  assertFalse(ledger.reserve(UUID.randomUUID(),key,1))
  ledger.retire(key.actor);val next=UUID.randomUUID();assertTrue(ledger.reserve(next,key.copy(session=UUID.randomUUID()),1))
  ledger.resolve(first,FirstHitResolution.CONFIRMED);ledger.resolve(next,FirstHitResolution.REJECTED)
  ledger.retire(key.target);assertTrue(ledger.reserve(UUID.randomUUID(),key,1))
 }
 @Test fun onlyApprovedEnchantBudgetsAreSelectableAndLegacyThreeHitBudgetIsUnchanged(){
  assertTrue(firstHitBudgetAllowed("first_strike",1));assertTrue(firstHitBudgetAllowed("triple_strike",3))
  for((id,budget) in listOf("first_strike" to 3,"triple_strike" to 1,"other" to 1))assertFalse(firstHitBudgetAllowed(id,budget))
  val ledger=FirstHitsLedger();val key=key();assertFalse(ledger.reserve(UUID.randomUUID(),key,0));assertFalse(ledger.reserve(UUID.randomUUID(),key,4))
 }
}
