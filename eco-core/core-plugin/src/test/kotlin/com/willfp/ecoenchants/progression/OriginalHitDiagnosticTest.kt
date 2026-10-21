package com.willfp.ecoenchants.progression

import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class OriginalHitDiagnosticTest {
 @Test fun `disabled and different player do not evaluate payload or write logs`() {
  val diagnostic=OriginalHitDiagnostic { 0 };val event=Any();val player=UUID.randomUUID();val rows=mutableListOf<String>()
  diagnostic.observe(player,event);diagnostic.note(event,"off") { error("disabled payload evaluated") }
  diagnostic.start(player,rows::add);diagnostic.observe(UUID.randomUUID(),event)
  diagnostic.note(event,"other") { error("other player payload evaluated") };assertTrue(rows.isEmpty())
 }
 @Test fun `at most eight event identities and 24 rows each are retained`() {
  val diagnostic=OriginalHitDiagnostic { 0 };val player=UUID.randomUUID();val rows=mutableListOf<String>();diagnostic.start(player,rows::add)
  repeat(12){val event=Any();diagnostic.observe(player,event);repeat(40){diagnostic.note(event,"stage")}}
  assertEquals(8*24,rows.size);assertTrue(rows.none { "hit=9 " in it })
 }
 @Test fun `expiry stop and restart never retain prior events or evaluate their payload`() {
  var now=0L;val diagnostic=OriginalHitDiagnostic { now };val player=UUID.randomUUID();val rows=mutableListOf<String>();val event=Any()
  diagnostic.start(player,rows::add);diagnostic.observe(player,event);diagnostic.note(event,"before");now=30_000
  diagnostic.note(event,"expired") { error("expired payload evaluated") };assertEquals(1,rows.size)
  diagnostic.start(player,rows::add);diagnostic.note(event,"old") { error("old event retained") }
  diagnostic.observe(player,event);diagnostic.clear();diagnostic.note(event,"stopped") { error("stopped event retained") }
 }
 @Test fun `same event follows real confirmation and dispatch stage order without spending a second hit`() {
  val diagnostic=OriginalHitDiagnostic { 0 };val player=UUID.randomUUID();val rows=mutableListOf<String>();val event=Any()
  diagnostic.start(player,rows::add)
  for(stage in listOf("observe","source","confirm","dispatch","native_dispatch","effect_entry","execute","accepted_hit")) {
   diagnostic.observe(player,event);diagnostic.note(event,stage)
  }
  assertEquals(8,rows.size);assertTrue(rows.all { "hit=1 " in it })
 }
 @Test fun `diagnostic sink or detail failure never changes the native call path`() {
  val player=UUID.randomUUID();val event=Any();val diagnostic=OriginalHitDiagnostic { 0 }
  diagnostic.start(player) { error("logger failure") };diagnostic.observe(player,event)
  assertDoesNotThrow { diagnostic.note(event,"sink") }
  diagnostic.start(player) { };diagnostic.observe(player,event)
  assertDoesNotThrow { diagnostic.note(event,"detail") { throw NoClassDefFoundError("diagnostic only") } }
 }

}
