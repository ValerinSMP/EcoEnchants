package com.willfp.ecoenchants.progression

import com.willfp.ecoenchants.EcoEnchantsPlugin
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.scheduler.*
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID
import java.util.logging.Logger

class NativeVfxRuntimeTest {
 private fun field(name:String)=OriginalRuntime::class.java.getDeclaredField(name).also{it.isAccessible=true}
 private fun active()=(field("visualScenes").get(OriginalRuntime) as Map<*,*>).size
 private fun runScene(test:(Player,LivingEntity,World,MutableList<Runnable>,BukkitTask)->Unit){
  val owner=mock(EcoEnchantsPlugin::class.java);val player=mock(Player::class.java);val victim=mock(LivingEntity::class.java)
  val world=mock(World::class.java);val scheduler=mock(BukkitScheduler::class.java);val task=mock(BukkitTask::class.java)
  val jobs=mutableListOf<Runnable>();val id=UUID.randomUUID()
  `when`(player.uniqueId).thenReturn(id);`when`(player.isOnline).thenReturn(true);`when`(player.world).thenReturn(world)
  `when`(player.location).thenReturn(Location(world,0.0,64.0,0.0));`when`(player.canSee(player)).thenReturn(true);`when`(player.canSee(victim)).thenReturn(true)
  `when`(victim.uniqueId).thenReturn(UUID.randomUUID());`when`(victim.world).thenReturn(world);`when`(victim.isValid).thenReturn(true);`when`(victim.location).thenReturn(Location(world,1.0,64.0,0.0))
  `when`(world.uid).thenReturn(UUID.randomUUID());`when`(world.players).thenReturn(listOf(player));`when`(world.isChunkLoaded(anyInt(),anyInt())).thenReturn(true)
  val profiles=visualConfig(mapOf("example" to visualSpec("minecraft:block.note_block.bass")))
  val root=mock(com.willfp.eco.core.config.base.ConfigYml::class.java)
  `when`(root.getIntOrNull(anyString())).thenReturn(null)
  `when`(root.getBoolOrNull("original-procs.visuals.enabled")).thenReturn(false)
  `when`(owner.configYml).thenReturn(root);`when`(owner.logger).thenReturn(Logger.getLogger("vfx-test"))
  `when`(scheduler.runTaskTimer(eq(owner),any(Runnable::class.java),eq(1L),eq(1L))).thenAnswer{jobs.add(it.getArgument(1));task}
  val previousOwner=field("owner").get(OriginalRuntime)
  mockStatic(Bukkit::class.java).use{ bukkit ->
   bukkit.`when`<Boolean>{Bukkit.isPrimaryThread()}.thenReturn(true)
   bukkit.`when`<BukkitScheduler>{Bukkit.getScheduler()}.thenReturn(scheduler)
   bukkit.`when`<Player>{Bukkit.getPlayer(id)}.thenReturn(player)
   bukkit.`when`<Collection<Player>>{Bukkit.getOnlinePlayers()}.thenReturn(emptyList())
   field("owner").set(OriginalRuntime,owner)
   try{
    OriginalRuntime.reset()
    field("visualProfiles").set(OriginalRuntime,VfxProfiles.compile(profiles,{true},{throw AssertionError(it)}))
    test(player,victim,world,jobs,task)
   }finally{OriginalRuntime.reset();field("owner").set(OriginalRuntime,previousOwner)}
  }
 }
 private fun emit(player:Player,target:LivingEntity,event:UUID=UUID.randomUUID())=
  OriginalRuntime.procVisualProfile(player,player.location,"example","resolved",listOf(target),event,2.0)
 @Test fun `live renderer emits phases once shares hard budget and finishes its only timer`()=runScene{player,target,_,jobs,task ->
  val event=UUID.randomUUID();emit(player,target,event);emit(player,target,event)
  assertEquals(1,active());assertEquals(1,jobs.size)
  repeat(5){jobs.single().run()}
  assertEquals(0,active());verify(task,atLeastOnce()).cancel()
  val calls=mockingDetails(player).invocations
  assertEquals(9,calls.count{it.method.name=="spawnParticle"});assertEquals(1,calls.count{it.method.name=="playSound"})
 }
 @Test fun `saturation only discards cosmetic scenes and reload cancels every scene`()=runScene{player,target,_,jobs,task ->
  repeat(20){emit(player,target)};assertEquals(4,active());assertEquals(1,jobs.size)
  OriginalRuntime.reset();assertEquals(0,active());verify(task,atLeastOnce()).cancel()
  emit(player,target);assertEquals(0,active()) // global disabled on the prepared reload
 }
 @Test fun `quit retires indexed scenes and chunk unload cancels without loading chunks`()=runScene{player,target,world,_,task ->
  emit(player,target);assertEquals(1,active())
  val quit=mock(PlayerQuitEvent::class.java);`when`(quit.player).thenReturn(player);OriginalRuntime.quit(quit)
  assertEquals(0,active());verify(task,atLeastOnce()).cancel()
  emit(player,target);assertEquals(1,active())
  val event=mock(ChunkUnloadEvent::class.java);val chunk=mock(Chunk::class.java)
  `when`(event.world).thenReturn(world);`when`(event.chunk).thenReturn(chunk);`when`(chunk.x).thenReturn(0);`when`(chunk.z).thenReturn(0)
  OriginalRuntime.visualChunkUnload(event);assertEquals(0,active())
  verify(world,never()).getChunkAt(anyInt(),anyInt())
 }
 @Test fun `dead removed or moved receivers do not produce later frames`()=runScene{player,target,_,jobs,_ ->
  emit(player,target);clearInvocations(player)
  `when`(target.isDead).thenReturn(true);jobs.single().run()
  assertEquals(0,active());assertEquals(0,mockingDetails(player).invocations.count{it.method.name=="spawnParticle"})
 }
}
