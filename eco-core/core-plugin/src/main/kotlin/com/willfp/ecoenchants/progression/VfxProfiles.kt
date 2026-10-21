package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.inventory.ItemStack
import kotlin.math.*

/** Prepared cosmetic data and pure shared geometry. No entity access or gameplay expressions. */
internal object VfxProfiles {
 enum class Primitive { MARK, RING, ARC, FLASH }
 enum class Motion { STATIC, EXPAND, CONTRACT }
 data class Point(val x:Double,val y:Double,val z:Double)
 data class Step(val primitive:Primitive,val motion:Motion,val particle:Particle,val data:Any?,val at:Int,
                 val ticks:Int,val scale:Double,val count:Int,val height:Double)
 data class Phase(val steps:List<Step>,val sound:String,val soundTick:Int,val volume:Float,val pitch:Float,val soundAtTarget:Boolean=false) {
  val ticks=steps.maxOf { it.at+it.ticks }
 }
 data class Profile(val phases:Map<String,Phase>)
 fun compile(root:Config,soundValid:(String)->Boolean,warn:(String)->Unit):Map<String,Profile> {
  val result=linkedMapOf<String,Profile>()
  for(key in root.getKeys(false).take(16))try {
   require(key.matches(Regex("[a-z0-9_]{1,64}"))) { "Invalid profile key" }
   val profile=root.getSubsection(key)
   if(profile.getBoolOrNull("enabled")==false)continue
   val phases=linkedMapOf<String,Phase>()
   val sections=profile.getSubsection("phases")
   for(phase in sections.getKeys(false).take(4)) {
    require(phase in setOf("progress","resolved","link")) { "Invalid phase $phase" }
    val config=sections.getSubsection(phase)
    val configs=config.getSubsections("steps")
    require(configs.size in 1..4) { "A phase needs 1..4 steps" }
    val steps=configs.map { step ->
     val primitive=Primitive.valueOf(step.getString("primitive").uppercase())
     val motion=Motion.valueOf(step.getStringOrNull("motion")?.uppercase()?:"STATIC")
     val particle=Particle.valueOf(step.getString("particle").uppercase())
     val scale=(step.getDoubleOrNull("scale")?:1.0).also{require(it.isFinite()&&it>0)}.coerceAtMost(2.0)
     val height=(step.getDoubleOrNull("height")?:0.8).also{require(it.isFinite())}.coerceIn(-2.0,2.0)
     val at=(step.getIntOrNull("at")?:0).coerceIn(0,39)
     val ticks=(step.getIntOrNull("duration")?:1).coerceIn(1,40)
     require(at+ticks<=40) { "Phase exceeds 40 ticks" }
     val data:Any?=when(particle.dataType) {
      Void::class.java->null
      Particle.DustOptions::class.java->{
       val color=step.getStringOrNull("color")?:"#FFD166"
       require(color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid dust color" }
       Particle.DustOptions(Color.fromRGB(color.drop(1).toInt(16)),(step.getDoubleOrNull("size")?:0.6).also{require(it.isFinite()&&it>0)}.coerceAtMost(1.5).toFloat())
      }
      ItemStack::class.java->{val material=Material.valueOf(step.getString("material").uppercase());require(material.isItem&&!material.isAir);ItemStack(material)}
      org.bukkit.block.data.BlockData::class.java->{val material=Material.valueOf(step.getString("material").uppercase());require(material.isBlock&&!material.isAir);material.createBlockData()}
      else->throw IllegalArgumentException("Unsupported particle data for $particle")
     }
     Step(primitive,motion,particle,data,at,ticks,scale,(step.getIntOrNull("quantity")?:4).coerceIn(1,16),height)
    }
    val sound=config.getStringOrNull("sound")?:""
    require(sound.isEmpty()||soundValid(sound)) { "Unknown vanilla sound $sound" }
    val volume=(config.getDoubleOrNull("volume")?:0.25).also{require(it.isFinite())}.coerceIn(0.0,1.0).toFloat()
    val pitch=(config.getDoubleOrNull("pitch")?:1.0).also{require(it.isFinite())}.coerceIn(0.5,2.0).toFloat()
    val end=steps.maxOf{it.at+it.ticks}
    val soundTick=(config.getIntOrNull("sound-tick")?:0).coerceIn(0,end-1)
    val anchor=config.getStringOrNull("sound-anchor")?:"origin"
    require(anchor in setOf("origin","recipient")) { "Invalid sound anchor" }
    phases[phase]=Phase(steps.toList(),sound,soundTick,volume,pitch,anchor=="recipient")
   }
   require(phases.isNotEmpty()) { "No phases" }
   result[key]=Profile(phases.toMap())
  }catch(problem:RuntimeException){warn("Visual profile $key disabled: ${problem.message}")}
  return result.toMap()
 }
 fun points(step:Step,age:Int,origin:Point,target:Point,radiusLimit:Double,progress:Int):List<Point> {
  if(age !in step.at until step.at+step.ticks)return emptyList()
  val fraction=if(step.ticks==1)1.0 else (age-step.at).toDouble()/(step.ticks-1)
  val radius=min(step.scale,radiusLimit.coerceAtLeast(0.0))*when(step.motion){Motion.STATIC->1.0;Motion.EXPAND->fraction;Motion.CONTRACT->1-fraction}
  return (0 until step.count).map { i ->
   val angle=2*PI*i/step.count+progress*2*PI/3
   when(step.primitive) {
    Primitive.RING->Point(origin.x+cos(angle)*radius,origin.y+step.height,origin.z+sin(angle)*radius)
    Primitive.MARK->Point(target.x+cos(angle)*radius*.25,target.y+step.height,target.z+sin(angle)*radius*.25)
    Primitive.ARC->Point(origin.x+(target.x-origin.x)*fraction,origin.y+(target.y-origin.y)*fraction+sin(PI*fraction)*radius+step.height,origin.z+(target.z-origin.z)*fraction)
    Primitive.FLASH->Point(target.x+cos(angle)*radius*.15,target.y+step.height,target.z+sin(angle)*radius*.15)
   }
  }
 }
}
