package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.ecoenchants.enchant.EcoEnchants
import com.willfp.libreforge.*
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.impl.AdditiveDamageGroups
import com.willfp.libreforge.effects.impl.DamageMultiplierBypass
import com.willfp.libreforge.triggers.*
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent
import io.papermc.paper.event.entity.EntityKnockbackEvent
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.IdentityHashMap
import java.util.UUID

internal object NativeReactiveAction : Trigger("native_reactive_action") {
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.ITEM,TriggerParameter.EVENT,TriggerParameter.VALUE,TriggerParameter.LOCATION)
}
internal object EffectNativeReactive : Effect<NoCompileData>("native_reactive") {
 override val supportsDelay=false
 override val parameters=NativeReactiveAction.parameters
 override val arguments=arguments{
  require("family","Specify the native family.",description="Original reactive family.",type=ArgType.STRING)
  require("percent","Specify its tier percentage.",description="Configured native percentage.",type=ArgType.EXPRESSION)
 }
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData)=NativeReactiveProgression.execute(config.getString("family"),config.getDoubleFromExpression("percent",data),data)
}
/** Native pre-action and post-damage boundaries share OriginalRuntime's sessions/queue/engine. */
internal object NativeReactiveProgression : Listener {
 private lateinit var owner:EcoEnchantsPlugin
 private var enabled=false
 private val itemKey=NamespacedKey("ecoenchants","original_item_id")
 private data class Key(val actor:UUID,val family:String)
 private data class Arm(val source:NativeBurstProgression.Context,val family:String,val tier:Int,val percent:Double,val target:UUID?,var task:BukkitTask?=null)
 private val arms=ArmedProcLedger<Key,Arm>({cap()},entities={key,value->listOfNotNull(key.actor,value.target)}){now()}
 private data class Taken(val token:UUID,val world:UUID,var time:Long)
 private val taken=mutableMapOf<UUID,Taken>()
 internal data class Capture(val player:UUID,val token:UUID,val world:UUID,val guard:NativeBurstProgression.Context?,val attacker:LivingEntity?,val primary:Boolean)
 private data class Reservation(val source:NativeBurstProgression.Context,val family:OriginalProcEngine.Family,val lease:OriginalProcEngine.Lease?,val arm:ArmedProcLedger.Entry<Arm>?)
 private val pending=IdentityHashMap<EntityDamageEvent,MutableList<Reservation>>()
 private val pendingEntities=mutableMapOf<UUID,MutableSet<EntityDamageEvent>>()
 private data class Impulse(val source:NativeBurstProgression.Context,val family:OriginalProcEngine.Family,val lease:OriginalProcEngine.Lease,val event:EntityPushedByEntityAttackEvent,var task:BukkitTask?=null)
 private val impulses=mutableMapOf<UUID,Impulse>()
 private data class Move(val source:NativeBurstProgression.Context,val event:PlayerMoveEvent,var task:BukkitTask?=null)
 private val moves=mutableMapOf<UUID,Move>()
 private data class Proof(val source:NativeBurstProgression.Context,val family:String,val event:Event,val victim:LivingEntity,val phase:String,val damage:Double=0.0,val eventId:UUID?=null,var used:Boolean=false)
 private var proof:Proof?=null
 private fun now()=System.nanoTime()/1_000_000L
 private fun cap()=(owner.configYml.getIntOrNull("original-procs.max-active-effects")?:512).coerceIn(1,1024)
 private fun id(item:ItemStack?)=item?.itemMeta?.persistentDataContainer?.get(itemKey,PersistentDataType.STRING)?.let{runCatching{UUID.fromString(it)}.getOrNull()}
 private fun active(player:Player)=enabled&&player.isOnline&&!player.isDead&&!player.isInvulnerable&&!player.hasMetadata("NPC")
 private fun current(source:NativeBurstProgression.Context)=Bukkit.getPlayer(source.player)?.takeIf{active(it)&&it.world.uid==source.world&&OriginalRuntime.sameSession(it.uniqueId,source.token)}
 private fun equip(player:Player,slot:EquipmentSlot):ItemStack?=when(slot){EquipmentSlot.HEAD->player.inventory.helmet;EquipmentSlot.CHEST->player.inventory.chestplate;EquipmentSlot.LEGS->player.inventory.leggings;EquipmentSlot.FEET->player.inventory.boots;else->player.inventory.itemInMainHand}
 private fun slot(family:String)=when(family){"guardia_cristal"->EquipmentSlot.CHEST;"velo_lunar"->EquipmentSlot.HEAD;"vigilia"->EquipmentSlot.LEGS;"ancla","paso_ceniza"->EquipmentSlot.FEET;else->EquipmentSlot.HAND}
 private fun candidate(player:Player,family:String):Boolean {
  val item=equip(player,slot(family))?:return false;val enchant=EcoEnchants.getByID(family)?:return false
  return item.getEnchantmentLevel(enchant.enchantment) in 1..3&&enchant.targets.any{it.matches(item)}
 }
 private fun snapshot(player:Player,family:String):NativeBurstProgression.Context? {
  val item=equip(player,slot(family))?:return null;val enchant=EcoEnchants.getByID(family)?:return null
  if(item.getEnchantmentLevel(enchant.enchantment) !in 1..3)return null
  return NativeBurstProgression.itemContext(player,item,"protection",true,slot(family))?.takeIf{family in it.tiers}
 }
 fun register(){Triggers.register(NativeReactiveAction);Effects.register(EffectNativeReactive)}
 fun enable(plugin:EcoEnchantsPlugin){if(enabled)return;owner=plugin;enabled=true;Bukkit.getPluginManager().registerEvents(this,plugin);Bukkit.getOnlinePlayers().forEach{arrival(it)}}
 fun reset(){arms.clear().forEach{it.value.task?.cancel()};pending.clear();pendingEntities.clear();impulses.values.forEach{it.task?.cancel()};impulses.clear();moves.values.forEach{it.task?.cancel()};moves.clear();taken.clear();proof=null;if(enabled)Bukkit.getOnlinePlayers().forEach{arrival(it)}}
 fun close(){enabled=false;reset();HandlerList.unregisterAll(this)}
 fun arrival(player:Player){if(!enabled||!player.isOnline||player.hasMetadata("NPC")||taken.size>=cap()&&player.uniqueId !in taken)return;val token=OriginalRuntime.session(player)?:return;taken[player.uniqueId]=Taken(token,player.world.uid,now())}
 fun retire(uuid:UUID){
  taken.remove(uuid);moves.remove(uuid)?.task?.cancel();impulses.remove(uuid)?.task?.cancel()
  arms.retireEntity(uuid).forEach{it.value.task?.cancel()}
  for(event in pendingEntities.remove(uuid)?.toList()?:emptyList()){
   val entries=pending[event]?:continue
   unindex(event,entries)
   entries.removeIf{it.source.player==uuid||event.entity.uniqueId==uuid||it.arm?.value?.target==uuid}
   if(entries.isEmpty())pending.remove(event)else entries.forEach{index(event,it)}
  }
 }
 private fun attacker(event:EntityDamageEvent):LivingEntity? {
  val source=(event as? EntityDamageByEntityEvent)?.damager?:return null
  return when(source){is LivingEntity->source;is Projectile->source.shooter as? LivingEntity;else->null}
 }
 fun capture(event:EntityDamageEvent,victim:LivingEntity,primary:Boolean):Capture? {
  val player=victim as? Player?:return null;if(!active(player))return null
  val token=OriginalRuntime.session(player)?:return null
  val source=attacker(event)
  val guard=if(primary&&NativeCombatProgression.gameplay()&&NativeCombatRules.physical(event.cause)&&source!=null&&candidate(player,"guardia_cristal")&&OriginalRuntime.eligible(player,source))snapshot(player,"guardia_cristal")else null
  return Capture(player.uniqueId,token,player.world.uid,guard,source,primary)
 }
 fun confirm(capture:Capture,event:EntityDamageEvent,victim:LivingEntity,damage:Double,eventId:UUID){
  val player=victim as? Player?:return
  if(damage<=0||!active(player)||capture.player!=player.uniqueId||capture.world!=player.world.uid||!OriginalRuntime.sameSession(player.uniqueId,capture.token))return
  val last=taken[player.uniqueId]
  if(last!=null&&last.token==capture.token&&last.world==capture.world)last.time=now() else arrival(player)
  val source=capture.guard?:return;val enemy=capture.attacker?:return
  if(!capture.primary||!OriginalRuntime.eligible(player,enemy)||id(player.inventory.chestplate)!=source.itemId)return
  force(source,"guardia_cristal",event,enemy,"confirmed",damage,eventId)
 }
 fun canArm(source:NativeBurstProgression.Context,family:String)=current(source)!=null&&arms.canArm(Key(source.player,family))
 fun arm(source:NativeBurstProgression.Context,family:String,tier:Int,percent:Double,target:UUID?):Boolean {
  if(!canArm(source,family))return false
  val key=Key(source.player,family);removeArm(key)
  val state=arms.arm(key,Arm(source,family,tier,percent,target),3000)?:return false;schedule(key,state)
  return true
 }
 private fun schedule(key:Key,state:ArmedProcLedger.Entry<Arm>){state.value.task=Bukkit.getScheduler().runTaskLater(owner,Runnable{if(arms.peek(key)===state)removeArm(key)},maxOf(1L,(state.until-now()+49)/50))}
 private fun removeArm(key:Key){arms.retire(key)?.value?.task?.cancel()}
 private fun readyArm(player:Player,family:String):ArmedProcLedger.Entry<Arm>? {
  val key=Key(player.uniqueId,family);val state=arms.peek(key)?:return null
  if(current(state.value.source)==null){removeArm(key);return null}
  return state
 }
 private fun force(source:NativeBurstProgression.Context,family:String,event:Event,victim:LivingEntity,phase:String,damage:Double=0.0,eventId:UUID?=null):Boolean {
  val player=current(source)?:return false;val tier=source.tiers[family]?:return false;val enchant=EcoEnchants.getByID(family)?:return false
  val invocation=Proof(source,family,event,victim,phase,damage,eventId);val before=proof;proof=invocation
  try{NativeReactiveAction.dispatch(player.toDispatcher(),TriggerData(player=player,victim=victim,item=source.item.clone(),event=event,location=victim.location,value=damage),listOf(ItemProvidedHolder(enchant.getLevel(tier),source.item.clone())))}finally{proof=before}
  return invocation.used
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 fun beforeDamage(event:EntityDamageEvent){
  if(!enabled||!NativeCombatProgression.gameplay()||DamageMultiplierBypass.current!=null||event.damage<=0||!event.damage.isFinite())return
  val target=event.entity as? LivingEntity?:return
  if(event is EntityDamageByEntityEvent){
   val player=when(val src=event.damager){is Player->src;is Projectile->src.shooter as? Player;else->null}
   if(player!=null&&active(player)){
    if(event.damager===player&&event.cause==EntityDamageEvent.DamageCause.ENTITY_ATTACK){
     val eligibleArms=listOf("marca_celeste","eclipse").mapNotNull{family->readyArm(player,family)?.takeIf{state->(family!="marca_celeste"||state.value.target==target.uniqueId)&&(family!="eclipse"||id(player.inventory.itemInMainHand)==state.value.source.itemId)}}
     if(eligibleArms.isNotEmpty()&&OriginalRuntime.eligible(player,target))for(state in eligibleArms)force(state.value.source,state.value.family,event,target,"bonus")
    }else if(event.cause==EntityDamageEvent.DamageCause.PROJECTILE){
     val projectile=event.damager as? Projectile
     if(projectile!=null)NativeBurstProgression.projectileSource(projectile,player,"aguja_aurora")?.let{source->
      if(source.item.type==Material.CROSSBOW&&source.charged&&source.origin.world==target.world&&source.origin.distanceSquared(target.location)>225&&OriginalRuntime.eligible(player,target))force(source,"aguja_aurora",event,target,"bonus")
     }
    }
   }
  }
  val player=target as? Player?:return;if(!active(player))return
  val monster=attacker(event)
  if(monster!=null&&NativeCombatRules.physical(event.cause)){
   val families=listOf("velo_lunar","vigilia").filter{candidate(player,it)}
   val guard=readyArm(player,"guardia_cristal")?.takeIf{id(player.inventory.chestplate)==it.value.source.itemId}
   if((families.isNotEmpty()||guard!=null)&&OriginalRuntime.eligible(player,monster)){
    for(family in families)snapshot(player,family)?.let{force(it,family,event,monster,"guard")}
    guard?.let{force(it.value.source,"guardia_cristal",event,monster,"guard")}
   }
  }
  if(event.cause in setOf(EntityDamageEvent.DamageCause.FIRE,EntityDamageEvent.DamageCause.FIRE_TICK,EntityDamageEvent.DamageCause.LAVA,EntityDamageEvent.DamageCause.HOT_FLOOR)){
   val state=readyArm(player,"paso_ceniza")?:return
   if(id(player.inventory.boots)!=state.value.source.itemId)return
   val causing=event.damageSource.causingEntity
   if(causing!=null){val living=when(causing){is LivingEntity->causing;is Projectile->causing.shooter as? LivingEntity;else->null}?:return;if(!OriginalRuntime.eligible(player,living))return}
   force(state.value.source,"paso_ceniza",event,player,"fire")
  }
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 fun knockback(event:EntityPushedByEntityAttackEvent){
  if(!enabled||!NativeCombatProgression.gameplay()||DamageMultiplierBypass.current!=null||event.cause !in setOf(EntityKnockbackEvent.Cause.ENTITY_ATTACK,EntityKnockbackEvent.Cause.DAMAGE,EntityKnockbackEvent.Cause.SWEEP_ATTACK))return
  val player=event.entity as? Player?:return;if(!active(player)||!player.isOnGround||impulses.size>=cap())return
  val enemy=when(val entity=event.pushedBy){is LivingEntity->entity;is Projectile->entity.shooter as? LivingEntity;else->null}?:return
  if(!candidate(player,"ancla")||!OriginalRuntime.eligible(player,enemy))return
  val source=snapshot(player,"ancla")?:return;force(source,"ancla",event,enemy,"impulse")
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 fun move(event:PlayerMoveEvent){
  if(!enabled||!NativeCombatProgression.gameplay()||!active(event.player)||event.from.world!=event.to.world)return
  if(event.from.blockX==event.to.blockX&&event.from.blockY==event.to.blockY&&event.from.blockZ==event.to.blockZ)return
  if(!candidate(event.player,"paso_ceniza"))return
  if(event.from.block.type!=Material.LAVA||event.to.block.type==Material.LAVA||event.player.uniqueId in moves||moves.size>=cap())return
  val source=snapshot(event.player,"paso_ceniza")?:return
  val move=Move(source,event);moves[source.player]=move
  move.task=Bukkit.getScheduler().runTask(owner,Runnable{
   if(moves[source.player]!==move)return@Runnable
   moves.remove(source.player)
   val player=current(source)?:return@Runnable
   if(event.isCancelled||player.isInLava||id(player.inventory.boots)!=source.itemId)return@Runnable
   force(source,"paso_ceniza",event,player,"lava")
  })
 }
 private fun index(event:EntityDamageEvent,entry:Reservation){
  for(entity in listOfNotNull(entry.source.player,event.entity.uniqueId,entry.arm?.value?.target))pendingEntities.getOrPut(entity){java.util.Collections.newSetFromMap(IdentityHashMap())}.add(event)
 }
 private fun unindex(event:EntityDamageEvent,entries:List<Reservation>){
  for(entry in entries)for(entity in listOfNotNull(entry.source.player,event.entity.uniqueId,entry.arm?.value?.target))pendingEntities[entity]?.let{events->events.remove(event);if(events.isEmpty())pendingEntities.remove(entity)}
 }
 private fun reserveDamage(p:Proof,event:EntityDamageEvent,descriptor:OriginalProcEngine.Family,state:ArmedProcLedger.Entry<Arm>?,needsCooldown:Boolean):Boolean {
  if(pending.size>=1024&&event !in pending)return false
  val attempt=OriginalRuntime.track(event as? EntityDamageByEntityEvent?:return false)?:return false
  val existing=pending[event];if(existing!=null&&(existing.size>=8||existing.any{it.family==descriptor}))return false
  val lease=if(needsCooldown)OriginalRuntime.reserveNative(descriptor,p.source.player,attempt).orElse(null)?:return false else null
  if(state!=null){val key=Key(p.source.player,p.family);if(!arms.consume(key,state)){lease?.let{OriginalRuntime.rollbackNative(descriptor,p.source.player,it)};return false};state.value.task?.cancel()}
  val entry=Reservation(p.source,descriptor,lease,state)
  pending.getOrPut(event){mutableListOf()}.add(entry);index(event,entry);return true
 }
 fun execute(family:String,percent:Double,data:TriggerData):Boolean {
  val p=proof?:return false;val player=current(p.source)?:return false;val level=data.holder.holder as? EcoEnchantLevel?:return false
  if(p.used||p.family!=family||level.enchant.id!=family||level.level!=p.source.tiers[family]||data.event!==p.event||data.victim!==p.victim||data.player!==player||id(data.item)!=p.source.itemId||!percent.isFinite()||percent<=0)return false
  val descriptor=try{OriginalProcEngine.Family.valueOf(family.uppercase())}catch(ignored:IllegalArgumentException){return false}
  if(!ProcGuards.arrivalReady(descriptor,OriginalRuntime.age(player)))return false
  if(p.phase=="confirmed"&&family=="guardia_cristal"){
   if(!canArm(p.source,family))return false
   val activation=OriginalRuntime.recordNative(descriptor,OriginalProcEngine.Hit(p.eventId?:return false,player.uniqueId,p.source.itemId,player.uniqueId,p.source.world,level.level,p.damage,true,true,true,false,false,false,false,false,true,false,false,false)).orElse(null)?:return false
   p.used=arm(p.source,family,level.level,percent,player.uniqueId);return p.used
  }
  if(p.phase=="lava"&&family=="paso_ceniza"){
   if(!canArm(p.source,family))return false
   val lease=OriginalRuntime.reserveNative(descriptor,player.uniqueId,UUID.randomUUID()).orElse(null)?:return false
   p.used=arm(p.source,family,level.level,percent,player.uniqueId)
   if(!p.used)OriginalRuntime.rollbackNative(descriptor,player.uniqueId,lease)
   return p.used
  }
  if(p.phase=="impulse"&&family=="ancla"){
   val event=p.event as? EntityPushedByEntityAttackEvent?:return false
   if(percent>=100||event.isCancelled||!finiteNativeImpulse(event.knockback)||player.uniqueId in impulses||impulses.size>=cap())return false
   val lease=OriginalRuntime.reserveNative(descriptor,player.uniqueId,UUID.randomUUID()).orElse(null)?:return false
   event.knockback=event.knockback.multiply(1-percent/100)
   val record=Impulse(p.source,descriptor,lease,event);impulses[player.uniqueId]=record
   record.task=Bukkit.getScheduler().runTask(owner,Runnable{
    if(impulses[player.uniqueId]!==record)return@Runnable
    impulses.remove(player.uniqueId)
    if(event.isCancelled||!finiteNativeImpulse(event.knockback))OriginalRuntime.rollbackNative(descriptor,player.uniqueId,lease)
   })
   p.used=true;return true
  }
  val event=p.event as? EntityDamageEvent?:return false
  if(event.isCancelled||event.damage<=0||!event.damage.isFinite())return false
  if(p.phase=="bonus"){
   val state=if(family=="aguja_aurora")null else readyArm(player,family)?:return false
   if(!reserveDamage(p,event,descriptor,state,family=="aguja_aurora"))return false
   if(!AdditiveDamageGroups.offer("ecoenchants_normal",data,1+(state?.value?.percent?:percent)/100)){undoLast(event);return false}
   p.used=true;return true
  }
  if(percent>=100||id(equip(player,slot(family)))!=p.source.itemId)return false
  var applied=percent
  when(family){
   "velo_lunar"->{val max=player.getAttribute(Attribute.MAX_HEALTH)?.value?:return false;if(max<=0||player.health>=max*.4)return false;if(!reserveDamage(p,event,descriptor,null,true))return false}
   "vigilia"->{val last=taken[player.uniqueId]?:return false;if(last.token!=p.source.token||last.world!=p.source.world||now()-last.time<4000)return false;if(!reserveDamage(p,event,descriptor,null,true))return false}
   "guardia_cristal"->{val state=readyArm(player,family)?:return false;if(!reserveDamage(p,event,descriptor,state,false))return false;applied=state.value.percent}
   "paso_ceniza"->{if(p.phase!="fire")return false;val state=readyArm(player,family)?:return false;applied=state.value.percent}
   else->return false
  }
  event.damage*=1-applied/100;p.used=true;return true
 }
 private fun undoLast(event:EntityDamageEvent){
  val list=pending[event]?:return;unindex(event,list);val entry=list.removeLastOrNull()?:return
  rollback(entry);if(list.isEmpty())pending.remove(event)else list.forEach{index(event,it)}
 }
 private fun rollback(entry:Reservation){
  entry.lease?.let{OriginalRuntime.rollbackNative(entry.family,entry.source.player,it)}
  val state=entry.arm?:return;val key=Key(entry.source.player,state.value.family)
  if(arms.settle(key,state,current(state.value.source)!=null))schedule(key,state)
 }
 fun finish(event:EntityDamageEvent){
  val entries=pending.remove(event)?:return
  unindex(event,entries)
  val rejected=explicitlyRejectedDamage(event)
  for(entry in entries)if(rejected)rollback(entry)else entry.arm?.let{state->arms.settle(Key(entry.source.player,state.value.family),state,false)}
 }
}

/** Invalid/nonfinite outcome is uncertain, never permission to repeat a pre-applied bonus. */
internal fun explicitlyRejectedDamage(event:EntityDamageEvent)=event.isCancelled||(event.finalDamage.isFinite()&&event.finalDamage<=0)
internal fun finiteNativeImpulse(vector:org.bukkit.util.Vector)=vector.x.isFinite()&&vector.y.isFinite()&&vector.z.isFinite()&&vector.lengthSquared().isFinite()&&vector.lengthSquared()>0
