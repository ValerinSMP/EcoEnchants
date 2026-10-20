package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchants
import com.willfp.ecoenchants.target.EnchantFinder.clearEnchantmentCache
import com.willfp.libreforge.*
import com.willfp.libreforge.conditions.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.triggers.*
import org.bukkit.*
import org.bukkit.attribute.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.*
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

internal object ConfirmedNativeCombat : Trigger("confirmed_native_combat") {
 override val parameters=setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.ITEM,TriggerParameter.EVENT,TriggerParameter.VALUE)
}
internal class NativeCombatEffect(private val family:String) : Effect<NoCompileData>(family) {
 override val parameters=ConfirmedNativeCombat.parameters
 override val supportsDelay=false
 override val arguments=arguments { require("percent","Specify the tier percentage.",description="Native tier percentage.",type=ArgType.EXPRESSION) }
 override fun onTrigger(config:Config,data:TriggerData,compileData:NoCompileData):Boolean =
  NativeCombatProgression.execute(family,config.getDoubleFromExpression("percent",data),data)
}
internal object ConditionRested : Condition<NoCompileData>("out_of_confirmed_combat") {
 override val invalidatedBy=emptySet<HolderChange>()
 override fun isMet(dispatcher:Dispatcher<*>,config:Config,holder:ProvidedHolder,compileData:NoCompileData):Boolean =
  dispatcher.get<Player>()?.let{NativeCombatProgression.rested(it)}?:false
}
/** Native hooks share OriginalRuntime's single bounded damage-confirmation queue and session identity. */
internal object NativeCombatProgression : Listener {
 private lateinit var owner:EcoEnchantsPlugin
 private var enabled=false
 private var commandGuard:CommandDamageGuard?=null
 private val itemKey=NamespacedKey("ecoenchants","original_item_id")
 private val armorKey=NamespacedKey("ecoenchants","letalidad")
 data class Capture(val actor:UUID?,val actorToken:UUID?,val victimToken:UUID?,val world:UUID,
  val weapon:ItemStack?,val chest:ItemStack?,val maximum:Double,val primary:Boolean)
 private data class Proof(val capture:Capture,val event:EntityDamageEvent,val victim:LivingEntity,val before:Double,val damage:Double)
 private var proof:Proof?=null
 private data class Quiet(var since:Long,var task:BukkitTask?=null)
 private val combat=mutableMapOf<UUID,Quiet>()
 private data class Buff(val item:UUID,val tier:Int,val until:Long)
 private val buffs=mutableMapOf<UUID,Buff>()
 private val buffChecks=mutableMapOf<UUID,Buff>()
 private val cooldowns=LastResortGate({cap()}){now()}
 private data class Armor(val target:LivingEntity,val stacks:MutableMap<Pair<UUID,Int>,ArmorStack> = mutableMapOf(),var modifier:AttributeModifier?=null,var task:BukkitTask?=null)
 private val armors=mutableMapOf<UUID,Armor>()
 private fun now()=System.nanoTime()/1_000_000L
 private fun cap()=(owner.configYml.getIntOrNull("native-combat.max-players")?:512).coerceIn(1,1024)
 private fun id(item:ItemStack?):UUID?=item?.itemMeta?.persistentDataContainer?.get(itemKey,PersistentDataType.STRING)?.let{runCatching{UUID.fromString(it)}.getOrNull()}
 private fun tier(item:ItemStack?,family:String)=if(item==null)0 else EcoEnchants.getByID(family)?.let{item.getEnchantmentLevel(it.enchantment)}?:0
 private fun refresh(player:Player){val d=player.toDispatcher();d.updateEffects();d.flushHolders()}
 private fun snapshot(player:Player,item:ItemStack?,family:String,chest:Boolean):ItemStack? {
  if(item==null||tier(item,family) !in 1..5)return null
  if(chest&&!item.type.name.endsWith("_CHESTPLATE")||!chest&&!item.type.name.endsWith("_SWORD"))return null
  val frozen=NativeProcAdapters.capture(item,itemKey)?:return null
  if(frozen.stamped){
   val live=if(chest)player.inventory.chestplate else player.inventory.itemInMainHand
   if(live!=item)return null
   if(chest)player.inventory.chestplate=frozen.item else player.inventory.setItemInMainHand(frozen.item)
   player.clearEnchantmentCache();val d=player.toDispatcher();d.forceRefreshHolders();d.flushHolders()
  }
  return frozen.item
 }
 fun register(){Triggers.register(ConfirmedNativeCombat);Effects.register(NativeCombatEffect("lethality_stack"));Effects.register(NativeCombatEffect("last_resort"));Conditions.register(ConditionRested)}
 fun enable(plugin:EcoEnchantsPlugin){if(enabled)return;owner=plugin;enabled=true
  commandGuard=try{CommandDamageGuard.resolve()}catch(problem:ReflectiveOperationException){owner.logger.log(java.util.logging.Level.SEVERE,"Letalidad/Último recurso/ecoenchants_normal disabled: native command damage guard unavailable",problem);null}catch(problem:RuntimeException){owner.logger.log(java.util.logging.Level.SEVERE,"Letalidad/Último recurso/ecoenchants_normal disabled: native command damage guard unavailable",problem);null}catch(problem:LinkageError){owner.logger.log(java.util.logging.Level.SEVERE,"Letalidad/Último recurso/ecoenchants_normal disabled: native command damage guard unavailable",problem);null}
  Bukkit.getPluginManager().registerEvents(this,plugin);Bukkit.getOnlinePlayers().forEach{arrival(it)}}
 fun reset(){combat.values.forEach{it.task?.cancel()};combat.clear();buffs.clear();buffChecks.clear();armors.keys.toList().forEach{removeArmor(it)};proof=null;cooldowns.prune();if(enabled)Bukkit.getOnlinePlayers().forEach{arrival(it)}}
 fun close(){enabled=false;reset();HandlerList.unregisterAll(this)}
 fun gameplay()=enabled&&commandGuard?.gameplay()==true
 fun rested(player:Player):Boolean {
  if(!enabled||OriginalRuntime.age(player)<10_000)return false
  val state=combat[player.uniqueId]?:return false // Missing is unknown, never authorize.
  return now()-state.since>=10_000
 }
 fun arrival(player:Player){if(!enabled)return;if(combat.size>=cap()&&player.uniqueId !in combat)return;OriginalRuntime.session(player);quiet(player,now())}
 private fun quiet(player:Player,time:Long){
  val old=combat[player.uniqueId]
  if(old==null){if(combat.size>=cap())return;val q=Quiet(time);combat[player.uniqueId]=q;scheduleQuiet(player.uniqueId,q)}
  else {val was=now()-old.since>=10_000;old.since=time;if(was){refresh(player);if(old.task==null)scheduleQuiet(player.uniqueId,old)}}
 }
 private fun scheduleQuiet(uuid:UUID,state:Quiet){
  state.task=Bukkit.getScheduler().runTaskLater(owner,Runnable{
   if(combat[uuid]!==state)return@Runnable
   state.task=null
   val player=Bukkit.getPlayer(uuid)?:return@Runnable
   if(now()-state.since<10_000)scheduleQuiet(uuid,state) else refresh(player)
  },maxOf(1L,(10_000-(now()-state.since)+49)/50))
 }
 fun capture(event:EntityDamageEvent,victim:LivingEntity,primary:Boolean):Capture? {
  if(!enabled||event !is EntityDamageByEntityEvent||!NativeCombatRules.physical(event.cause))return null
  val damager=when(val source=event.damager){is LivingEntity->source;is Projectile->source.shooter as? LivingEntity;else->null}?:return null
  val actor=damager as? Player;val receiver=victim as? Player
  if(actor==null&&receiver==null||victim.isInvulnerable||damager.isDead)return null
  if(actor!=null&&!AntigriefManager.canInjure(actor,victim))return null
  if(actor!=null&&(!actor.isOnline||actor.isDead)||receiver!=null&&(!receiver.isOnline||receiver.isDead))return null
  val aToken=actor?.let{OriginalRuntime.session(it)?:return null};val vToken=receiver?.let{OriginalRuntime.session(it)?:return null}
  val sword=if(primary&&commandGuard?.gameplay()==true&&actor!=null&&event.damager===actor&&event.cause==EntityDamageEvent.DamageCause.ENTITY_ATTACK&&OriginalRuntime.eligible(actor,victim))snapshot(actor,actor.inventory.itemInMainHand,"letalidad",false) else null
  val chest=if(primary&&receiver!=null&&commandGuard?.gameplay()==true)snapshot(receiver,receiver.inventory.chestplate,"ultimo_recurso",true) else null
  return Capture(actor?.uniqueId,aToken,vToken,victim.world.uid,sword,chest,victim.getAttribute(Attribute.MAX_HEALTH)?.value?:0.0,primary)
 }
 fun uncertain(c:Capture,victim:LivingEntity){
  // Queue saturation cannot leave an out-of-combat authorization based on stale evidence.
  sequenceOf(c.actor,(victim as? Player)?.uniqueId).filterNotNull().distinct().forEach{uuid->
   combat.remove(uuid)?.task?.cancel();Bukkit.getPlayer(uuid)?.let{refresh(it)}
  }
 }
 fun confirm(c:Capture,event:EntityDamageEvent,victim:LivingEntity,before:Double,damage:Double){
  if(!enabled||victim.world.uid!=c.world||damage<=0)return
  val actor=c.actor?.let{Bukkit.getPlayer(it)}
  if(c.actor!=null&&(actor==null||c.actorToken==null||!OriginalRuntime.sameSession(c.actor,c.actorToken)||!actor.isOnline||actor.world.uid!=c.world))return
  val receiver=victim as? Player
  if(receiver!=null&&(c.victimToken==null||!OriginalRuntime.sameSession(receiver.uniqueId,c.victimToken)||!receiver.isOnline))return
  actor?.takeIf{!it.isDead}?.let{quiet(it,now())};receiver?.takeIf{!it.isDead}?.let{quiet(it,now())}
  val old=proof;proof=Proof(c,event,victim,before,damage)
  try {
   if(actor!=null&&c.weapon!=null&&id(actor.inventory.itemInMainHand)==id(c.weapon)&&OriginalRuntime.eligible(actor,victim,true))dispatch(actor,victim,c.weapon,"letalidad",event,damage)
   if(receiver!=null&&c.chest!=null&&c.primary&&!receiver.isDead&&receiver.health>0&&receiver.getAttribute(Attribute.MAX_HEALTH)?.value==c.maximum&&
    NativeCombatRules.crossed(before,receiver.health,c.maximum)&&id(receiver.inventory.chestplate)==id(c.chest))dispatch(receiver,receiver,c.chest,"ultimo_recurso",event,damage)
  } finally {proof=old}
 }
 private fun dispatch(player:Player,victim:LivingEntity,item:ItemStack,family:String,event:EntityDamageEvent,damage:Double){
  val enchant=EcoEnchants.getByID(family)?:return;val t=tier(item,family);if(t !in 1..5)return
  val holder=ItemProvidedHolder(enchant.getLevel(t),item.clone())
  ConfirmedNativeCombat.dispatch(player.toDispatcher(),TriggerData(player=player,victim=victim,item=item.clone(),event=event,value=damage),listOf(holder))
 }
 fun execute(family:String,percent:Double,data:TriggerData):Boolean {
  val p=proof?:return false;val player=data.player?:return false;val supplied=data.item?:return false
  if(data.event!==p.event||data.victim?.uniqueId!=p.victim.uniqueId||!percent.isFinite())return false
  if(family=="lethality_stack"){
   val item=p.capture.weapon?:return false;val t=tier(item,"letalidad")
   if(player.uniqueId!=p.capture.actor||id(supplied)!=id(item)||t !in 1..5||percent!=t.toDouble()||!OriginalRuntime.eligible(player,p.victim))return false
   val attribute=p.victim.getAttribute(Attribute.ARMOR)?:return false;if(attribute.value<=0)return false
   var armor=armors[p.victim.uniqueId]
   if(armor==null){if(armors.size>=cap())return false;armor=Armor(p.victim);armors[p.victim.uniqueId]=armor}
   val key=player.uniqueId to t
   if(key !in armor.stacks&&armor.stacks.size>=128)return false
   armor.stacks[key]=(armor.stacks[key]?:ArmorStack(t)).also{it.hit(now())};applyArmor(armor);return true
  }
  if(family=="last_resort"){
   if(commandGuard?.gameplay()!=true)return false
   val item=p.capture.chest?:return false;val t=tier(item,"ultimo_recurso");val uuid=player.uniqueId
   if(p.victim!==player||id(supplied)!=id(item)||t !in 1..5||percent!=3.0*t||
    !NativeCombatRules.crossed(p.before,player.health,p.capture.maximum)||!p.capture.primary)return false
   val identity=id(item)?:return false
   if(!cooldowns.claim(uuid,OriginalRuntime.age(player)))return false
   buffs[uuid]=Buff(identity,t,now()+5_000);return true
  }
  return false
 }
 private fun applyArmor(armor:Armor){
  val time=now();armor.stacks.entries.removeIf{!it.value.live(time)}
  if(armor.stacks.isEmpty()||!armor.target.isValid||armor.target.isDead){removeArmor(armor.target.uniqueId);return}
  val attr=armor.target.getAttribute(Attribute.ARMOR)?:run{removeArmor(armor.target.uniqueId);return}
  armor.modifier?.let{attr.removeModifier(it)}
  val fraction=armor.stacks.values.maxOf{it.fraction()}
  armor.modifier=NativeProcAdapters.temporaryReduction(attr,armorKey,fraction)
  if(armor.task==null)armorExpiry(armor)
 }
 private fun armorExpiry(armor:Armor){
  val delay=maxOf(1L,(armor.stacks.values.minOf{it.until}-now()+49)/50)
  armor.task=Bukkit.getScheduler().runTaskLater(owner,Runnable{
   if(armors[armor.target.uniqueId]!==armor)return@Runnable
   armor.task=null;applyArmor(armor)
  },delay)
 }
 private fun removeArmor(uuid:UUID){val armor=armors.remove(uuid)?:return;armor.task?.cancel();armor.modifier?.let{armor.target.getAttribute(Attribute.ARMOR)?.removeModifier(it)}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 fun reduce(event:EntityDamageByEntityEvent){
  val player=event.entity as? Player?:return;val buff=buffs[player.uniqueId]?:return
  val item=player.inventory.chestplate
  if(now()>=buff.until||player.isDead||player.isInvulnerable||id(item)!=buff.item||tier(item,"ultimo_recurso")!=buff.tier){buffs.remove(player.uniqueId);return}
  if(commandGuard?.gameplay()!=true||!NativeCombatRules.physical(event.cause)||com.willfp.libreforge.effects.impl.DamageMultiplierBypass.current!=null)return
  val source=when(val entity=event.damager){is LivingEntity->entity;is Projectile->entity.shooter as? LivingEntity;else->null}?:return
  if(source is Player&&!AntigriefManager.canInjure(source,player))return
  val enchant=EcoEnchants.getByID("ultimo_recurso")?:return;val holder=ItemProvidedHolder(enchant.getLevel(buff.tier),item!!)
  if(!holder.holder.conditions.areMet(player.toDispatcher(),holder)){buffs.remove(player.uniqueId);return}
  if(event.damage.isFinite()&&event.damage>0)event.damage*=1-0.03*buff.tier
 }
 fun retire(uuid:UUID){
  combat.remove(uuid)?.task?.cancel();buffs.remove(uuid);buffChecks.remove(uuid)
  armors.values.toList().forEach{armor->if(armor.target.uniqueId==uuid)removeArmor(uuid) else if(armor.stacks.keys.removeIf{it.first==uuid})applyArmor(armor)}
 }
 private fun checkBuff(player:Player){
  val uuid=player.uniqueId;val buff=buffs[uuid]?:return
  if(buffChecks[uuid]===buff)return
  buffChecks[uuid]=buff
  Bukkit.getScheduler().runTask(owner,Runnable{
   if(buffChecks[uuid]!==buff)return@Runnable
   buffChecks.remove(uuid)
   if(buffs[uuid]===buff&&(id(player.inventory.chestplate)!=buff.item||tier(player.inventory.chestplate,"ultimo_recurso")!=buff.tier))buffs.remove(uuid)
  })
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun inventory(event:InventoryClickEvent){(event.whoClicked as? Player)?.let{checkBuff(it)}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) fun drag(event:InventoryDragEvent){(event.whoClicked as? Player)?.let{checkBuff(it)}}
}
