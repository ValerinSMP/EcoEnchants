package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchant.*
import com.willfp.ecoenchants.enchant.impl.EcoEnchantBase
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.effects.EffectList
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.scoreboard.Scoreboard
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID

class DelegatedCombatPolicyTest {
 private class Delegated(private val base:EcoEnchant): EcoEnchant by base
 internal class Fixture {
  lateinit var bukkit: org.mockito.MockedStatic<Bukkit>
  val owner=mock(EcoEnchantsPlugin::class.java)
  val config=mock(Config::class.java)
  val actor=mock(Player::class.java)
  val target=mock(LivingEntity::class.java)
  val world=mock(World::class.java)
  val board=mock(Scoreboard::class.java)
  val registered=EcoEnchants.javaClass.superclass.getDeclaredField("registry").apply{isAccessible=true}
  @Suppress("UNCHECKED_CAST") val registry=registered.get(EcoEnchants) as Registry<EcoEnchant>
  val base=base();val delegated:EcoEnchant=Delegated(base)
  fun base():EcoEnchantBase {
   val value=mock(EcoEnchantBase::class.java)
   `when`(value.id).thenReturn("eco_resonante");`when`(value.getID()).thenReturn("eco_resonante")
   `when`(value.plugin).thenReturn(owner);`when`(value.config).thenReturn(config)
   `when`(value.combatTargetPolicy).thenAnswer{CombatTargetPolicy.compile(config){error(it)}}
   val levels=mutableMapOf<Int,EcoEnchantLevel>()
   `when`(value.getLevel(anyInt())).thenAnswer { call -> levels.getOrPut(call.getArgument(0)){EcoEnchantLevel(value,call.getArgument(0),EffectList(emptyList()),ConditionList(emptyList()))} }
   return value
  }
  fun run(body:Fixture.()->Unit) {
   val field=Class.forName("com.willfp.ecoenchants.EcoEnchantsPluginKt").getDeclaredField("plugin").apply{isAccessible=true};val previous=field.get(null)
   `when`(owner.isEnabled).thenReturn(true);`when`(owner.createNamespacedKey(anyString())).thenAnswer{NamespacedKey("ecoenchants",it.getArgument(0))}
   `when`(config.getStringOrNull("combat-mode")).thenReturn("both")
   `when`(world.uid).thenReturn(UUID.randomUUID());`when`(world.pvp).thenReturn(true)
   `when`(actor.uniqueId).thenReturn(UUID.randomUUID());`when`(actor.world).thenReturn(world);`when`(actor.isOnline).thenReturn(true)
   `when`(actor.gameMode).thenReturn(GameMode.CREATIVE);`when`(actor.name).thenReturn("Actor");`when`(actor.scoreboard).thenReturn(board)
   `when`(target.uniqueId).thenReturn(UUID.randomUUID());`when`(target.world).thenReturn(world);`when`(target.type).thenReturn(EntityType.ZOMBIE)
   `when`(target.isValid).thenReturn(true);`when`(target.persistentDataContainer).thenReturn(mock(PersistentDataContainer::class.java))
   field.set(null,owner);registry.register(delegated)
   mockStatic(Bukkit::class.java).use{bukkit -> this.bukkit=bukkit; mockStatic(AntigriefManager::class.java).use{protection ->
    bukkit.`when`<Boolean>{Bukkit.isPrimaryThread()}.thenReturn(true)
    bukkit.`when`<Player>{Bukkit.getPlayer(actor.uniqueId)}.thenReturn(actor)
    protection.`when`<Boolean>{AntigriefManager.canInjure(actor,target)}.thenReturn(true)
    try { body() } finally { registry.remove(delegated);registry.get("eco_resonante")?.let{registry.remove(it)};field.set(null,previous) }
   }}
  }
 }
 @Test fun `registered delegated enchant reaches its real native holder and preserves both policy decisions`()=Fixture().run {
  assertFalse(delegated is EcoEnchantBase)
  assertTrue(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
  assertTrue(delegated.getLevel(3).allowsCombatTarget(actor,target,false,false))
  `when`(config.getStringOrNull("combat-mode")).thenReturn("pvp")
  assertFalse(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
  `when`(config.getStringOrNull("combat-mode")).thenReturn("both")
  `when`(target.isInvulnerable).thenReturn(true);assertFalse(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
  `when`(target.isInvulnerable).thenReturn(false);`when`(target.hasMetadata("NPC")).thenReturn(true)
  assertFalse(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
 }
 @Test fun `replacement and removal reject old holders including posthumous effects without a policy registry`()=Fixture().run {
  val old=delegated.getLevel(3);assertTrue(old.allowsCombatTarget(actor,target,false,false))
  registry.remove(delegated);val replacement=Delegated(base());registry.register(replacement)
  assertFalse(old.allowsCombatTarget(actor,target,false,false));assertFalse(old.allowsPosthumousCombatTarget(actor,target))
  assertTrue(replacement.getLevel(3).allowsCombatTarget(actor,target,false,false))
  assertTrue(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
  registry.remove(replacement);assertFalse(old.allowsCombatTarget(actor,target,false,false))
  assertFalse(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
 }
 @Test fun `disabled owning plugin remains excluded through delegation`()=Fixture().run {
  `when`(owner.isEnabled).thenReturn(false);assertFalse(OriginalRuntime.eligibleFor("eco_resonante",actor,target))
 }
}
