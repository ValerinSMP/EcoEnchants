package com.willfp.ecoenchants.progression

import com.willfp.eco.core.blocks.Blocks
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchants
import com.willfp.ecoenchants.target.EnchantFinder.clearEnchantmentCache
import com.willfp.libreforge.*
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.triggers.*
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.block.*
import org.bukkit.event.player.*
import org.bukkit.event.inventory.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

internal object ManualCropGuard : Trigger("manual_crop_guard") {
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.BLOCK,TriggerParameter.ITEM,TriggerParameter.EVENT)
}
internal object EffectGuardImmature : Effect<NoCompileData>("guard_immature_crops") {
 override val parameters=ManualCropGuard.parameters
 override val supportsDelay=false
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean {
  val event=data.event as? BlockBreakEvent ?: return false
  if(!NativeMiningProgression.guarding(event))return false
  event.isCancelled=true
  return true
 }
}
internal object ConditionMiningRhythm : Condition<NoCompileData>("has_mining_rhythm") {
 override val invalidatedBy=emptySet<HolderChange>() // Explicit transition invalidation, no interval polling.
 override fun isMet(dispatcher:Dispatcher<*>,config:Config,holder:ProvidedHolder,compileData:NoCompileData):Boolean {
  val player=dispatcher.get<Player>() ?: return false
  val supplied=holder.provider as? ItemStack ?: return false
  val level=holder.holder as? com.willfp.ecoenchants.enchant.EcoEnchantLevel ?: return false
  return level.enchant.id=="ritmo_minero"&&NativeMiningProgression.active(player,supplied,level.level)
 }
}
/** Uses native break attribution, forceHolders, conditions, and mining_speed_multiplier lifecycle. */
object NativeMiningProgression : Listener {
 private lateinit var owner:EcoEnchantsPlugin
 private val itemKey=NamespacedKey("ecoenchants","original_item_id")
 private data class Mark(val world:UUID,val x:Int,val y:Int,val z:Int,val item:UUID,val tier:Int,val time:Long)

 private val marks=mutableMapOf<UUID,Mark>()
 private val sequences=mutableMapOf<UUID,MiningRhythmState>()
 private val pending=mutableMapOf<UUID,UUID>() // attempt -> actor; identity fences late callbacks
 private val pendingCounts=mutableMapOf<UUID,Int>()
 private val cropMarks=mutableMapOf<UUID,Mark>()
 private val checks=mutableMapOf<UUID,UUID>()
 private var guard:BlockBreakEvent?=null
 private var installed=false
 private fun now()=System.nanoTime()/1_000_000L
 private fun limit()=(owner.configYml.getIntOrNull("native-mining.max-players")?:512).coerceIn(1,2048)
 private fun stale(block:Block)=block.hasMetadata("blockbreakevent-ignore")||Blocks.isCustomBlock(block)
 private fun mineral(type:Material)=type in setOf(Material.STONE,Material.DEEPSLATE,Material.NETHERRACK,Material.END_STONE)||type.name.endsWith("_ORE")||type==Material.ANCIENT_DEBRIS
 private fun id(item:ItemStack):UUID?=item.itemMeta?.persistentDataContainer?.get(itemKey,PersistentDataType.STRING)?.let{runCatching{UUID.fromString(it)}.getOrNull()}
 private fun tier(item:ItemStack,name:String):Int=EcoEnchants.getByID(name)?.let{item.getEnchantmentLevel(it.enchantment)}?:0
 private fun current(player:Player,sequence:MiningRhythmState)=player.isOnline&&!player.isDead&&player.world.uid==sequence.world&&id(player.inventory.itemInMainHand)==sequence.item&&tier(player.inventory.itemInMainHand,"ritmo_minero")==sequence.tier
 fun active(player:Player):Boolean=sequences[player.uniqueId]?.let{current(player,it)&&it.active(now())}?:false
 fun active(player:Player,supplied:ItemStack,tier:Int):Boolean=sequences[player.uniqueId]?.let{
  it.tier==tier&&id(supplied)==it.item&&current(player,it)&&it.active(now())
 }?:false
 fun guarding(event:BlockBreakEvent)=guard===event
 internal fun withGuard(event:BlockBreakEvent,body:()->Unit){val previous=guard;guard=event;try{body()}finally{guard=previous}}
 @JvmStatic fun register(){Triggers.register(ManualCropGuard);Effects.register(EffectGuardImmature);Conditions.register(ConditionMiningRhythm)}
 @JvmStatic fun enable(plugin:EcoEnchantsPlugin){if(installed)return;owner=plugin;Bukkit.getPluginManager().registerEvents(this,plugin);installed=true}
 @JvmStatic fun reset(){marks.clear();cropMarks.clear();checks.clear();pending.clear();pendingCounts.clear();sequences.keys.toList().forEach{retire(it)}}
 @JvmStatic fun close(){reset();HandlerList.unregisterAll(this);installed=false}
 private fun invalidate(player:Player){val dispatcher=player.toDispatcher();dispatcher.updateEffects();dispatcher.flushHolders()}
 private fun clearSequence(uuid:UUID){val old=sequences.remove(uuid)?:return;old.expiry?.cancel();Bukkit.getPlayer(uuid)?.takeIf{it.isOnline}?.let{invalidate(it)}}
 private fun retire(uuid:UUID){marks.remove(uuid);cropMarks.remove(uuid);pending.entries.removeIf{it.value==uuid};pendingCounts.remove(uuid);checks.remove(uuid);clearSequence(uuid)}
 private fun expiry(uuid:UUID,sequence:MiningRhythmState){
  sequence.expiry=Bukkit.getScheduler().runTaskLater(owner,Runnable{
   if(sequences[uuid]!==sequence)return@Runnable
   val player=Bukkit.getPlayer(uuid)
   val remaining=10_000-(now()-sequence.last)
   if(player==null||!current(player,sequence))retire(uuid)
   else if(remaining<=0)clearSequence(uuid)
   else expiry(uuid,sequence)
  },maxOf(1L,(10_000-(now()-sequence.last)+49)/50))
 }
 private fun freeze(player:Player,item:ItemStack):NativeProcAdapters.FrozenItem? {
  val frozen=NativeProcAdapters.capture(item,itemKey)?:return null
  if(frozen.stamped){
   if(player.inventory.itemInMainHand!=item)return null
   player.inventory.setItemInMainHand(frozen.item)
   player.clearEnchantmentCache()
   val dispatcher=player.toDispatcher();dispatcher.forceRefreshHolders();dispatcher.flushHolders()
  }
  return frozen
 }
 @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
 fun crops(event:BlockBreakEvent){
  val player=event.player;val item=player.inventory.itemInMainHand;val enchant=EcoEnchants.getByID("cuidado")?:return
  if(!item.type.name.endsWith("_HOE")||item.getEnchantmentLevel(enchant.enchantment)!=1||stale(event.block))return
  val block=event.block;val type=block.type
  val manual=cropMarks.remove(player.uniqueId)?:return
  if(manual.world!=block.world.uid||manual.x!=block.x||manual.y!=block.y||manual.z!=block.z||now()-manual.time>60_000)return
  val age=block.blockData as? Ageable
  val protectedCrop=protectsImmatureCrop(type,age?.age,age?.maximumAge)
  if(!protectedCrop||!AntigriefManager.canBreakBlock(player,block))return
  withGuard(event){ManualCropGuard.dispatch(player.toDispatcher(),TriggerData(player=player,block=block,item=item,event=event),listOf(ItemProvidedHolder(enchant.getLevel(1),item)))}
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 fun damage(event:BlockDamageEvent){
  val player=event.player;val item=player.inventory.itemInMainHand;val tier=tier(item,"ritmo_minero")
  if(item.type.name.endsWith("_HOE")&&tier(item,"cuidado")==1&&!stale(event.block)&&
    (cropMarks.size<limit()||cropMarks.containsKey(player.uniqueId))){
   val b=event.block;cropMarks[player.uniqueId]=Mark(b.world.uid,b.x,b.y,b.z,UUID(0,0),1,now())
  }
  if(tier !in 1..3||!item.type.name.endsWith("_PICKAXE")||!mineral(event.block.type)||stale(event.block))return
  if(marks.size>=limit()&&!marks.containsKey(player.uniqueId))return
  val frozen=freeze(player,item)?:return
  val block=event.block;marks[player.uniqueId]=Mark(block.world.uid,block.x,block.y,block.z,frozen.id,tier,now())
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 fun broken(event:BlockBreakEvent){
  val player=event.player;val uuid=player.uniqueId;val mark=marks[uuid]?:return;val block=event.block
  if(mark.world!=block.world.uid||mark.x!=block.x||mark.y!=block.y||mark.z!=block.z||now()-mark.time>60_000||stale(block)||!mineral(block.type))return
  marks.remove(uuid)
  if(!AntigriefManager.canBreakBlock(player,block)||(pendingCounts[uuid]?:0)>=8||pending.size>=limit())return
  val attempt=UUID.randomUUID();pending[attempt]=uuid;pendingCounts[uuid]=(pendingCounts[uuid]?:0)+1
  Bukkit.getScheduler().runTask(owner,Runnable{
   if(pending.remove(attempt)!=uuid)return@Runnable
   val left=(pendingCounts[uuid]?:1)-1;if(left==0)pendingCounts.remove(uuid) else pendingCounts[uuid]=left
   if(event.isCancelled||!player.isOnline||player.isDead||player.world.uid!=mark.world)return@Runnable
   if(!block.world.isChunkLoaded(block.x shr 4,block.z shr 4)||(!block.type.isAir&&block.type!=Material.WATER&&block.type!=Material.LAVA))return@Runnable
   val item=player.inventory.itemInMainHand
   if(id(item)!=mark.item||tier(item,"ritmo_minero")!=mark.tier)return@Runnable
   val enchant=EcoEnchants.getByID("ritmo_minero")?:return@Runnable;val holder=ItemProvidedHolder(enchant.getLevel(mark.tier),item)
   if(!holder.holder.conditions.areMet(player.toDispatcher(),holder))return@Runnable
   var seq=sequences[uuid]
   if(seq!=null&&(!current(player,seq)||now()-seq.last>=10_000)){clearSequence(uuid);seq=null}
   if(seq==null){if(sequences.size>=limit())return@Runnable;seq=MiningRhythmState(mark.item,mark.tier,mark.world,now());sequences[uuid]=seq;expiry(uuid,seq)}
   if(seq.record(now()))invalidate(player)
  })
 }
 private fun checkLater(player:Player){
  val uuid=player.uniqueId
  if((uuid !in sequences&&uuid !in marks&&uuid !in cropMarks)||uuid in checks||checks.size>=limit())return
  val attempt=UUID.randomUUID();checks[uuid]=attempt
  Bukkit.getScheduler().runTask(owner,Runnable{
   if(checks[uuid]!=attempt)return@Runnable
   checks.remove(uuid)
   val seq=sequences[uuid]
   if(seq!=null&&!current(player,seq))retire(uuid)
   marks.remove(uuid);cropMarks.remove(uuid)
  })
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun held(event:PlayerItemHeldEvent){if(event.newSlot!=event.previousSlot){retire(event.player.uniqueId);marks.remove(event.player.uniqueId)}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun swap(event:PlayerSwapHandItemsEvent){retire(event.player.uniqueId);marks.remove(event.player.uniqueId)}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun click(event:InventoryClickEvent){(event.whoClicked as? Player)?.let{checkLater(it)}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun drag(event:InventoryDragEvent){(event.whoClicked as? Player)?.let{checkLater(it)}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun drop(event:PlayerDropItemEvent){checkLater(event.player)}
 @EventHandler fun quit(event:PlayerQuitEvent){retire(event.player.uniqueId)}
 @EventHandler fun world(event:PlayerChangedWorldEvent){retire(event.player.uniqueId)}
 @EventHandler fun death(event:org.bukkit.event.entity.PlayerDeathEvent){retire(event.entity.uniqueId)}
 @EventHandler fun abort(event:BlockDamageAbortEvent){marks.remove(event.player.uniqueId);cropMarks.remove(event.player.uniqueId)}
}
