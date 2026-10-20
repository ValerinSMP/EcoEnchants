package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
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
    private data class Session(val token: UUID,val arrived: Long)
    private val sessions = mutableMapOf<UUID,Session>()
    private data class Source(val player: UUID,val world: UUID,val token: UUID,val family: OriginalProcEngine.Family,
                              val itemId: UUID,val item: ItemStack,val tier: Int)
    private var shots = ShotLedger<Source>(4096,30_000) { now() }
    private data class Pending(val event: EntityDamageEvent,val source: Source?,val combat: NativeCombatProgression.Capture?,val victim: LivingEntity,
                               val before: Double,val eventId: UUID,val arrow: UUID?,val burst:NativeBurstProgression.Capture?=null,val reactive:NativeReactiveProgression.Capture?=null,var task: BukkitTask? = null)
    private val pending = IdentityHashMap<EntityDamageEvent,Pending>()
    private data class Proof(val pending: Pending,val damage: Double)
    private var proof: Proof? = null
    private var secondary = 0
    private var mythicBroken = false
    private var registered = false
    private data class Slow(val source: UUID,val entity: LivingEntity,val modifier: AttributeModifier,var task: BukkitTask? = null)
    private val slows = mutableMapOf<UUID,Slow>()
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
        pending.values.toList().forEach { it.task?.cancel() };pending.clear()
        slows.keys.toList().forEach { removeSlow(it) }
        shots.clear();sessions.clear();engine.clear();budget.clear();proof=null;mythicBroken=false
        if(::owner.isInitialized){
            shots=ShotLedger(limit("max-shots",4096,4096).coerceAtLeast(1),limit("shot-ttl-seconds",30,60).coerceAtLeast(1)*1000L) { now() }
            Bukkit.getOnlinePlayers().forEach { token(it) }
        }
    }
    fun close(){reset();sessions.clear();HandlerList.unregisterAll(this);registered=false}
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
    internal fun eligible(player: Player,victim: LivingEntity,allowDead: Boolean=false): Boolean {
        if(victim !is Enemy||(!allowDead&&(!victim.isValid||victim.isDead))||victim.world!=player.world||!ProcGuards.mayAct(player.isOnline,player.isDead))return false
        val reason=victim.entitySpawnReason.name
        val boss=victim is Boss||victim.type in setOf(EntityType.WARDEN,EntityType.ELDER_GUARDIAN)
        val marked=victim.persistentDataContainer.keys.isNotEmpty()||victim.scoreboardTags.isNotEmpty()||victim.hasMetadata("NPC")||victim.customName()!=null||victim is Tameable
        val allied=sequenceOf(player.scoreboard,Bukkit.getScoreboardManager()?.mainScoreboard).filterNotNull().any { board ->
            val team=board.getEntryTeam(player.name)
            team!=null&&team==board.getEntryTeam(victim.uniqueId.toString())
        }
        // Reject known cheap exclusions before Mythic/protection integrations, including swarm scans.
        if(!ProcGuards.eligible(true,reason,boss,false,marked,allied,victim.isInvulnerable,false))return false
        val mythic=Bukkit.getPluginManager().getPlugin("MythicMobs")
        if(mythicBroken||(mythic!=null&&!mythic.isEnabled))return false
        val managed=if(mythic==null)false else try { MythicExclusion.isManaged(victim) } catch(problem: LinkageError) {
            mythicBroken=true;owner.logger.log(java.util.logging.Level.SEVERE,"Original procs blocked: MythicMobs identity API unavailable",problem);return false
        } catch(problem: RuntimeException) {
            mythicBroken=true;owner.logger.log(java.util.logging.Level.SEVERE,"Original procs blocked: MythicMobs identity query failed",problem);return false
        }
        return !managed&&AntigriefManager.canInjure(player,victim)
    }
    // Preserve the old entry point while using one confirmation queue for all native combat.
    fun hit(event: EntityDamageByEntityEvent) = observe(event)
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun observe(event: EntityDamageEvent) {
        if(pending.containsKey(event)||event.finalDamage<=0)return
        val victim=event.entity as? LivingEntity ?: return
        val original=if(secondary==0&&DamageMultiplierBypass.current==null&&event is EntityDamageByEntityEvent) originalSource(event,victim) else null
        val combat=NativeCombatProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        val burst=NativeBurstProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        val reactive=NativeReactiveProgression.capture(event,victim,secondary==0&&DamageMultiplierBypass.current==null)
        if(original==null&&combat==null&&burst==null&&reactive==null)return
        if(pending.size>=limit("max-pending-hits",512,1024)){combat?.let{NativeCombatProgression.uncertain(it,victim)};return}
        val candidate=Pending(event,original?.first,combat,victim,victim.health,UUID.randomUUID(),original?.second,burst,reactive)
        pending[event]=candidate
        candidate.task=Bukkit.getScheduler().runTask(owner,Runnable { confirm(candidate) })
    }
    private fun originalSource(event: EntityDamageByEntityEvent,victim: LivingEntity): Pair<Source,UUID?>? {
        var arrowId: UUID?=null
        val context=when(val damager=event.damager){
            is Player -> {
                if(event.cause!=EntityDamageEvent.DamageCause.ENTITY_ATTACK)return null
                val item=damager.inventory.itemInMainHand
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
        return if(eligible(player,victim))Pair(context,arrowId) else null
    }
    internal fun track(event: EntityDamageByEntityEvent): UUID? {
        observe(event)
        return pending[event]?.eventId
    }
    internal fun session(player: Player): UUID? = token(player)
    internal fun age(player: Player): Long = sessions[player.uniqueId]?.let { now()-it.arrived } ?: -1
    internal fun sameSession(id: UUID,expected: UUID): Boolean = sessions[id]?.token==expected
    private fun confirm(candidate: Pending) {
        if(pending[candidate.event]!==candidate)return
        pending.remove(candidate.event)
        candidate.task?.cancel()
        var observedDamage=0.0
        try {
        val victim=candidate.victim
        val damage=NativeProcAdapters.confirmed(candidate.event,victim,candidate.before)
        observedDamage=damage
        if(damage<=0)return
        candidate.combat?.let { NativeCombatProgression.confirm(it,candidate.event,victim,candidate.before,damage) }
        candidate.reactive?.let{NativeReactiveProgression.confirm(it,candidate.event,victim,damage,candidate.eventId)}
        candidate.burst?.let{NativeBurstProgression.confirm(it,candidate.event,victim,damage,candidate.eventId)}
        val source=candidate.source ?: return
        val player=Bukkit.getPlayer(source.player) ?: return
        if(sessions[source.player]?.token!=source.token||player.world.uid!=source.world||!player.isOnline||!eligible(player,victim,true))return
        if(candidate.arrow!=null&&!shots.claim(candidate.arrow,victim.uniqueId))return
        val id=if(source.family==OriginalProcEngine.Family.ECO_RESONANTE)"eco_resonante" else "sello_glacial"
        val enchant=EcoEnchants.getByID(id) ?: return
        val itemCopy=source.item.clone()
        val holder=ItemProvidedHolder(enchant.getLevel(source.tier),itemCopy)
        val previous=proof;proof=Proof(candidate,damage)
        try {
            ConfirmedOriginalHit.dispatch(player.toDispatcher(),TriggerData(player=player,victim=victim,item=itemCopy,
                location=victim.location,event=candidate.event,value=damage),listOf(holder))
        } finally { proof=previous }
        } finally { NativeFirstHits.finish(candidate.eventId,observedDamage,candidate.event);NativeReactiveProgression.finish(candidate.event) }
    }
    fun execute(family: OriginalProcEngine.Family,amount: Double,data: TriggerData): Boolean {
        val confirmed=proof ?: return false
        val candidate=confirmed.pending;val src=candidate.source ?: return false
        if(src.family!=family||data.event!==candidate.event||data.player?.uniqueId!=src.player||data.victim?.uniqueId!=candidate.victim.uniqueId)return false
        val expected=if(family==OriginalProcEngine.Family.ECO_RESONANTE)2.0+src.tier*2 else 5.0+src.tier*5
        if(!amount.isFinite()||abs(amount-expected)>1e-9)return false
        val arrived=sessions[src.player]?.arrived ?: return false
        if(!ProcGuards.arrivalReady(family,now()-arrived))return false
        if(family==OriginalProcEngine.Family.SELLO_GLACIAL &&
            (!candidate.victim.isValid||candidate.victim.isDead||candidate.victim.getAttribute(Attribute.MOVEMENT_SPEED)==null||
                (!slows.containsKey(candidate.victim.uniqueId)&&slows.size>=limit("max-active-slows",512,1024))))return false
        val activation=engine.record(family,OriginalProcEngine.Hit(candidate.eventId,src.player,src.itemId,
            candidate.victim.uniqueId,src.world,src.tier,confirmed.damage,true,true,true,false,false,false,false,false,true,false,false,false)).orElse(null) ?: return false
        val player=Bukkit.getPlayer(src.player) ?: return false
        if(family==OriginalProcEngine.Family.ECO_RESONANTE){
            val origin=candidate.victim.location
            val targets=NativeBurstProgression.targets(player,candidate.victim,2.0,2)
            targets.forEach { target ->
                if(eligible(player,target)&&target.location.distanceSquared(origin)<=4&&candidate.victim.hasLineOfSight(target)&&target.noDamageTicks<=target.maximumNoDamageTicks/2){
                    withSecondaryDamage {
                        com.willfp.libreforge.effects.impl.RepeatedDamage.run(player.uniqueId,player.uniqueId,target.uniqueId){
                            target.dealDamage(confirmed.damage*activation.value()/100,source=player,trueDamage=false,checkAntigrief=true)
                        }
                    }
                }
            }
        } else applySlow(player,candidate.victim,activation.value()/100)
        visual(player,candidate.victim.location,family,activation.maxParticles())
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
        record.task=Bukkit.getScheduler().runTaskLater(owner,Runnable {
            if(slows[target.uniqueId]===record)removeSlow(target.uniqueId)
        },40L)
    }
    private fun removeSlow(id: UUID) {
        val slow=slows.remove(id) ?: return;slow.task?.cancel()
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
        NativeReactiveProgression.retire(id)
        NativeBurstProgression.retire(id)
        NativeCombatProgression.retire(id)
        sessions.remove(id);engine.clearEntity(id)
        pending.values.filter { it.source?.player==id||it.victim.uniqueId==id||it.combat?.actor==id||it.reactive?.player==id }.toList().forEach { pending.remove(it.event);it.task?.cancel() }
        shots.removeOwner(id)
        slows.filter { it.value.source==id||it.key==id }.keys.toList().forEach { removeSlow(it) }
    }
    @EventHandler fun join(event: PlayerJoinEvent){token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler fun respawn(event: PlayerRespawnEvent){token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler fun quit(event: PlayerQuitEvent){retire(event.player.uniqueId)}
    @EventHandler fun world(event: PlayerChangedWorldEvent){retire(event.player.uniqueId);token(event.player);NativeCombatProgression.arrival(event.player);NativeReactiveProgression.arrival(event.player)}
    @EventHandler(priority=EventPriority.MONITOR)
    fun death(event: EntityDeathEvent) {
        // Death is a real post-damage boundary and may confirm the third hit immediately.
        pending.values.filter { it.victim==event.entity }.toList().forEach { confirm(it) }
        retire(event.entity.uniqueId)
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    fun teleport(event: EntityTeleportEvent){if(event.to?.world!=event.from.world)retire(event.entity.uniqueId)}
}
