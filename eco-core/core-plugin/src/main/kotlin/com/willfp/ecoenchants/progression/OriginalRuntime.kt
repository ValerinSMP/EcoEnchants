package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.EcoEnchants
import com.willfp.libreforge.ItemProvidedHolder
import com.willfp.libreforge.dealDamage
import com.willfp.libreforge.effects.impl.DamageMultiplierBypass
import com.willfp.libreforge.effects.impl.DamageOrigin
import com.willfp.libreforge.effects.impl.DamageProvenance
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.event.TriggerDispatchEvent
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.IdentityHashMap
import java.util.UUID
import kotlin.math.*

/** All world access stays on MAIN. No polling, profile cache, synthetic damage event or XP award. */
internal object OriginalRuntime : Listener {
    private lateinit var owner: EcoEnchantsPlugin
    private val itemKey = NamespacedKey("ecoenchants","original_item_id")
    private val slowKey = NamespacedKey("ecoenchants","glacial_seal")
    private val engine = OriginalProcEngine(4096) { now() }
    private val budget = ParticleBudget { now() }
    private val diagnostic = OriginalHitDiagnostic { now() }
    private var diagnosticExpiry: BukkitTask? = null
    internal fun startDiagnostic(player: UUID) {
        stopDiagnostic(); diagnostic.start(player) { owner.logger.info(it) }
        diagnosticExpiry=Bukkit.getScheduler().runTaskLater(owner,Runnable { diagnostic.clear(); diagnosticExpiry=null },600L)
    }
    internal fun stopDiagnostic() { diagnosticExpiry?.cancel(); diagnosticExpiry=null; diagnostic.clear() }
    internal fun diagnostic(data: TriggerData, stage: String, detail: () -> String) { data.event?.let { diagnostic.note(it,stage,detail) } }
    @EventHandler(priority=EventPriority.MONITOR)
    fun diagnosticDispatch(event: TriggerDispatchEvent) {
        if(event.trigger.trigger !== ConfirmedOriginalHit)return
        diagnostic(event.trigger.data,"native_dispatch") { "cancelled=${event.isCancelled}" }
    }
    private data class Session(val token: UUID,val arrived: Long)
    private val sessions = mutableMapOf<UUID,Session>()
    private data class Source(val player: UUID,val world: UUID,val token: UUID,val family: OriginalProcEngine.Family,
                              val itemId: UUID,val item: ItemStack,val tier: Int)
    private var shots = ShotLedger<Source>(4096,30_000) { now() }
    private data class Pending(val event: EntityDamageEvent,val source: Source?,val combat: NativeCombatProgression.Capture?,val victim: LivingEntity,
                               val before: Double,val eventId: UUID,val arrow: UUID?,val burst:NativeBurstProgression.Capture?=null,val reactive:NativeReactiveProgression.Capture?=null,var task: BukkitTask? = null)
    private val pending = IdentityHashMap<EntityDamageEvent,Pending>()
    private val pendingEntities=mutableMapOf<UUID,MutableSet<EntityDamageEvent>>()
    private data class Proof(val pending: Pending,val damage: Double)
    private var proof: Proof? = null
    private var secondary = 0
    private var registered = false
    private data class Slow(val source: UUID,val entity: LivingEntity,val modifier: AttributeModifier,var task: BukkitTask? = null)
    private val slows = mutableMapOf<UUID,Slow>()
    private val slowsBySource=mutableMapOf<UUID,MutableSet<UUID>>()
    private var visualProfiles=emptyMap<String,VfxProfiles.Profile>()
    private data class VisualScene(val id:UUID,val profile:String,val phase:VfxProfiles.Phase,val actor:UUID,val token:UUID,
        val origin:Location,val receivers:List<LivingEntity>,val observers:List<UUID>,val radius:Double,val progress:Int,
        val sourceEvent:UUID,val phaseKey:String,var age:Int=0,val sent:MutableMap<UUID,Int> = mutableMapOf(),val sounded:MutableSet<UUID> = mutableSetOf())
    private val visualScenes=linkedMapOf<UUID,VisualScene>()
    private val visualEntities=mutableMapOf<UUID,MutableSet<UUID>>()
    private var visualTask:BukkitTask?=null
    private val visualFailures=mutableSetOf<String>()
    private fun prepareVisuals(){
        visualTask?.cancel();visualTask=null;visualScenes.clear();visualEntities.clear();visualFailures.clear()
        visualProfiles=if(config().getBoolOrNull("original-procs.visuals.enabled")==false)emptyMap() else
            VfxProfiles.compile(config().getSubsection("original-procs.visuals.profiles"),{ sound ->
                NamespacedKey.fromString(sound)?.let { org.bukkit.Registry.SOUNDS.get(it)!=null }?:false
            },{owner.logger.warning(it)})
    }
    internal fun procVisualProfile(player:Player,origin:Location,key:String,phase:String,receivers:List<LivingEntity>,
                                   sourceEvent:UUID,radius:Double,progress:Int=0){
        // This is cosmetic only; never let validation/emission failures change a resolved effect.
        if(!Bukkit.isPrimaryThread())return
        proof?.pending?.event?.let { diagnostic.note(it,"visual_entry") { "profile=$key phase=$phase prepared=${visualProfiles[key]?.phases?.containsKey(phase)==true} receivers=${receivers.size}" } }
        try{visual(player,origin,key,phase,receivers,sourceEvent,radius,progress)}
        catch(problem:RuntimeException){visualFailure(key,problem)}catch(problem:LinkageError){visualFailure(key,problem)}
    }
    private fun visualFailure(key:String,problem:Throwable){
        if(visualFailures.size<16&&visualFailures.add(key))owner.logger.log(java.util.logging.Level.WARNING,"Native visual $key suppressed",problem)
    }
    private fun visual(player:Player,origin:Location,key:String,phase:String,receivers:List<LivingEntity>,event:UUID,radius:Double,progress:Int){
        val prepared=visualProfiles[key]?.phases?.get(phase)?:return
        if(!radius.isFinite()||radius<0||!player.isOnline||player.isDead||player.world!=origin.world||receivers.isEmpty())return
        if(visualScenes.size>=limit("visuals.max-scenes",32,64)||
            (visualEntities[player.uniqueId]?.count{visualScenes[it]?.actor==player.uniqueId}?:0)>=limit("visuals.per-source-scenes",4,8))return
        if(visualScenes.values.any{it.sourceEvent==event&&it.profile==key&&it.phaseKey==phase&&it.receivers.map{e->e.uniqueId}==receivers.map{e->e.uniqueId}})return
        val targets=receivers.take(2).filter{it.isValid&&!it.isDead&&it.world==origin.world&&it.location.distanceSquared(origin)<=32*32}
        if(targets.isEmpty())return
        val observerRadius=limit("visuals.observer-radius",24,32).toDouble()
        val observers=origin.world.players.asSequence().filter{it.location.distanceSquared(origin)<=observerRadius*observerRadius&&it.canSee(player)&&targets.all{target->it.canSee(target)}}
            .take(limit("visuals.max-observers",16,32)).map{it.uniqueId}.toList()
        if(observers.isEmpty())return
        val scene=VisualScene(UUID.randomUUID(),key,prepared,player.uniqueId,session(player)?:return,origin.clone(),targets,observers,minOf(radius,2.0),progress.coerceIn(0,3),event,phase)
        visualScenes[scene.id]=scene
        for(id in targets.map{it.uniqueId}+player.uniqueId)visualEntities.getOrPut(id){mutableSetOf()}.add(scene.id)
        try{if(!renderVisual(scene))removeVisual(scene.id)}catch(problem:RuntimeException){removeVisual(scene.id);throw problem}catch(problem:LinkageError){removeVisual(scene.id);throw problem}
        if(visualScenes.isNotEmpty()&&visualTask==null)visualTask=Bukkit.getScheduler().runTaskTimer(owner,Runnable{
            for(current in visualScenes.values.toList())try{
                if(!renderVisual(current))removeVisual(current.id)
            }catch(problem:RuntimeException){removeVisual(current.id);visualFailure(current.profile,problem)}
             catch(problem:LinkageError){removeVisual(current.id);visualFailure(current.profile,problem)}
            if(visualScenes.isEmpty()){visualTask?.cancel();visualTask=null}
        },1L,1L)
    }
    private fun renderVisual(scene:VisualScene):Boolean {
        if(scene.age>=scene.phase.ticks||!sameSession(scene.actor,scene.token))return false
        val actor=Bukkit.getPlayer(scene.actor)?:return false
        val world=scene.origin.world
        if(!actor.isOnline||actor.isDead||actor.world!=world||!world.isChunkLoaded(scene.origin.blockX shr 4,scene.origin.blockZ shr 4))return false
        if(scene.receivers.any{!it.isValid||it.isDead||it.world!=world||it.location.distanceSquared(scene.origin)>32*32})return false
        val origin=VfxProfiles.Point(scene.origin.x,scene.origin.y,scene.origin.z)
        val location=scene.receivers.first().location
        if(!world.isChunkLoaded(location.blockX shr 4,location.blockZ shr 4))return false
        val target=VfxProfiles.Point(location.x,location.y,location.z)
        val observerRadius=limit("visuals.observer-radius",24,32).toDouble()
        val frames=scene.phase.steps.map{it to VfxProfiles.points(it,scene.age,origin,target,scene.radius,scene.progress)}
        val perScene=limit("visuals.per-activation",16,16)
        for(observerId in scene.observers){
            val observer=Bukkit.getPlayer(observerId)?:continue
            if(!observer.isOnline||observer.world!=world||observer.location.distanceSquared(scene.origin)>observerRadius*observerRadius||!observer.canSee(actor)||scene.receivers.any{!observer.canSee(it)})continue
            for((step,points) in frames){
                val remaining=perScene-(scene.sent[observerId]?:0)
                val count=budget.take(observerId,minOf(points.size,remaining.coerceAtLeast(0)),perScene,limit("visuals.per-observer-per-second",64,256),limit("visuals.global-per-second",256,4096))
                for(point in points.take(count))observer.spawnParticle(step.particle,point.x,point.y,point.z,1,0.0,0.0,0.0,0.0,step.data)
                if(count>0){
                    scene.sent[observerId]=(scene.sent[observerId]?:0)+count
                    if(scene.phase.sound.isNotEmpty()&&scene.age>=scene.phase.soundTick&&scene.sounded.add(observerId))
                        observer.playSound(if(scene.phase.soundAtTarget)location else scene.origin,scene.phase.sound,SoundCategory.PLAYERS,scene.phase.volume,scene.phase.pitch)
                }
            }
        }
        proof?.pending?.event?.let { diagnostic.note(it,"visual_frame") { "profile=${scene.profile} phase=${scene.phaseKey} sent=${scene.sent.values.sum()} observers=${scene.observers.size}" } }
        scene.age++
        return scene.age<scene.phase.ticks&&scene.observers.any{(scene.sent[it]?:0)<perScene}
    }
    private fun removeVisual(id:UUID){
        val scene=visualScenes.remove(id)?:return
        for(entity in scene.receivers.map{it.uniqueId}+scene.actor)visualEntities[entity]?.let{it.remove(id);if(it.isEmpty())visualEntities.remove(entity)}
        if(visualScenes.isEmpty()){visualTask?.cancel();visualTask=null}
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun visualWorldUnload(event:org.bukkit.event.world.WorldUnloadEvent){visualScenes.values.filter{it.origin.world==event.world}.map{it.id}.forEach{removeVisual(it)}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun visualChunkUnload(event:org.bukkit.event.world.ChunkUnloadEvent){
        visualScenes.values.filter{scene->(listOf(scene.origin)+scene.receivers.map{it.location}).any{it.world==event.world&&(it.blockX shr 4)==event.chunk.x&&(it.blockZ shr 4)==event.chunk.z}}.map{it.id}.forEach{removeVisual(it)}
    }

    private fun now() = System.nanoTime()/1_000_000L
    private fun config() = owner.configYml
    private fun limit(key: String,default: Int,max: Int): Int = (config().getIntOrNull("original-procs.$key") ?: default).coerceIn(0,max)
    fun enable(plugin: EcoEnchantsPlugin) {
        if(registered)return
        owner=plugin
        reset()
        Bukkit.getPluginManager().registerEvents(this,plugin)
        registered=true
    }
    fun reset() {
        stopDiagnostic()
        pending.values.toList().forEach { it.task?.cancel() };pending.clear();pendingEntities.clear()
        slows.keys.toList().forEach { removeSlow(it) };slowsBySource.clear()
        shots.clear();sessions.clear();engine.clear();budget.clear();proof=null
        if(::owner.isInitialized){
            prepareVisuals()
            shots=ShotLedger(limit("max-shots",4096,4096).coerceAtLeast(1),limit("shot-ttl-seconds",30,60).coerceAtLeast(1)*1000L) { now() }
            Bukkit.getOnlinePlayers().forEach { token(it) }
        }
    }
    fun close(){reset();visualProfiles=emptyMap();sessions.clear();HandlerList.unregisterAll(this);registered=false}
    private fun token(player: Player): UUID? {
        sessions[player.uniqueId]?.let { return it.token }
        if(sessions.size>=1024)return null
        return UUID.randomUUID().also { sessions[player.uniqueId]=Session(it,now()) }
    }
    private fun source(player: Player,item: ItemStack,family: OriginalProcEngine.Family,slot: EquipmentSlot): Source? {
        val id=if(family==OriginalProcEngine.Family.ECO_RESONANTE)"eco_resonante" else "sello_glacial"
        val enchant=EcoEnchants.getByID(id) ?: return null
        val tier=item.getEnchantmentLevel(enchant.enchantment)
        if(tier !in 1..3||item.amount!=1)return null
        val session=token(player) ?: return null
        val frozen=NativeProcAdapters.capture(item,itemKey) ?: return null
        if(frozen.stamped){
            val live=if(slot==EquipmentSlot.HAND)player.inventory.itemInMainHand else player.inventory.itemInOffHand
            if(live!=item)return null
            if(slot==EquipmentSlot.HAND)player.inventory.setItemInMainHand(frozen.item) else player.inventory.setItemInOffHand(frozen.item)
        }
        return Source(player.uniqueId,player.world.uid,session,family,frozen.id,frozen.item,tier)
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun shoot(event: EntityShootBowEvent) {
        val player=event.entity as? Player ?: return
        val bow=event.bow ?: return
        if(event.projectile !is AbstractArrow)return
        val live=if(event.hand==EquipmentSlot.HAND)player.inventory.itemInMainHand else player.inventory.itemInOffHand
        if(live!=bow)return
        NativeBurstProgression.shoot(event)
        if(bow.type!=Material.BOW)return
        val actual=if(event.hand==EquipmentSlot.HAND)player.inventory.itemInMainHand else player.inventory.itemInOffHand
        val snapshot=source(player,actual,OriginalProcEngine.Family.SELLO_GLACIAL,event.hand) ?: return
        shots.remember(event.projectile.uniqueId,player.uniqueId,snapshot)
    }
    internal fun modeAllows(family:String,player:Player,target:LivingEntity,defensive:Boolean=false,allowDead:Boolean=false):Boolean =
        eligibleFor(family,player,target,allowDead,defensive)
    internal fun eligible(player:Player,victim:LivingEntity,allowDead:Boolean=false):Boolean = eligibleFor("swarm",player,victim,allowDead)
    internal fun eligibleFor(family:String?,player:Player,victim:LivingEntity,allowDead:Boolean=false,defensive:Boolean=false):Boolean {
        val enchant=EcoEnchants.getByID(family?:"swarm") as? com.willfp.ecoenchants.enchant.impl.EcoEnchantBase ?: return false
        return enchant.combatTargetPolicy.allows(player,victim,allowDead,defensive)
    }
    // Preserve the old entry point while using one confirmation queue for all native combat.
    fun hit(event: EntityDamageByEntityEvent) = observe(event)
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun observe(event: EntityDamageEvent) {
        val actor = (event as? EntityDamageByEntityEvent)?.damager.let { it as? Player ?: (it as? Projectile)?.shooter as? Player }
        diagnostic.observe(actor?.uniqueId,event)
        diagnostic.note(event,"observe") { "cancelled=${event.isCancelled} finalDamage=${event.finalDamage} duplicate=${pending.containsKey(event)} secondary=$secondary provenance=${DamageMultiplierBypass.current?.origin}" }
        if(pending.containsKey(event)||event.finalDamage<=0)return
        val victim=event.entity as? LivingEntity ?: return
        val original=if(secondary==0&&DamageMultiplierBypass.current==null&&event is EntityDamageByEntityEvent) originalSource(event,victim) else null
        diagnostic.note(event,"source") { "captured=${original!=null} family=${original?.first?.family} tier=${original?.first?.tier}" }
        val combat=NativeCombatProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        val burst=NativeBurstProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        val reactive=NativeReactiveProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        if(original==null&&combat==null&&burst==null&&reactive==null)return
        if(pending.size>=limit("max-pending-hits",512,1024)){combat?.let{NativeCombatProgression.uncertain(it,victim)};return}
        val candidate=Pending(event,original?.first,combat,victim,victim.health,UUID.randomUUID(),original?.second,burst,reactive)
        pending[event]=candidate;linkPending(candidate)
        candidate.task=Bukkit.getScheduler().runTask(owner,Runnable { confirm(candidate) })
    }
    private fun originalSource(event: EntityDamageByEntityEvent,victim: LivingEntity): Pair<Source,UUID?>? {
        var arrowId: UUID?=null
        val context=when(val damager=event.damager){
            is Player -> {
                if(event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK)return null
                val item=damager.inventory.itemInMainHand
                diagnostic.note(event,"weapon") { "type=${item.type} amount=${item.amount} tier=${EcoEnchants.getByID("eco_resonante")?.let { item.getEnchantmentLevel(it.enchantment) }}" }
                if(!item.type.name.endsWith("_SWORD"))return null
                source(damager,item,OriginalProcEngine.Family.ECO_RESONANTE,EquipmentSlot.HAND)
            }
            is AbstractArrow -> {
                val found=shots.source(damager.uniqueId) ?: return null
                if(damager.shooter !is Player||(damager.shooter as Player).uniqueId!=found.player)return null
                arrowId=damager.uniqueId;found
            }
            else -> null
        } ?: return null
        val player=Bukkit.getPlayer(context.player) ?: return null
        val eligible=eligibleFor(if(context.family==OriginalProcEngine.Family.ECO_RESONANTE)"eco_resonante" else "sello_glacial",player,victim)
        diagnostic.note(event,"eligibility") { "allowed=$eligible family=${context.family} tier=${context.tier}" }
        return if(eligible)Pair(context,arrowId) else null
    }
    private fun actor(candidate:Pending)=candidate.source?.player?:candidate.combat?.actor?:candidate.burst?.sources?.firstOrNull()?.player
    private fun linkPending(candidate:Pending){
        for(id in listOfNotNull(candidate.victim.uniqueId,actor(candidate)))pendingEntities.getOrPut(id){java.util.Collections.newSetFromMap(IdentityHashMap())}.add(candidate.event)
    }
    private fun unlinkPending(candidate:Pending){
        for(id in listOfNotNull(candidate.victim.uniqueId,actor(candidate)))pendingEntities[id]?.let{events->events.remove(candidate.event);if(events.isEmpty())pendingEntities.remove(id)}
    }
    internal fun track(event: EntityDamageByEntityEvent): UUID? {
        observe(event)
        return pending[event]?.eventId
    }
    internal fun observedSession(player: Player): UUID? = sessions[player.uniqueId]?.token
    internal fun session(player: Player): UUID? = token(player)
    internal fun age(player: Player): Long = sessions[player.uniqueId]?.let { now()-it.arrived } ?: -1
    internal fun sameSession(id: UUID,expected: UUID): Boolean = sessions[id]?.token==expected
    private fun confirm(candidate: Pending) {
        if(pending[candidate.event]!==candidate)return
        pending.remove(candidate.event);unlinkPending(candidate)
        candidate.task?.cancel()
        var observedDamage=0.0
        try {
        val victim=candidate.victim
        val damage=NativeProcAdapters.confirmed(candidate.event,victim,candidate.before)
        observedDamage=damage
        diagnostic.note(candidate.event,"confirm") { "acceptedDamage=$damage cancelled=${candidate.event.isCancelled} sameLastCause=${victim.lastDamageCause===candidate.event} before=${candidate.before} after=${victim.health} finalDamage=${candidate.event.finalDamage}" }
        if(damage<=0)return
        candidate.combat?.let { NativeCombatProgression.confirm(it,candidate.event,victim,candidate.before,damage) }
        candidate.reactive?.let{NativeReactiveProgression.confirm(it,candidate.event,victim,damage,candidate.eventId)}
        candidate.burst?.let{NativeBurstProgression.confirm(it,candidate.event,victim,damage,candidate.eventId)}
        val source=candidate.source ?: return
        val player=Bukkit.getPlayer(source.player) ?: return
        if(sessions[source.player]?.token!=source.token||player.world.uid!=source.world||!player.isOnline||!eligibleFor(if(source.family==OriginalProcEngine.Family.ECO_RESONANTE)"eco_resonante" else "sello_glacial",player,victim,true))return
        if(candidate.arrow!=null&&!shots.claim(candidate.arrow,victim.uniqueId))return
        val id=if(source.family==OriginalProcEngine.Family.ECO_RESONANTE)"eco_resonante" else "sello_glacial"
        val enchant=EcoEnchants.getByID(id) ?: return
        diagnostic.note(candidate.event,"confirmed_source") { "family=${source.family} tier=${source.tier}" }
        val itemCopy=source.item.clone()
        val holder=ItemProvidedHolder(enchant.getLevel(source.tier),itemCopy)
        val previous=proof;proof=Proof(candidate,damage)
        try {
            diagnostic.note(candidate.event,"dispatch") { "triggerEnabled=${ConfirmedOriginalHit.isEnabled} holder=${holder.holder.id}" }
            ConfirmedOriginalHit.dispatch(player.toDispatcher(),TriggerData(player=player,victim=victim,item=itemCopy,
                location=victim.location,event=candidate.event,value=damage),listOf(holder))
            diagnostic.note(candidate.event,"dispatch_returned")
        } finally { proof=previous }
        } finally { NativeFirstHits.finish(candidate.eventId,observedDamage,candidate.event);NativeReactiveProgression.finish(candidate.event) }
    }
    fun execute(family: OriginalProcEngine.Family,amount: Double,data: TriggerData): Boolean {
        data.event?.let { event -> diagnostic.note(event,"execute") { "family=$family amount=$amount proof=${proof!=null}" } }
        val confirmed=proof ?: return false
        val candidate=confirmed.pending;val src=candidate.source ?: return false
        if(src.family!=family||data.event!==candidate.event||data.player?.uniqueId!=src.player||data.victim?.uniqueId!=candidate.victim.uniqueId)return false
        val expected=if(family==OriginalProcEngine.Family.ECO_RESONANTE)2.0+src.tier*2 else 5.0+src.tier*5
        if(!amount.isFinite()||abs(amount-expected)>1e-9)return false
        val arrived=sessions[src.player]?.arrived ?: return false
        diagnostic.note(candidate.event,"arrival") { "ageMillis=${now()-arrived} ready=${ProcGuards.arrivalReady(family,now()-arrived)}" }
        if(!ProcGuards.arrivalReady(family,now()-arrived))return false
        if(family==OriginalProcEngine.Family.SELLO_GLACIAL &&
            (!candidate.victim.isValid||candidate.victim.isDead||candidate.victim.getAttribute(Attribute.MOVEMENT_SPEED)==null||
                (!slows.containsKey(candidate.victim.uniqueId)&&slows.size>=limit("max-active-slows",512,1024))))return false
        val activation=engine.record(family,OriginalProcEngine.Hit(candidate.eventId,src.player,src.itemId,
            candidate.victim.uniqueId,src.world,src.tier,confirmed.damage,true,true,true,false,false,false,false,false,true,false,false,false), { accepted ->
                diagnostic.note(candidate.event,"accepted_hit") { "count=$accepted" }
                if(family==OriginalProcEngine.Family.ECO_RESONANTE&&accepted<3)procVisualProfile(data.player!!,candidate.victim.location,"eco_resonante","progress",listOf(candidate.victim),candidate.eventId,2.0,accepted)
            }).orElse(null) ?: return false
        diagnostic.note(candidate.event,"activation") { "family=$family value=${activation.value()}" }
        val player=Bukkit.getPlayer(src.player) ?: return false
        if(family==OriginalProcEngine.Family.ECO_RESONANTE){
            val origin=candidate.victim.location
            val targets=NativeBurstProgression.targets(player,candidate.victim,2.0,2,family="eco_resonante")
            diagnostic.note(candidate.event,"area_candidates") { "count=${targets.size}" }
            val resolved=mutableListOf<LivingEntity>()
            targets.forEach { target ->
                if(eligibleFor("eco_resonante",player,target)&&target.location.distanceSquared(origin)<=4&&candidate.victim.hasLineOfSight(target)&&target.noDamageTicks<=target.maximumNoDamageTicks/2){
                    val before=target.health;val previousDamage=target.lastDamageCause
                    withSecondaryDamage {
                        com.willfp.libreforge.effects.impl.RepeatedDamage.run(player.uniqueId,player.uniqueId,target.uniqueId){
                            target.dealDamage(confirmed.damage*activation.value()/100,source=player,trueDamage=false,checkAntigrief=true)
                        }
                    }
                    if(NativeProcAdapters.resolvedBy(player,target,before,previousDamage))resolved.add(target)
                }
            }
            if(resolved.isNotEmpty())procVisualProfile(player,origin,"eco_resonante","resolved",resolved,candidate.eventId,2.0)
        } else applySlow(player,candidate.victim,activation.value()/100)
        if(family!=OriginalProcEngine.Family.ECO_RESONANTE)visual(player,candidate.victim.location,family,activation.maxParticles())
        return true
    }
    internal fun recordNative(family:OriginalProcEngine.Family,hit:OriginalProcEngine.Hit)=engine.record(family,hit)
    internal fun reserveNative(family:OriginalProcEngine.Family,player:UUID,attempt:UUID)=engine.reserve(family,player,attempt)
    internal fun rollbackNative(family:OriginalProcEngine.Family,player:UUID,lease:OriginalProcEngine.Lease)=engine.rollback(family,player,lease)
    internal fun procVisual(player:Player,origin:Location,requested:Int)=visual(player,origin,OriginalProcEngine.Family.ECO_RESONANTE,requested)
    private fun applySlow(player: Player,target: LivingEntity,fraction: Double) {
        if(!target.isValid||target.isDead)return
        val attribute=target.getAttribute(Attribute.MOVEMENT_SPEED) ?: return
        if(!slows.containsKey(target.uniqueId)&&slows.size>=limit("max-active-slows",512,1024))return
        removeSlow(target.uniqueId)
        val modifier=NativeProcAdapters.slow(attribute,slowKey,fraction)
        val record=Slow(player.uniqueId,target,modifier);slows[target.uniqueId]=record
        slowsBySource.getOrPut(player.uniqueId){mutableSetOf()}.add(target.uniqueId)
        record.task=Bukkit.getScheduler().runTaskLater(owner,Runnable {
            if(slows[target.uniqueId]===record)removeSlow(target.uniqueId)
        },40L)
    }
    private fun removeSlow(id: UUID) {
        val slow=slows.remove(id) ?: return;slow.task?.cancel()
        slowsBySource[slow.source]?.let{targets->targets.remove(id);if(targets.isEmpty())slowsBySource.remove(slow.source)}
        slow.entity.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(slow.modifier)
    }
    private fun visual(player: Player,origin: Location,family: OriginalProcEngine.Family,requested: Int) {
        if(config().getBoolOrNull("original-procs.visuals.enabled")==false)return
        val radius=limit("visuals.observer-radius",24,32).toDouble()
        val maxObservers=limit("visuals.max-observers",16,32)
        val particleName=config().getStringOrNull("original-procs.visuals."+
            if(family==OriginalProcEngine.Family.ECO_RESONANTE)"pulse-particle" else "seal-particle")
        val fallback=if(family==OriginalProcEngine.Family.ECO_RESONANTE)Particle.END_ROD else Particle.SNOWFLAKE
        val particle=runCatching { Particle.valueOf(particleName ?: fallback.name) }.getOrDefault(fallback)
            .takeIf { it in setOf(Particle.END_ROD,Particle.SNOWFLAKE,Particle.WAX_OFF) } ?: fallback
        for(observer in origin.world.players.asSequence().filter { it.location.distanceSquared(origin)<=radius*radius }.take(maxObservers)){
            val count=budget.take(observer.uniqueId,requested,limit("visuals.per-activation",16,16),limit("visuals.per-observer-per-second",64,256),limit("visuals.global-per-second",256,4096))
            if(count<=0)continue
            try {
                for(i in 0 until count){
                    val angle=2*PI*i/count
                    observer.spawnParticle(particle,origin.x+cos(angle)*0.7,origin.y+1.0,origin.z+sin(angle)*0.7,1,0.0,0.0,0.0,0.0)
                }
                val sound=config().getStringOrNull("original-procs.visuals.sound") ?: "block.amethyst_block.chime"
                if(sound.isNotEmpty())observer.playSound(origin,sound,SoundCategory.PLAYERS,(config().getDoubleOrNull("original-procs.visuals.volume") ?: 0.25).coerceIn(0.0,1.0).toFloat(),(config().getDoubleOrNull("original-procs.visuals.pitch") ?: 1.2).coerceIn(0.5,2.0).toFloat())
            } catch(ignored: RuntimeException) { /* Only aesthetics degrade; the mechanic is already resolved. */ }
        }
    }
    internal fun withSecondaryDamage(body: () -> Unit){secondary++;try{body()}finally{secondary--}}
    @EventHandler(priority=EventPriority.HIGHEST)
    fun noSecondaryProcs(event: TriggerDispatchEvent){
        // Never suppress real death/drop/XP or receiver callbacks to avoid sender proc recursion.
        if(secondary>0&&event.trigger.trigger.id in setOf("melee_attack","bow_attack","trident_attack","headshot"))event.isCancelled=true
    }
    private fun retire(id: UUID) {
        visualEntities[id]?.toList()?.forEach { removeVisual(it) }
        NativeReactiveProgression.retire(id)
        NativeBurstProgression.retire(id)
        NativeCombatProgression.retire(id)
        sessions.remove(id);engine.clearEntity(id)
        for(event in pendingEntities.remove(id)?.toList()?:emptyList()){
            val candidate=pending[event]?:continue
            pending.remove(event);unlinkPending(candidate);candidate.task?.cancel()
        }
        shots.removeOwner(id)
        removeSlow(id);slowsBySource[id]?.toList()?.forEach { removeSlow(it) }
    }
    @EventHandler fun join(event: PlayerJoinEvent){token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler fun respawn(event: PlayerRespawnEvent){token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler fun quit(event: PlayerQuitEvent){retire(event.player.uniqueId)}
    @EventHandler fun world(event: PlayerChangedWorldEvent){retire(event.player.uniqueId);token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler(priority=EventPriority.MONITOR)
    fun death(event: EntityDeathEvent) {
        // Death is a real post-damage boundary and may confirm the third hit immediately.
        pendingEntities[event.entity.uniqueId]?.toList()?.forEach { damage -> pending[damage]?.takeIf { it.victim==event.entity }?.let { confirm(it) } }
        retire(event.entity.uniqueId)
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun teleport(event: EntityTeleportEvent){if(event.to?.world!=event.from.world)retire(event.entity.uniqueId)}
}
