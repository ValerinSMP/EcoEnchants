package com.willfp.ecoenchants.progression
import java.util.UUID
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask

internal fun protectsImmatureCrop(type:Material,age:Int?,maximum:Int?):Boolean =
 type in setOf(Material.MELON_STEM,Material.PUMPKIN_STEM,Material.ATTACHED_MELON_STEM,Material.ATTACHED_PUMPKIN_STEM)||
 (type in setOf(Material.WHEAT,Material.CARROTS,Material.POTATOES,Material.BEETROOTS,Material.NETHER_WART)&&
  age!=null&&maximum!=null&&age>=0&&age<maximum)

internal class MiningRhythmState(val item:UUID,val tier:Int,val world:UUID,var last:Long) {
 var count=0
 var expiry:BukkitTask?=null
 fun active(at:Long)=count>=10&&at-last in 0 until 10_000
 fun record(at:Long):Boolean {
  val before=active(at)
  if(at-last>=10_000||at<last)count=0
  last=at;count=minOf(10,count+1)
  return !before&&active(at)
 }
}
