package com.willfp.ecoenchants.progression

import java.util.UUID

/** MAIN-owned temporary proc state, including consumed but unsettled entries in its capacity. */
internal class ArmedProcLedger<K,V>(private val capacity:()->Int,private val clock:()->Long) {
 data class Entry<V>(val value:V,val until:Long,val revision:UUID=UUID.randomUUID())
 private val current=mutableMapOf<K,Entry<V>>()
 private val active=mutableMapOf<K,Entry<V>>()
 fun canArm(key:K)=key in current||current.size<capacity()
 fun arm(key:K,value:V,duration:Long):Entry<V>? {
  require(duration in 1..60_000)
  if(!canArm(key))return null
  val entry=Entry(value,clock()+duration);current[key]=entry;active[key]=entry;return entry
 }
 fun peek(key:K):Entry<V>? {
  val entry=active[key]?:return null
  if(entry.until<=clock()){retire(key);return null}
  return entry
 }
 fun consume(key:K,entry:Entry<V>):Boolean {
  if(current[key]!==entry||peek(key)!==entry)return false
  active.remove(key);return true
 }
 /** Only explicit rejection can restore the same unexpired revision. Unknown outcomes consume. */
 fun settle(key:K,entry:Entry<V>,rejected:Boolean):Boolean {
  if(current[key]!==entry||key in active)return false
  if(rejected&&entry.until>clock()){active[key]=entry;return true}
  current.remove(key);return false
 }
 fun retire(key:K):Entry<V>? {active.remove(key);return current.remove(key)}
 fun entriesSnapshot():Map<K,Entry<V>> = current.toMap()
 fun clear():List<Entry<V>> {val entries=current.values.toList();active.clear();current.clear();return entries}
 fun retainedEntries()=current.size
}
