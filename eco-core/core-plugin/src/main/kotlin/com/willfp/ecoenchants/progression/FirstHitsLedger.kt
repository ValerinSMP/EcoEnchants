package com.willfp.ecoenchants.progression

import java.util.UUID

internal enum class FirstHitResolution { CONFIRMED, REJECTED, UNCERTAIN }
/** Reservation budget: uncertain already-offered damage consumes quota instead of granting a retry. */
internal class FirstHitsLedger(private val maxPairs:Int=32768,private val maxPerActor:Int=256,private val cyclic:Boolean=false) {
 constructor(maxPairs:Int,maxPerActor:Int):this(maxPairs,maxPerActor,false)
 data class Key(val actor:UUID,val session:UUID,val world:UUID,val target:UUID)
 private class Record(var confirmed:Int=0,var uncertain:Int=0,val pending:MutableSet<UUID> = mutableSetOf())
 private val records=mutableMapOf<Key,Record>()
 private val attempts=mutableMapOf<UUID,Key>()
 private val actors=mutableMapOf<UUID,MutableSet<Key>>()
 private val targets=mutableMapOf<UUID,MutableSet<Key>>()
 fun reserve(attempt:UUID,key:Key):Boolean = reserve(attempt,key,3)
 fun reserve(attempt:UUID,key:Key,hits:Int):Boolean {
  if(hits !in 1..3)return false
  attempts[attempt]?.let{return it==key&&(hits==3||(position(attempt)?:Int.MAX_VALUE)<=hits)}
  if(attempts.size>=1024)return false
  var record=records[key]
  if(record==null){
   if(records.size>=maxPairs||(actors[key.actor]?.size?:0)>=maxPerActor)return false
   record=Record();records[key]=record
   actors.getOrPut(key.actor){mutableSetOf()}.add(key);targets.getOrPut(key.target){mutableSetOf()}.add(key)
  }
  if(cyclic&&record.pending.isNotEmpty())return false
  if(record.confirmed+record.uncertain+record.pending.size>=hits)return false
  record.pending.add(attempt);attempts[attempt]=key;return true
 }
 fun position(attempt:UUID):Int? {
  val key=attempts[attempt]?:return null;val record=records[key]?:return null
  if(cyclic&&record.uncertain>0)return null
  return record.confirmed+record.uncertain+record.pending.indexOf(attempt)+1
 }
 fun resolve(attempt:UUID,resolution:FirstHitResolution){
  val key=attempts.remove(attempt)?:return;val record=records[key]?:return
  record.pending.remove(attempt)
  when(resolution){FirstHitResolution.CONFIRMED->record.confirmed++;FirstHitResolution.UNCERTAIN->record.uncertain++;FirstHitResolution.REJECTED->Unit}
  if(record.pending.isEmpty()&&(record.confirmed+record.uncertain==0||cyclic&&record.confirmed+record.uncertain>=3))remove(key)
 }
 private fun remove(key:Key){
  val record=records.remove(key)?:return;record.pending.forEach{attempts.remove(it)}
  actors[key.actor]?.let{it.remove(key);if(it.isEmpty())actors.remove(key.actor)}
  targets[key.target]?.let{it.remove(key);if(it.isEmpty())targets.remove(key.target)}
 }
 fun retire(uuid:UUID){
  actors[uuid]?.toList()?.forEach{remove(it)}
  targets[uuid]?.toList()?.forEach{remove(it)}
 }
 fun clear(){records.clear();attempts.clear();actors.clear();targets.clear()}
}
