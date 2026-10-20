package com.willfp.ecoenchants.progression

import com.willfp.libreforge.triggers.event.*
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.lang.reflect.InvocationTargetException
import java.util.UUID

class NativeUtilityProgressionTest {
 private fun player()=mock(Player::class.java).also{`when`(it.uniqueId).thenReturn(UUID.randomUUID())}
 private fun drop(p:Player,xp:Int=7,cause:XpDropCause=XpDropCause.ENTITY,entity:org.bukkit.entity.Entity=mock(LivingEntity::class.java))=
  EditableXpDropEvent(xp,cause,XpDropContext(player=p,entity=entity),Location(null,0.0,0.0,0.0))
 @Test fun `vanilla mob XP doubles only once while preserving cancellation and original XP`(){
  val p=player();val event=drop(p)
  assertTrue(doubleMobXp(event,p));assertEquals(14,event.finalXp);assertEquals(7,event.originalXp)
  repeat(12){assertFalse(doubleMobXp(event,p))};assertEquals(14,event.finalXp)
  val cancelled=drop(p);cancelled.setCancelled(true);assertFalse(doubleMobXp(cancelled,p));assertEquals(0,cancelled.finalXp)
 }
 @Test fun `XP effect rejects player block zero unknown and foreign attribution instead of giving money or skill XP`(){
  val p=player()
  assertFalse(doubleMobXp(drop(p,entity=player()),p));assertFalse(doubleMobXp(drop(p,cause=XpDropCause.BLOCK),p));assertFalse(doubleMobXp(drop(p,0),p));assertFalse(doubleMobXp(drop(p),player()));assertFalse(doubleMobXp(null,p));assertFalse(doubleMobXp(drop(p),null))
  val max=drop(p,Int.MAX_VALUE);assertTrue(doubleMobXp(max,p));assertEquals(Int.MAX_VALUE,max.finalXp)
 }
 @Test fun `a quarter percent of a hundred mana is exact after four hits without inventing a minimum`(){
  var remainder=0;var awarded=0
  repeat(4){i->val next=manaCredit(100,1,remainder);remainder=next.remainder;awarded+=next.whole;assertEquals(if(i==3)1 else 0,awarded)}
  assertEquals(0,remainder)
 }
 @Test fun `all tiers and small large changing limits retain earned fractions without floating drift`(){
  for(limit in listOf(1,50,100,133,1000,Int.MAX_VALUE))for(tier in 1..3){
   var rem=0;var whole=0L
   repeat(400){val next=manaCredit(limit,tier,rem);whole+=next.whole;rem=next.remainder;assertTrue(rem in 0..399)}
   assertEquals(limit.toLong()*tier,whole);assertEquals(0,rem)
  }
  assertThrows(IllegalArgumentException::class.java){manaCredit(-1,1,0)};assertThrows(IllegalArgumentException::class.java){manaCredit(100,4,0)};assertThrows(IllegalArgumentException::class.java){manaCredit(100,1,400)}
 }
 @Test fun `uncertain native magic write is read back once and never repeated`(){
  var stored=10;var writes=0;var reads=0
  val result=confirmMagicWrite(11,{writes++;stored=11;throw InvocationTargetException(IllegalStateException("observer"))},{reads++;stored})
  assertTrue(result.confirmed);assertNotNull(result.failure);assertEquals(1,writes);assertEquals(1,reads)
  writes=0;reads=0;stored=10
  val failed=confirmMagicWrite(11,{writes++;throw InvocationTargetException(IllegalStateException("before-write"))},{reads++;stored})
  assertFalse(failed.confirmed);assertEquals(10,stored);assertEquals(1,writes);assertEquals(1,reads)
  assertFalse(confirmMagicWrite(11,{}, {null}).confirmed)
 }
}
