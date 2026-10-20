package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class ArmedProcLedgerTest {
 @Test fun `explicit cancellation restores original deadline and consumes only once`(){
  var now=0L;val state=ArmedProcLedger<String,String>({4}){now};val first=state.arm("guard","original",3000)!!
  assertTrue(state.consume("guard",first));assertNull(state.peek("guard"));assertFalse(state.consume("guard",first));now=1000
  assertTrue(state.settle("guard",first,true));assertSame(first,state.peek("guard"));assertEquals(3000,first.until)
  assertTrue(state.consume("guard",first));assertFalse(state.settle("guard",first,false));assertNull(state.peek("guard"));assertEquals(0,state.retainedEntries())
 }
 @Test fun `old rejected callback cannot replace newer protection even if that newer one was consumed`(){
  val state=ArmedProcLedger<String,String>({4}){0L};val old=state.arm("actor","old",3000)!!;assertTrue(state.consume("actor",old))
  val fresh=state.arm("actor","new",3000)!!;assertTrue(state.consume("actor",fresh))
  assertFalse(state.settle("actor",old,true));assertTrue(state.settle("actor",fresh,true));assertEquals("new",state.peek("actor")!!.value)
 }
 @Test fun `uncertain accepted outcomes and expired cancellations never grant another bonus`(){
  var now=0L;val state=ArmedProcLedger<String,String>({4}){now}
  val first=state.arm("a","once",3000)!!;state.consume("a",first);assertFalse(state.settle("a",first,false));assertNull(state.peek("a"))
  val second=state.arm("b","expired",3000)!!;state.consume("b",second);now=3000;assertFalse(state.settle("b",second,true));assertNull(state.peek("b"));assertEquals(0,state.retainedEntries())
 }
 @Test fun `consumed entries count towards bounded capacity and retirement fences reconnection`(){
  val state=ArmedProcLedger<String,String>({1}){0L};val old=state.arm("uuid","first-session",3000)!!;state.consume("uuid",old)
  assertNull(state.arm("other","overflow",3000));state.retire("uuid");assertFalse(state.settle("uuid",old,true))
  val fresh=state.arm("uuid","second-session",3000)!!;assertFalse(state.settle("uuid",old,true));assertSame(fresh,state.peek("uuid"));assertEquals(1,state.clear().size);assertEquals(0,state.retainedEntries())
 }
 @Test fun `per-key expiry and invalid windows stay explicit`(){
  var now=0L;val state=ArmedProcLedger<String,String>({1}){now};state.arm("a","x",3000);now=3000;assertNull(state.peek("a"));assertEquals(0,state.retainedEntries())
  assertThrows(IllegalArgumentException::class.java){state.arm("a","x",0)};assertThrows(IllegalArgumentException::class.java){state.arm("a","x",60001)}
 }
}
