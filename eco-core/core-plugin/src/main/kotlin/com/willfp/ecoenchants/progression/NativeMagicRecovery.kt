package com.willfp.ecoenchants.progression

import com.willfp.ecoenchants.EcoEnchantsPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import java.util.logging.Level

internal data class ManaCredit(val whole:Int,val remainder:Int)
/** .25% per tier = tier/400 exactly; no floating drift or invented minimum of one mana. */
internal fun manaCredit(limit:Int,tier:Int,remainder:Int):ManaCredit {
 require(limit>=0&&tier in 1..3&&remainder in 0..399)
 val numerator=limit.toLong()*tier+remainder
 return ManaCredit((numerator/400).toInt(),(numerator%400).toInt())
}
/** Public native EcoSkills access, resolved per actual plugin instance; no profile reads/defaults. */
internal object NativeMagicRecovery {
 private lateinit var owner:EcoEnchantsPlugin
 private var capacity:()->Int={512}
 private data class Bridge(val plugin:Plugin,val type:Class<*>,val registry:Any,val lookup:Method,val peek:Method,val maximum:Method,val set:Method)
 private var bridge:Bridge?=null
 private var failed:WeakReference<Plugin>?=null
 private var warned:WeakReference<Plugin>?=null
 private val carry=mutableMapOf<UUID,Int>() // EcoEnchants-owned earned fraction, less than one native mana point.
 fun enable(plugin:EcoEnchantsPlugin,limit:()->Int){owner=plugin;capacity=limit}
 fun reset(){carry.clear();bridge=null;failed=null;warned=null}
 fun retire(uuid:UUID){carry.remove(uuid)}
 fun disabled(plugin:Plugin){if(bridge?.plugin===plugin||failed?.get()===plugin)reset()}
 private fun fail(plugin:Plugin,problem:Throwable){
  val root=(problem as? InvocationTargetException)?.targetException?:problem
  if(failed?.get()!==plugin)owner.logger.log(Level.SEVERE,"Mana steal unavailable: native EcoSkills cached magic API failed",root)
  bridge=null;carry.clear();failed=WeakReference(plugin)
 }
 private fun access():Bridge? {
  val plugin=Bukkit.getPluginManager().getPlugin("EcoSkills")
  if(plugin==null||!plugin.isEnabled){if(bridge!=null||failed?.get()!=null)reset();return null}
  bridge?.takeIf{it.plugin===plugin}?.let{return it}
  if(failed?.get()===plugin)return null
  bridge=null;carry.clear()
  return try{
   val loader=plugin.javaClass.classLoader
   val type=Class.forName("com.willfp.ecoskills.magic.MagicType",false,loader)
   val registryClass=Class.forName("com.willfp.ecoskills.magic.MagicTypes",true,loader)
   val registry=registryClass.getField("INSTANCE").get(null)
   val api=Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI",false,loader)
   val map=Class.forName("com.willfp.ecoskills.magic.MagicMapKt",false,loader)
   val peek=map.getMethod("peekObservedMagic",UUID::class.java,type)
   val maximum=api.getMethod("getMaxMagic",Player::class.java,type)
   val set=api.getMethod("setMagic",Player::class.java,type,Int::class.javaPrimitiveType)
   check(peek.returnType==Integer::class.java&&maximum.returnType==Int::class.javaPrimitiveType&&set.returnType==Void.TYPE){"Incompatible native magic signatures"}
   Bridge(plugin,type,registry,registryClass.getMethod("getByID",String::class.java),peek,maximum,set).also{bridge=it;failed=null}
  }catch(problem:ReflectiveOperationException){fail(plugin,problem);null}
   catch(problem:RuntimeException){fail(plugin,problem);null}
   catch(problem:LinkageError){fail(plugin,problem);null}
 }
 fun recover(player:Player,tier:Int):Boolean {
  if(!Bukkit.isPrimaryThread()||!player.isOnline||player.isDead||tier !in 1..3)return false
  val native=access()?:return false;val uuid=player.uniqueId
  try{
   val mana=native.lookup.invoke(native.registry,"mana")?:run{carry.remove(uuid);return false}
   if(!native.type.isInstance(mana)){carry.remove(uuid);return false}
   val current=(native.peek.invoke(null,uuid,mana) as? Number)?.toInt()?:run{carry.remove(uuid);return false}
   val maximum=(native.maximum.invoke(null,player,mana) as Number).toInt()
   if(current<0||maximum<=0||current>=maximum){carry.remove(uuid);return false}
   val credit=manaCredit(maximum,tier,carry[uuid]?:0)
   if(credit.remainder!=0&&uuid !in carry&&carry.size>=capacity())return false
   if(credit.whole==0){carry[uuid]=credit.remainder;return true}
   val wanted=(current.toLong()+credit.whole).coerceAtMost(maximum.toLong()).toInt()
   val result=confirmMagicWrite(wanted,{native.set.invoke(null,player,mana,wanted)},{(native.peek.invoke(null,uuid,mana) as? Number)?.toInt()})
   if(!result.confirmed){carry.remove(uuid);result.failure?.let{fail(native.plugin,it)};return false}
   if(wanted>=maximum||credit.remainder==0)carry.remove(uuid) else carry[uuid]=credit.remainder
   if(result.failure!=null&&warned?.get()!==native.plugin){
    warned=WeakReference(native.plugin)
    owner.logger.log(Level.WARNING,"Mana steal native write confirmed after observer exception; grant not repeated",(result.failure as? InvocationTargetException)?.targetException?:result.failure)
   }
   return true
  }catch(problem:ReflectiveOperationException){fail(native.plugin,problem);return false}
   catch(problem:RuntimeException){fail(native.plugin,problem);return false}
   catch(problem:LinkageError){fail(native.plugin,problem);return false}
 }
}

internal data class MagicWriteResult(val confirmed:Boolean,val failure:Throwable?)
/** One invocation plus native readback; exceptions after a successful write never cause a retry. */
internal fun confirmMagicWrite(wanted:Int,write:()->Unit,read:()->Int?):MagicWriteResult {
 var failure:Throwable?=null
 try{write()}catch(problem:ReflectiveOperationException){failure=problem}
 return MagicWriteResult(read()==wanted,failure)
}
