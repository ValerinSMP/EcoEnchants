package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.mockito.Answers
import org.bukkit.Particle
import java.util.UUID

@Suppress("UNCHECKED_CAST")
internal fun visualConfig(values:Map<String,Any?>):Config = mock(Config::class.java) { call ->
 val path=call.arguments.firstOrNull() as? String
 fun value():Any? = path?.split('.')?.fold(values as Any?){current,key -> (current as? Map<*,*>)?.get(key)}
 when(call.method.name){
  "getKeys"->values.keys.toList()
  "getSubsection","getSubsectionOrNull"->(value() as? Map<String,Any?>)?.let{visualConfig(it)}?:visualConfig(emptyMap())
  "getSubsections","getSubsectionsOrNull"->(value() as? List<*>)?.map{visualConfig(it as Map<String,Any?>)}?:emptyList<Config>()
  "getString","getStringOrNull"->value() as? String
  "getBoolOrNull"->value() as? Boolean
  "getDoubleOrNull"->(value() as? Number)?.toDouble()
  "getIntOrNull"->(value() as? Number)?.toInt()
  else->Answers.RETURNS_DEFAULTS.answer(call)
 }
}
internal fun visualSpec(sound:String="")=mapOf<String,Any?>("enabled" to true,"phases" to mapOf("resolved" to mapOf("sound" to sound,"steps" to listOf(mapOf("primitive" to "RING","particle" to "END_ROD","duration" to 3,"quantity" to 3,"scale" to 2.0)))))
class NativeVfxTest {
 @Test fun `the two delivered YAML profiles prepare without an ID specific renderer`() {
  val path=java.io.File("../../delivery/native-vfx-20261021/visuals.yml")
  val values=org.yaml.snakeyaml.Yaml().load<Map<String,Any?>>(path.readText())
  val config=visualConfig(values).getSubsection("original-procs.visuals.profiles")
  val profiles=VfxProfiles.compile(config,{it.startsWith("minecraft:")},{throw AssertionError(it)})
  assertEquals(setOf("eco_resonante","brasa_errante"),profiles.keys)
  assertTrue(profiles.getValue("brasa_errante").phases.getValue("link").soundAtTarget)
  assertEquals(setOf("progress","resolved"),profiles.getValue("eco_resonante").phases.keys)
 }
 @Test fun `postdamage visual witness rejects stale cancelled immune and unrelated damage`() {
  val player=mock(org.bukkit.entity.Player::class.java);val target=mock(org.bukkit.entity.LivingEntity::class.java)
  val event=mock(org.bukkit.event.entity.EntityDamageByEntityEvent::class.java)
  `when`(target.lastDamageCause).thenReturn(event);`when`(target.health).thenReturn(10.0)
  `when`(event.damager).thenReturn(player);`when`(event.finalDamage).thenReturn(5.0)
  assertTrue(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,event))
  `when`(event.isCancelled).thenReturn(true);assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  `when`(event.isCancelled).thenReturn(false);`when`(event.finalDamage).thenReturn(0.0)
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
  `when`(event.finalDamage).thenReturn(5.0);`when`(event.damager).thenReturn(mock(org.bukkit.entity.Player::class.java))
  assertFalse(NativeProcAdapters.resolvedBy(player,target,15.0,null))
 }

 @Test fun `arbitrary profile keys compile and disabled profiles disappear on reload`() {
  val mutable=visualSpec().toMutableMap()
  val config=visualConfig(mapOf("reusable_example" to mutable))
  val first=VfxProfiles.compile(config,{true},{fail(it)})
  assertEquals(setOf("reusable_example"),first.keys)
  mutable["enabled"]=false
  assertTrue(VfxProfiles.compile(config,{true},{fail(it)}).isEmpty())
  assertEquals(3,first.getValue("reusable_example").phases.getValue("resolved").ticks)
 }
 @Test fun `invalid particle sound geometry and nonfinite values disable only their profile`() {
  for(change in listOf(mapOf("particle" to "NO_PARTICLE"),mapOf("scale" to Double.NaN),mapOf("primitive" to "DAMAGE"),mapOf("at" to 39,"duration" to 2))){
   val invalid=mapOf("phases" to mapOf("resolved" to mapOf("steps" to listOf(mapOf("primitive" to "RING","particle" to "END_ROD")+change))))
   val warnings=mutableListOf<String>()
   val prepared=VfxProfiles.compile(visualConfig(mapOf("valid" to visualSpec(),"invalid" to invalid)),{true},warnings::add)
   assertEquals(setOf("valid"),prepared.keys);assertEquals(1,warnings.size)
  }
  assertTrue(VfxProfiles.compile(visualConfig(mapOf("bad_sound" to visualSpec("not:vanilla"))),{false},{}).isEmpty())
 }
 @Test fun `geometry uses shared primitives stays in actual pulse radius and links real endpoints`() {
  val origin=VfxProfiles.Point(0.0,0.0,0.0);val target=VfxProfiles.Point(3.0,0.0,0.0)
  for(kind in VfxProfiles.Primitive.entries){
   val step=VfxProfiles.Step(kind,VfxProfiles.Motion.EXPAND,Particle.END_ROD,null,0,4,2.0,8,0.0)
   assertTrue(VfxProfiles.points(step,-1,origin,target,1.0,1).isEmpty());assertTrue(VfxProfiles.points(step,4,origin,target,1.0,1).isEmpty())
   val points=VfxProfiles.points(step,3,origin,target,1.0,1);assertEquals(8,points.size)
   if(kind==VfxProfiles.Primitive.RING)points.forEach{assertTrue(it.x*it.x+it.z*it.z<=1.000001)}
   if(kind==VfxProfiles.Primitive.ARC){assertEquals(origin,VfxProfiles.points(step,0,origin,target,1.0,0).first());assertEquals(target.x,points.first().x);assertEquals(target.y,points.first().y,1e-9)}
  }
 }
 @Test fun `hard profile limits and shared legacy particle budget also cover scenes`() {
  val step=mapOf("primitive" to "MARK","particle" to "END_ROD","quantity" to 5000,"duration" to 5000,"scale" to 5000.0)
  val profile=mapOf("phases" to mapOf("progress" to mapOf("steps" to listOf(step))))
  val value=VfxProfiles.compile(visualConfig(mapOf("large" to profile)),{true},{fail(it)}).getValue("large").phases.getValue("progress").steps.single()
  assertEquals(16,value.count);assertEquals(40,value.ticks);assertEquals(2.0,value.scale)
  val budget=ParticleBudget{0};val observer=UUID.randomUUID()
  assertEquals(16,budget.take(observer,16,16,20,32));assertEquals(4,budget.take(observer,16,16,20,32))
  assertEquals(12,budget.take(UUID.randomUUID(),16,16,20,32));assertEquals(0,budget.take(UUID.randomUUID(),16,16,20,32))
 }
 @Test fun `observation reports only native accepted hits and cannot alter activation on cosmetic failure`() {
  val engine=OriginalProcEngine(64){0};val owner=UUID.randomUUID();val target=UUID.randomUUID();val item=UUID.randomUUID();val world=UUID.randomUUID()
  fun hit(event:UUID,cancelled:Boolean=false)=OriginalProcEngine.Hit(event,owner,item,target,world,1,10.0,true,true,true,cancelled,false,false,false,false,true,false,false,false)
  val progress=mutableListOf<Int>();val first=hit(UUID.randomUUID())
  assertTrue(engine.record(OriginalProcEngine.Family.ECO_RESONANTE,first,progress::add).isEmpty)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,first,progress::add)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID(),true),progress::add)
  assertEquals(listOf(1),progress)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID()),progress::add)
  val result=engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID())){throw IllegalArgumentException("cosmetic")}
  assertTrue(result.isPresent);assertEquals(4.0,result.get().value());assertEquals(listOf(1,2),progress)
  engine.record(OriginalProcEngine.Family.ECO_RESONANTE,hit(UUID.randomUUID()),progress::add)
  assertEquals(listOf(1,2),progress)
 }
}
