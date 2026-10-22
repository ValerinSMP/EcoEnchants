package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.entities.BoundedLivingQueries
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.ecoenchants.enchant.EcoEnchants
import com.willfp.ecoenchants.target.EnchantFinder
import com.willfp.ecoenchants.target.EnchantFinder.clearEnchantmentCache
import com.willfp.libreforge.*
import com.willfp.libreforge.conditions.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.impl.*
import com.willfp.libreforge.triggers.*
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.inventory.*
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

internal object ConfirmedNativeBurst : Trigger("confirmed_native_burst") {
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.ITEM,TriggerParameter.EVENT,TriggerParameter.VALUE,TriggerParameter.LOCATION)
}
internal object EffectNativeBurst : Effect<NoCompileData>("native_burst") {
 override val supportsDelay=false
 override val parameters=ConfirmedNativeBurst.parameters
 override val arguments=arguments{
  require("family","Specify the native family.",description="Original progression family.",type=ArgType.STRING)
  optional("percent",description="Tier percentage for percentage effects.",type=ArgType.EXPRESSION)
  optional("damage",description="Flat damage for carve and damage-over-time effects.",type=ArgType.EXPRESSION)
  optional("amount",description="Bounded damage-over-time pulse count (1..32).",type=ArgType.EXPRESSION)
  optional("visual_profile",description="Shared Mythic visual phase profile (cosmetic only).",type=ArgType.STRING)
  optional("interval",description="Damage-over-time interval in ticks (1..1200).",type=ArgType.EXPRESSION)
 }
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean {
  val key=if(config.has("damage"))"damage"else"percent"
  if(!config.has(key))return false
  return NativeBurstProgression.execute(config.getString("family"),config.getDoubleFromExpression(key,data),data,
   if(config.has("amount"))config.getIntFromExpression("amount",data)else 3,
   if(config.has("interval"))config.getIntFromExpression("interval",data)else 20,config.getStringOrNull("visual_profile"))
 }
}
internal object ConditionHuntersTrail : Condition<NoCompileData>("has_hunters_trail") {
 override val invalidatedBy=emptySet<HolderChange>()
 override fun isMet(dispatcher:Dispatcher<*>,config:Config,holder:ProvidedHolder,compileData:NoCompileData):Boolean {
  val player=dispatcher.get<Player>()?:return false
  val item=holder.provider as? ItemStack?:return false
  val level=holder.holder as? EcoEnchantLevel?:return false
  return level.enchant.id=="estela_cazador"&&NativeBurstProgression.activeTrail(player,item,level.level)
 }
}
/** Source snapshots and effects on the existing confirmed-damage boundary, not another damage queue. */
internal object NativeBurstProgression : Listener {
 private lateinit var owner:EcoEnchantsPlugin
 private var enabled=false
 private val itemKey=NamespacedKey("ecoenchants","original_item_id")
 private val familiesByKind=mapOf(
  "melee" to setOf("brasa_errante","cicatriz_ignea","ruptura_sonica","tormenta_encadenada","legado_valerin","eclipse","thunderbolt","combo","lifesteal","drain","carve","cleave","bleed"),
  "arrow" to setOf("rebote_prismatico","cometa","marca_celeste","aguja_aurora","wound","snipe"),
  "trident" to setOf("pulso_abisal","constelacion"),
  "ranged_kill" to setOf("estela_cazador"),
  "protection" to setOf("ancla","guardia_cristal","velo_lunar","vigilia","paso_ceniza")
 )
 internal data class Context(val player:UUID,val token:UUID,val world:UUID,val item:ItemStack,val itemId:UUID,val tiers:Map<String,Int>,val kind:String,val charged:Boolean,val submerged:Boolean,val origin:Location)
 internal data class Capture(val sources:List<Context>,val projectile:UUID?)
 private data class Shot(val sources:List<Context>) { var origin:Location?=null }
 private val shots=ShotLedger<Shot>(4096,30_000){now()}
 private data class Proof(val capture:Capture,val source:Context,val event:EntityDamageEvent,val victim:LivingEntity,val damage:Double,val eventId:UUID)
 private var proof:Proof?=null
 private data class Trail(val item:UUID,val tier:Int,val token:UUID,val world:UUID,val until:Long,var task:BukkitTask?=null,var visual:BukkitTask?=null,var emissions:Int=0)
 private val trails=mutableMapOf<UUID,Trail>()
 private data class Dot(val source:Context,val target:LivingEntity,val damage:Double,val family:String="cicatriz_ignea",var remaining:Int=3,val interval:Long=20,var task:BukkitTask?=null)
 private val dots=mutableMapOf<UUID,Dot>()
 private val dotEntities=mutableMapOf<UUID,MutableSet<UUID>>()
 private val queryBudget=SwarmQueryBudget()
 private data class Recent(val token:UUID,val world:UUID,val expires:java.util.ArrayDeque<Long> = java.util.ArrayDeque())
 private val recent=mutableMapOf<UUID,Recent>()
 private val legacy=setOf("lifesteal","drain")
 private val expandedTiers=legacy+setOf("thunderbolt","combo","carve","cleave","bleed","wound","snipe")
 private fun now()=System.nanoTime()/1_000_000L
 private fun limit()=(owner.configYml.getIntOrNull("original-procs.max-active-effects")?:512).coerceIn(1,1024)
 private fun id(item:ItemStack)=item.itemMeta?.persistentDataContainer?.get(itemKey,PersistentDataType.STRING)?.let{runCatching{UUID.fromString(it)}.getOrNull()}
 fun register(){Triggers.register(ConfirmedNativeBurst);Effects.register(EffectNativeBurst);Conditions.register(ConditionHuntersTrail)}
 fun enable(plugin:EcoEnchantsPlugin){if(enabled)return;owner=plugin;enabled=true;Bukkit.getPluginManager().registerEvents(this,plugin)}
 fun reset(){recent.clear();shots.clear();dots.keys.toList().forEach{removeDot(it)};dotEntities.clear();trails.keys.toList().forEach{removeTrail(it)};queryBudget.reset();proof=null}
 fun close(){reset();enabled=false;HandlerList.unregisterAll(this)}
 fun retire(uuid:UUID){recent.remove(uuid);shots.removeOwner(uuid);removeTrail(uuid);dotEntities[uuid]?.toList()?.forEach{removeDot(it)}}
 private fun refresh(player:Player){player.clearEnchantmentCache();val d=player.toDispatcher();d.forceRefreshHolders();d.flushHolders()}
 internal fun itemContext(player:Player,item:ItemStack,kind:String,charged:Boolean,slot:EquipmentSlot?,projectile:Trident?=null):Context? {
  val allowed=familiesByKind[kind]?:return null
  val tiers=EnchantFinder.find(item).filter{it.enchant.id in allowed&&it.level in 1..(if(it.enchant.id=="legado_valerin")1 else if(it.enchant.id in expandedTiers)it.enchant.maximumLevel.coerceIn(1,16) else 3)&&it.enchant.targets.any{target->target.matches(item)}}.associate{it.enchant.id to it.level}
  if(tiers.isEmpty())return null
  val token=OriginalRuntime.session(player)?:return null
  val frozen=NativeProcAdapters.capture(item,itemKey)?:return null
  if(frozen.stamped){
   when(slot){EquipmentSlot.HAND->player.inventory.setItemInMainHand(frozen.item);EquipmentSlot.OFF_HAND->player.inventory.setItemInOffHand(frozen.item);EquipmentSlot.FEET->player.inventory.boots=frozen.item;EquipmentSlot.HEAD->player.inventory.helmet=frozen.item;EquipmentSlot.CHEST->player.inventory.chestplate=frozen.item;EquipmentSlot.LEGS->player.inventory.leggings=frozen.item;else->if(projectile!=null)projectile.itemStack=frozen.item else return null}
   refresh(player)
  }
  return Context(player.uniqueId,token,player.world.uid,frozen.item,frozen.id,tiers,kind,charged,player.isInWater,player.location.clone())
 }
 private fun trailContext(player:Player):Context? {
  val boots=player.inventory.boots?:return null;val enchant=EcoEnchants.getByID("estela_cazador")?:return null
  if(boots.getEnchantmentLevel(enchant.enchantment) !in 1..3)return null
  return itemContext(player,boots,"ranged_kill",true,EquipmentSlot.FEET)
 }
 fun shoot(event:EntityShootBowEvent){
  if(!enabled)return
  val player=event.entity as? Player?:return
  val bow=event.bow?:return
  if(event.projectile !is AbstractArrow||bow.type !in setOf(Material.BOW,Material.CROSSBOW))return
  val live=if(event.hand==EquipmentSlot.HAND)player.inventory.itemInMainHand else player.inventory.itemInOffHand
  if(live!=bow)return
  val sources=mutableListOf<Context>()
  itemContext(player,live,"arrow",event.force>=.99f,event.hand)?.let{sources.add(it)}
  trailContext(player)?.let{sources.add(it)}
  if(sources.isNotEmpty()) {
   val shot=Shot(sources);shot.origin=event.projectile.location.clone()
   shots.remember(event.projectile.uniqueId,player.uniqueId,shot)
  }
 }
 internal fun projectileSource(projectile:Projectile,player:Player,family:String):Context? {
  if(projectile.shooter!==player)return null
  return shots.source(projectile.uniqueId)?.sources?.firstOrNull{family in it.tiers&&it.player==player.uniqueId&&it.world==player.world.uid&&OriginalRuntime.sameSession(it.player,it.token)}
 }
 /** Authenticated launch-to-target-location displacement at this real damage attempt, not player distance/path length. */
 internal fun projectileDistance(projectile:Projectile,player:Player,impact:Location):Double? {
  val source=projectileSource(projectile,player,"snipe")?:return null
  if(projectile.world.uid!=source.world)return null
  val origin=shots.source(projectile.uniqueId)?.origin?:return null
  if(origin.world?.uid!=source.world)return null
  return launchDisplacement(origin,impact)
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 fun launch(event:ProjectileLaunchEvent){
  if(!enabled)return
  val trident=event.entity as? Trident?:return;val player=trident.shooter as? Player?:return
  val sources=mutableListOf<Context>()
  itemContext(player,trident.itemStack,"trident",true,null,trident)?.let{sources.add(it)}
  trailContext(player)?.let{sources.add(it)}
  if(sources.isNotEmpty())shots.remember(trident.uniqueId,player.uniqueId,Shot(sources))
 }
 fun capture(event:EntityDamageEvent,victim:LivingEntity,primary:Boolean):Capture? {
  if(!enabled||!primary||!NativeCombatProgression.gameplay()||event !is EntityDamageByEntityEvent)return null
  val sources=when(val damager=event.damager){
   is Player->{if(event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK)return null;val item=damager.inventory.itemInMainHand;itemContext(damager,item,"melee",true,EquipmentSlot.HAND)?.let{listOf(it)}?:return null}
   is Projectile->{if(event.cause!=EntityDamageEvent.DamageCause.PROJECTILE)return null;val shot=shots.source(damager.uniqueId)?:return null;val shooter=damager.shooter as? Player?:return null;if(shot.sources.any{it.player!=shooter.uniqueId})return null;shot.sources.map{if(it.kind=="trident")it.copy(submerged=shooter.isInWater)else it}}
   else->return null
  }
  val player=Bukkit.getPlayer(sources.first().player)?:return null
  if(sources.none{source->source.tiers.keys.any{if(it in legacy||it=="combo")OriginalRuntime.modeAllows(it,player,victim)else OriginalRuntime.eligibleFor(it,player,victim)}})return null
  return Capture(sources,(event.damager as? Projectile)?.uniqueId)
 }
 fun confirm(capture:Capture,event:EntityDamageEvent,victim:LivingEntity,damage:Double,eventId:UUID){
  if(!enabled||damage<=0||capture.projectile!=null&&!shots.claim(capture.projectile,victim.uniqueId))return
  for(source in capture.sources){
   val player=current(source)?:continue
   for((family,tier)in source.tiers){
    if(family in legacy||family=="combo"){
     if(!OriginalRuntime.modeAllows(family,player,victim,allowDead=true)||source.kind=="melee"&&id(player.inventory.itemInMainHand)!=source.itemId)continue
    }else if(!OriginalRuntime.eligibleFor(family,player,victim,true))continue
    if(!accepts(family,source.kind,source.charged,source.submerged,victim.isDead||victim.health<=0))continue
    val enchant=EcoEnchants.getByID(family)?:continue
    val holder=ItemProvidedHolder(enchant.getLevel(tier),source.item.clone())
    val previous=proof;proof=Proof(capture,source,event,victim,damage,eventId)
    try{ConfirmedNativeBurst.dispatch(player.toDispatcher(),TriggerData(player=player,victim=victim,item=source.item.clone(),event=event,location=victim.location,value=damage),listOf(holder))}finally{proof=previous}
   }
  }
 }
 internal fun accepts(family:String,kind:String,charged:Boolean,submerged:Boolean,killed:Boolean):Boolean=when(family){
  "combo"->kind=="melee"&&killed
  "lifesteal","drain","carve","cleave","bleed","thunderbolt"->kind=="melee"
  "wound"->kind=="arrow"
  "brasa_errante"->kind=="melee"&&killed
  "estela_cazador"->kind=="ranged_kill"&&killed
  "pulso_abisal"->kind=="trident"&&submerged
  "constelacion"->kind=="trident"
  "rebote_prismatico","cometa"->kind=="arrow"&&charged
  "cicatriz_ignea","ruptura_sonica","tormenta_encadenada","legado_valerin","eclipse"->kind=="melee"
  "marca_celeste"->kind=="arrow"&&charged&&!killed
  else->false
 }
 private fun current(source:Context):Player?=Bukkit.getPlayer(source.player)?.takeIf{it.isOnline&&!it.isDead&&it.world.uid==source.world&&OriginalRuntime.sameSession(source.player,source.token)}
 fun execute(family:String,percent:Double,data:TriggerData)=execute(family,percent,data,3,20)
 fun execute(family:String,percent:Double,data:TriggerData,pulses:Int,interval:Int):Boolean = execute(family,percent,data,pulses,interval,null)
 fun execute(family:String,percent:Double,data:TriggerData,pulses:Int,interval:Int,visualProfile:String?):Boolean {
  val p=proof?:return false;val source=p.source;val tier=source.tiers[family]?:return false
  val level=data.holder.holder as? EcoEnchantLevel?:return false
  if(level.enchant.id!=family||level.level!=tier||data.event!==p.event||data.player?.uniqueId!=source.player||data.victim!==p.victim||data.item?.let{id(it)}!=source.itemId)return false
  val descriptor=try{OriginalProcEngine.Family.valueOf(family.uppercase())}catch(ignored:IllegalArgumentException){return false}
  if(!percent.isFinite()||percent<=0||!ProcGuards.arrivalReady(descriptor,OriginalRuntime.age(data.player!!)))return false
  if(family=="estela_cazador"&&(trails.size>=limit()&&source.player !in trails||id(data.player!!.inventory.boots?:return false)!=source.itemId))return false
  if(family in setOf("cicatriz_ignea","bleed","wound")&&(dots.size>=limit()||!p.victim.isValid||p.victim.isDead||pulses !in 1..32||interval !in 1..1200))return false
  if(family in setOf("marca_celeste","eclipse")&&!NativeReactiveProgression.canArm(source,family))return false
  val activation=OriginalRuntime.recordNative(descriptor,OriginalProcEngine.Hit(p.eventId,source.player,source.itemId,p.victim.uniqueId,source.world,tier,p.damage,true,true,true,false,false,false,false,false,true,false,false,false)).orElse(null)?:return false
  val player=current(source)?:return false
  val visual=visualProfile?.takeIf{owner.configYml.getBoolOrNull("original-procs.visuals.enabled")!=false}?.let { com.willfp.libreforge.visual.VisualPhases.open(it,com.willfp.libreforge.visual.VisualContext(
   UUID.randomUUID(),"EcoEnchants",family,tier,player.uniqueId,player.eyeLocation,player.eyeLocation.direction,1200,0,source.itemId,activation.radius())) }
  visual?.emit(com.willfp.libreforge.visual.VisualPhase.ACTIVATE,receiver=p.victim.uniqueId,location=p.victim.location)
  when(family){
   "combo"->{
    var state=recent[source.player]
    if(state==null||state.token!=source.token||state.world!=source.world){if(recent.size>=limit()&&source.player !in recent)return false;state=Recent(source.token,source.world);recent[source.player]=state}
    while(state.expires.isNotEmpty()&&state.expires.first<=now())state.expires.removeFirst()
    while(state.expires.size>=7)state.expires.removeFirst()
    state.expires.addLast(now()+(2+2*tier)*1000L)
   }
   "marca_celeste","eclipse"->NativeReactiveProgression.arm(source,family,tier,percent,if(family=="marca_celeste")p.victim.uniqueId else null)
   "estela_cazador"->trail(player,source,tier)
   "cicatriz_ignea","bleed","wound"->{val key=UUID.randomUUID();val dot=Dot(source,p.victim,if(family=="cicatriz_ignea")p.damage*percent/100 else percent,family,if(family=="cicatriz_ignea")3 else pulses,if(family=="cicatriz_ignea")20 else interval.toLong());dots[key]=dot;for(entity in listOf(source.player,p.victim.uniqueId))dotEntities.getOrPut(entity){mutableSetOf()}.add(key);scheduleDot(key,dot)}
   "cometa","legado_valerin"->damage(source,p.victim,p.damage*percent/100,true,family)
   "tormenta_encadenada"->{
    val visited=mutableSetOf(p.victim.uniqueId);var anchor=p.victim
    for(hop in 0 until 3){
     val next=targets(player,anchor,3.0,1,visited,family).firstOrNull()?:break
     visited.add(next.uniqueId);damage(source,next,p.damage*percent/100,family=family);anchor=next
    }
   }
   else->{targets(player,p.victim,activation.radius(),activation.maxTargets(),family=family).forEach{target ->
    damage(source,target,if(family=="carve")percent else p.damage*percent/100,family=family,visual=visual,
      onResolved=if(family=="brasa_errante" && visualProfile==null)({ receiver -> OriginalRuntime.procVisualProfile(player,p.victim.location,"brasa_errante","link",listOf(receiver),p.eventId,activation.radius()) }) else null)
   }}
  }
  if(visualProfile==null && family !in setOf("brasa_errante","combo","cicatriz_ignea","bleed","wound","estela_cazador","marca_celeste","eclipse"))OriginalRuntime.procVisual(player,p.victim.location,if(family=="legado_valerin")16 else 12)
  return true
 }
 internal fun offerCombo(data:TriggerData):Boolean {
  val player=data.player?:return false;val victim=data.victim?:return false
  val level=data.holder.holder as? EcoEnchantLevel?:return false
  val event=data.event as? EntityDamageByEntityEvent?:return false
  if(level.enchant.id!="combo"||level.level !in 1..5||event.damager!==player||event.entity!==victim||event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK||!NativeCombatProgression.gameplay()||DamageMultiplierBypass.current!=null||!OriginalRuntime.modeAllows("combo",player,victim))return false
  val state=recent[player.uniqueId]?:return false
  if(state.token!=OriginalRuntime.session(player)||state.world!=player.world.uid){recent.remove(player.uniqueId);return false}
  // Different tiers give different deadlines: remove expired entries throughout this seven-entry bound.
  state.expires.removeIf{it<=now()}
  if(state.expires.isEmpty()){recent.remove(player.uniqueId);return false}
  return AdditiveDamageGroups.offer("ecoenchants_normal",data,1+minOf(state.expires.size,2+level.level)*level.level/100.0)
 }
 private fun damage(source:Context,target:LivingEntity,amount:Double,fresh:Boolean=false,family:String,onResolved:((LivingEntity)->Unit)?=null,visual:com.willfp.libreforge.visual.VisualSession?=null){
  val player=current(source)?:return
  if(!amount.isFinite()||amount<=0||!OriginalRuntime.eligibleFor(family,player,target)||!fresh&&target.noDamageTicks>target.maximumNoDamageTicks/2)return
  val before=if(onResolved!=null || visual!=null)target.health else 0.0;val previous=if(onResolved!=null || visual!=null)target.lastDamageCause else null
  OriginalRuntime.withSecondaryDamage{RepeatedDamage.run(player.uniqueId,player.uniqueId,target.uniqueId){
   if(fresh)freshDamageWindow(target){target.dealDamage(amount,source=player,trueDamage=false,checkAntigrief=true)}
   else target.dealDamage(amount,source=player,trueDamage=false,checkAntigrief=true)
  }}
  if((onResolved!=null || visual!=null) && NativeProcAdapters.resolvedBy(player,target,before,previous)) {
   onResolved?.invoke(target)
   val event=target.lastDamageCause
   if(event!=null)visual?.emit(com.willfp.libreforge.visual.VisualPhase.DAMAGE_CONFIRMED,receiver=target.uniqueId,
    location=target.location,confirmedDamage=NativeProcAdapters.confirmed(event,target,before))
  }
 }
 internal fun targets(player:Player,anchor:LivingEntity,radius:Double,maximum:Int,excluded:Set<UUID> = emptySet()):List<LivingEntity> = targets(player,anchor,radius,maximum,excluded,null)
 internal fun targets(player:Player,anchor:LivingEntity,radius:Double,maximum:Int,excluded:Set<UUID> = emptySet(),family:String?):List<LivingEntity>{
  val perTick=(owner.configYml.getIntOrNull("original-procs.queries-per-tick")?:8).coerceIn(0,32)
  if(!queryBudget.take(Bukkit.getCurrentTick(),perTick))return emptyList()
  val location=anchor.location;val scan=(owner.configYml.getIntOrNull("original-procs.max-candidate-scan")?:32).coerceIn(1,64)
  val result=BoundedLivingQueries.nearby(location,radius,scan)
  if(result.availability()!=BoundedLivingQueries.Availability.AVAILABLE)return emptyList()
  return result.candidates().filter{it!==anchor&&it.uniqueId !in excluded&&it.location.distanceSquared(location)<=radius*radius&&it.noDamageTicks<=it.maximumNoDamageTicks/2&&OriginalRuntime.eligibleFor(family,player,it)&&anchor.hasLineOfSight(it)}.sortedBy{it.location.distanceSquared(location)}.take(maximum)
 }
 private fun removeDot(key:UUID){
  val dot=dots.remove(key)?:return;dot.task?.cancel()
  for(entity in listOf(dot.source.player,dot.target.uniqueId))dotEntities[entity]?.let{ids->ids.remove(key);if(ids.isEmpty())dotEntities.remove(entity)}
 }
 private fun scheduleDot(key:UUID,dot:Dot){
  dot.task=Bukkit.getScheduler().runTaskLater(owner,Runnable{
   if(dots[key]!==dot)return@Runnable
   val player=current(dot.source)
   if(player==null||!OriginalRuntime.eligibleFor(dot.family,player,dot.target)){removeDot(key);return@Runnable}
   damage(dot.source,dot.target,dot.damage,true,dot.family);OriginalRuntime.procVisual(player,dot.target.location,3);if(dots[key]!==dot)return@Runnable;dot.remaining--
   if(dot.remaining>0)scheduleDot(key,dot)else removeDot(key)
  },dot.interval)
 }
 fun activeTrail(player:Player,item:ItemStack,tier:Int):Boolean { return trails[player.uniqueId]?.let{it.tier==tier&&it.until>now()&&it.token==OriginalRuntime.session(player)&&it.world==player.world.uid&&id(item)==it.item&&id(player.inventory.boots?:return false)==it.item}?:false }
 private fun trail(player:Player,source:Context,tier:Int){
  removeTrail(player.uniqueId);val state=Trail(source.itemId,tier,source.token,source.world,now()+3000);trails[player.uniqueId]=state;refresh(player);trailVisual(player.uniqueId,state)
  state.task=Bukkit.getScheduler().runTaskLater(owner,Runnable{if(trails[player.uniqueId]===state)removeTrail(player.uniqueId)},60L)
 }
 private fun trailVisual(uuid:UUID,state:Trail){
  if(trails[uuid]!==state)return
  val player=Bukkit.getPlayer(uuid)?:return
  if(!activeTrail(player,player.inventory.boots?:return,state.tier))return
  OriginalRuntime.procVisual(player,player.location,6);state.emissions++
  if(state.emissions<3)state.visual=Bukkit.getScheduler().runTaskLater(owner,Runnable{trailVisual(uuid,state)},10L)
 }
 private fun removeTrail(uuid:UUID){val state=trails.remove(uuid)?:return;state.task?.cancel();state.visual?.cancel();Bukkit.getPlayer(uuid)?.takeIf{it.isOnline}?.let{refresh(it)}}
}

/** Only a proc already earned by confirmed damage may request this native extra-hit window. */
internal fun <T> freshDamageWindow(target:LivingEntity,body:()->T):T {
 val previous=target.noDamageTicks;target.noDamageTicks=0
 try{return body()}finally{target.noDamageTicks=maxOf(previous,target.noDamageTicks)}
}

/** Straight 3D displacement from the frozen projectile launch point to the victim location at impact. */
internal fun launchDisplacement(origin:Location?,impact:Location):Double? {
 val world=origin?.world?.uid?:return null
 if(impact.world?.uid!=world)return null
 val coordinates=listOf(origin.x,origin.y,origin.z,impact.x,impact.y,impact.z)
 if(coordinates.any{!it.isFinite()})return null
 return StrictMath.hypot(StrictMath.hypot(impact.x-origin.x,impact.y-origin.y),impact.z-origin.z)
  .takeIf{it.isFinite()}
}
