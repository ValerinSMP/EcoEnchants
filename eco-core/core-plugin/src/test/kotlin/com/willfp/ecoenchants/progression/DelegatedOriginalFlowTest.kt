package com.willfp.ecoenchants.progression

import com.willfp.eco.util.NumberUtils
import com.willfp.eco.core.items.HashedItem
import com.willfp.eco.core.config.base.ConfigYml
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.InjectablePlaceholder
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.ecoenchants.enchant.EcoEnchantLevel
import com.willfp.libreforge.*
import com.willfp.libreforge.conditions.*
import com.willfp.libreforge.effects.*
import com.willfp.libreforge.effects.arguments.EffectArgumentList
import com.willfp.libreforge.filters.FilterList
import com.willfp.libreforge.mutators.MutatorList
import com.willfp.libreforge.triggers.*
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.entity.*
import org.bukkit.inventory.*
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.*
import org.bukkit.scheduler.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.mockito.Answers
import java.util.UUID
import java.util.logging.Logger

/** Real observe/confirm/forceHolders/condition-list/element/effect/counter.
 * Bukkit, skill authority and Eco expression service are simulated boundaries, not server acceptance. */
class DelegatedOriginalFlowTest {
 private fun <T> match(type:Class<T>):T = org.mockito.Mockito.any(type)
 private fun <T> nullArgument():T = org.mockito.Mockito.isNull<T>()
 private fun runtimeField(name:String)=OriginalRuntime::class.java.getDeclaredField(name).apply{isAccessible=true}
 private fun flow(skillLevel:Int)=DelegatedCombatPolicyTest.Fixture().run {
  val root=mock(ConfigYml::class.java)
  `when`(root.getIntOrNull(anyString())).thenReturn(null)
  `when`(root.getBoolOrNull("original-procs.visuals.enabled")).thenReturn(false)
  `when`(owner.configYml).thenReturn(root)
  val log=mock(Logger::class.java);`when`(owner.logger).thenReturn(log)
  val jobs=mutableListOf<Runnable>();val scheduler=mock(BukkitScheduler::class.java);val task=mock(BukkitTask::class.java)
  `when`(scheduler.runTask(eq(owner),any(Runnable::class.java))).thenAnswer{jobs.add(it.getArgument(1));task}
  `when`(scheduler.runTaskLater(eq(owner),any(Runnable::class.java),eq(600L))).thenReturn(task)
  bukkit.`when`<BukkitScheduler>{Bukkit.getScheduler()}.thenReturn(scheduler)
  bukkit.`when`<Collection<org.bukkit.entity.Player>>{Bukkit.getOnlinePlayers()}.thenReturn(emptyList())
  bukkit.`when`<org.bukkit.plugin.PluginManager>{Bukkit.getPluginManager()}.thenReturn(mock(org.bukkit.plugin.PluginManager::class.java))
  `when`(actor.location).thenReturn(Location(world,0.0,64.0,0.0));`when`(target.location).thenReturn(Location(world,1.0,64.0,0.0))
  val inventory=mock(PlayerInventory::class.java);val item=mock(ItemStack::class.java)
  `when`(actor.inventory).thenReturn(inventory);`when`(inventory.itemInMainHand).thenReturn(item)
  `when`(item.type).thenReturn(Material.WOODEN_SWORD);`when`(item.amount).thenReturn(1);`when`(item.clone()).thenReturn(item)
  `when`(item.getEnchantmentLevel(nullArgument<Enchantment>())).thenReturn(3)
  val meta=mock(ItemMeta::class.java);val pdc=mock(PersistentDataContainer::class.java)
  `when`(item.itemMeta).thenReturn(meta);`when`(meta.persistentDataContainer).thenReturn(pdc)
  `when`(pdc.get(NamespacedKey("ecoenchants","original_item_id"),PersistentDataType.STRING)).thenReturn(UUID.randomUUID().toString())
  val lf=mock(LibreforgeSpigotPlugin::class.java,Answers.RETURNS_DEEP_STUBS)
  `when`(lf.namespacedKeyFactory.create(anyString())).thenAnswer{NamespacedKey("libreforge",it.getArgument(0))}
  `when`(lf.dispatchedTriggerFactory).thenReturn(DispatchedTriggerFactory(lf))
  val lfField=Class.forName("com.willfp.libreforge.LibreforgeSpigotPluginKt").getDeclaredField("plugin").apply{isAccessible=true};val previousLF=lfField.get(null)
  val previousOwner=runtimeField("owner").get(OriginalRuntime)
  val burstOwner=NativeBurstProgression::class.java.getDeclaredField("owner").apply{isAccessible=true};val previousBurst=burstOwner.get(NativeBurstProgression)
  val providers=Class.forName("com.willfp.libreforge.HolderProviderKt").getDeclaredField("holderPlaceholderProviders").apply{isAccessible=true}.get(null) as MutableList<*>
  val providerCount=providers.size
  var restorePlaceholder: (() -> Unit)? = null
  val injected=mutableListOf<InjectablePlaceholder>()
  val values=mapOf<String,Any>("amount" to "2 + 2 * %level%","hits" to 3,"window" to 5,"proc_cooldown" to 8)
  val args=mock(Config::class.java){call ->
   val path=call.arguments.firstOrNull() as? String
   when(call.method.name){
    "getKeys"->values.keys.toList();"get"->values[path];"getString"->values[path]?.toString();"getInt"->values[path] as? Int?:0
    "getBool"->false;"has"->values.containsKey(path)
    "addInjectablePlaceholder"->{ @Suppress("UNCHECKED_CAST") injected.addAll((call.arguments[0] as Iterable<InjectablePlaceholder>).toList());null }
    "getPlaceholderInjections"->injected
    else->Answers.RETURNS_DEFAULTS.answer(call)
   }
  }
  val condition=mock(ConditionBlock::class.java)
  `when`(condition.condition).thenReturn(mock(Condition::class.java))
  `when`(condition.isMet(match(Dispatcher::class.java),match(ProvidedHolder::class.java))).thenReturn(skillLevel>=16)
  val effect=OriginalProcEffect(OriginalProcEngine.Family.ECO_RESONANTE,"resonant_pulse")
  val ctor=ChainElement::class.java.declaredConstructors.single().apply{isAccessible=true}
  @Suppress("UNCHECKED_CAST") val element=ctor.newInstance(effect,args,args,NoCompileData,EffectArgumentList(emptyList()),ConditionList(emptyList()),MutatorList(emptyList()),FilterList(emptyList()),1.0,null) as ChainElement<NoCompileData>
  val block=mock(EffectBlock::class.java);val chain=mock(Chain::class.java)
  `when`(chain.iterator()).thenAnswer{listOf(element).iterator()};`when`(block.effects).thenReturn(chain);`when`(block.conditions).thenReturn(ConditionList(emptyList()))
  `when`(block.canBeTriggeredBy(ConfirmedOriginalHit)).thenReturn(true)
  doAnswer{call ->
   val dispatched=call.getArgument<DispatchedTrigger>(0)
   assertTrue(dispatched.rawPlaceholders.any { "level" in it.identifiers })
   element.trigger(dispatched);null
  }.`when`(block).tryTrigger(match(DispatchedTrigger::class.java))
  val level=EcoEnchantLevel(base,3,EffectList(listOf(block)),ConditionList(listOf(condition)))
  doReturn(level).`when`(base).getLevel(3)
  mockStatic(HashedItem::class.java).use{hashes ->
  val hashed=mock(HashedItem::class.java)
  hashes.`when`<HashedItem>{HashedItem.of(item)}.thenReturn(hashed)
  mockStatic(NumberUtils::class.java).use{api ->
  api.`when`<Double>{NumberUtils.evaluateExpression(eq("2 + 2 * %level%"),match(PlaceholderContext::class.java))}.thenAnswer{call ->
   val context=call.getArgument<PlaceholderContext>(1)
   val placeholder=injected.lastOrNull()?:error("Native holder level placeholder missing")
   assertEquals("3",placeholder.getValue("",context));8.0
  }
   lfField.set(null,lf);runtimeField("owner").set(OriginalRuntime,owner);burstOwner.set(NativeBurstProgression,owner)
   try {
    val placeholders=com.willfp.libreforge.triggers.placeholders.TriggerPlaceholders
    val unrelatedVictimPlaceholder=placeholders.get("victim")!!
    // Health attributes need Paper registries; the proc uses its native holder level, not victim_max_health.
    placeholders.remove(unrelatedVictimPlaceholder)
    restorePlaceholder={placeholders.register(unrelatedVictimPlaceholder)}
    OriginalRuntime.reset();ConfirmedOriginalHit.enable()
    registerHolderPlaceholderProvider<EcoEnchantLevel>{holder,_->listOf(NamedValue("level",holder.level))}
    val token=OriginalRuntime.session(actor)!!
    val sessions=runtimeField("sessions").get(OriginalRuntime) as MutableMap<UUID,Any>
    val sessionCtor=Class.forName("com.willfp.ecoenchants.progression.OriginalRuntime\$Session").declaredConstructors.single().apply{isAccessible=true}
    sessions[actor.uniqueId]=sessionCtor.newInstance(token,System.nanoTime()/1_000_000L-9_000)
    OriginalRuntime.startDiagnostic(actor.uniqueId)
    var health=100.0;var last:EntityDamageEvent?=null
    `when`(target.health).thenAnswer{health};`when`(target.lastDamageCause).thenAnswer{last}
    repeat(3){
     val event=mock(EntityDamageByEntityEvent::class.java)
     `when`(event.entity).thenReturn(target);`when`(event.damager).thenReturn(actor);`when`(event.cause).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK)
     `when`(event.finalDamage).thenReturn(6.0)
     OriginalRuntime.observe(event);assertEquals(1,jobs.size,mockingDetails(log).invocations.toString()+" level="+item.getEnchantmentLevel(base.enchantment))
     health-=6;last=event;jobs.removeAt(0).run()
    }
    val messages=mockingDetails(log).invocations.filter{it.method.name=="info"}.map{it.arguments[0].toString()}
    assertTrue(messages.any{ "stage=eligibility allowed=true" in it })
    assertEquals(if(skillLevel>=16)3 else 0,messages.count{"stage=accepted_hit" in it})
    assertEquals(if(skillLevel>=16)1 else 0,messages.count{"stage=activation" in it})
    verify(condition,times(3)).isMet(match(Dispatcher::class.java),match(ProvidedHolder::class.java))
    if(skillLevel>=16)api.verify({NumberUtils.evaluateExpression(eq("2 + 2 * %level%"),match(PlaceholderContext::class.java))},times(3))
    else api.verify({NumberUtils.evaluateExpression(anyString(),match(PlaceholderContext::class.java))},never())
   } finally {
    OriginalRuntime.reset();runtimeField("owner").set(OriginalRuntime,previousOwner);burstOwner.set(NativeBurstProgression,previousBurst)
    while(providers.size>providerCount)providers.removeAt(providers.lastIndex)
    restorePlaceholder?.invoke()
    lfField.set(null,previousLF)
   }
  }
  }
 }
 @Test fun `delegated real hit confirms dispatches native level placeholder and activates on third hit`()=flow(50)
 @Test fun `delegated holder cannot bypass native skill condition`()=flow(15)
}
