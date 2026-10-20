package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class ArmedProcLedgerTest {
 @Test fun `indexed cleanup visits only states associated with actor or target`(){
  var reads=0;val actor=java.util.UUID.randomUUID();val unrelated=java.util.UUID.randomUUID()
  val state=ArmedProcLedger<Int,java.util.UUID>({1024},entities={_,target->reads++;listOf(actor,target)}){0L}
  val targets=(0 until 500).map{java.util.UUID.randomUUID()};targets.forEachIndexed{key,target->state.arm(key,target,3000)}
  val before=reads;repeat(1000){assertTrue(state.retireEntity(unrelated).isEmpty())};assertEquals(before,reads);assertEquals(500,state.retainedEntries())
  assertEquals(1,state.retireEntity(targets[0]).size);assertEquals(499,state.retireEntity(actor).size);assertEquals(0,state.retainedEntries());assertTrue(state.retireEntity(targets[1]).isEmpty())
 }
 @Test fun `replacement expiry and consumed settlement clean only current entity links`(){
  var now=0L;val actor=java.util.UUID.randomUUID();val oldTarget=java.util.UUID.randomUUID();val newTarget=java.util.UUID.randomUUID()
  val state=ArmedProcLedger<String,java.util.UUID>({4},entities={_,target->listOf(actor,target)}){now}
  val old=state.arm("key",oldTarget,3000)!!;state.consume("key",old);val fresh=state.arm("key",newTarget,3000)!!
  assertTrue(state.retireEntity(oldTarget).isEmpty());assertSame(fresh,state.peek("key"));assertFalse(state.settle("key",old,true));assertTrue(state.consume("key",fresh));assertFalse(state.settle("key",fresh,false));assertTrue(state.retireEntity(newTarget).isEmpty())
  state.arm("key",newTarget,3000);now=3000;assertNull(state.peek("key"));assertTrue(state.retireEntity(actor).isEmpty())
 }

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
