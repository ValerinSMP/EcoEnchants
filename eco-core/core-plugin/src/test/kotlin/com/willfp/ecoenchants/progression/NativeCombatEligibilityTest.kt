package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.EcoEnchants
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.plugin.PluginManager
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.event.entity.CreatureSpawnEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.util.UUID

class NativeCombatEligibilityTest {
 private fun fixture(body:(Player,Player,Config,Scoreboard)->Unit){
  val enchant=mock(com.willfp.ecoenchants.enchant.impl.EcoEnchantBase::class.java);val config=mock(Config::class.java)
  `when`(enchant.getID()).thenReturn("sello_glacial");`when`(enchant.id).thenReturn("sello_glacial");`when`(enchant.config).thenReturn(config)
  // This fixture recompiles on policy access to simulate category replacement for each legacy mode assertion.
  `when`(enchant.combatTargetPolicy).thenAnswer { CombatTargetPolicy.compile(config) { } }
  val field=EcoEnchants.javaClass.superclass.getDeclaredField("registry").apply{isAccessible=true}
  @Suppress("UNCHECKED_CAST") val registry=field.get(EcoEnchants) as Registry<EcoEnchant>
  registry.register(enchant)
  val world=mock(World::class.java);`when`(world.pvp).thenReturn(true)
  val owner=mock(Player::class.java);val target=mock(Player::class.java);val board=mock(Scoreboard::class.java)
  for(p in listOf(owner,target)){
   `when`(p.type).thenReturn(EntityType.PLAYER);`when`(p.persistentDataContainer).thenReturn(mock(PersistentDataContainer::class.java))
   `when`(p.uniqueId).thenReturn(UUID.randomUUID());`when`(p.world).thenReturn(world);`when`(p.isValid).thenReturn(true)
   `when`(p.isOnline).thenReturn(true);`when`(p.gameMode).thenReturn(GameMode.SURVIVAL);`when`(p.scoreboardTags).thenReturn(emptySet());`when`(p.scoreboard).thenReturn(board)
  }
  `when`(owner.name).thenReturn("Owner");`when`(target.name).thenReturn("Target")
  mockStatic(Bukkit::class.java).use{bukkit->mockStatic(AntigriefManager::class.java).use{protections->
   bukkit.`when`<Boolean>{Bukkit.isPrimaryThread()}.thenReturn(true)
   bukkit.`when`<Player>{Bukkit.getPlayer(owner.uniqueId)}.thenReturn(owner)
   val manager=mock(org.bukkit.scoreboard.ScoreboardManager::class.java);`when`(manager.mainScoreboard).thenReturn(board)
   bukkit.`when`<org.bukkit.scoreboard.ScoreboardManager>{Bukkit.getScoreboardManager()}.thenReturn(manager)
   bukkit.`when`<PluginManager>{Bukkit.getPluginManager()}.thenReturn(mock(PluginManager::class.java))
   protections.`when`<Boolean>{AntigriefManager.canInjure(owner,target)}.thenReturn(true)
   protections.`when`<Boolean>{AntigriefManager.canInjure(target,owner)}.thenReturn(false)
   try{body(owner,target,config,board)}finally{registry.remove(enchant)}
  }}
 }
 @Test fun `both allows vanilla player PvP without rejecting ordinary player PDC and pve keeps it out`()=fixture{owner,target,config,_->
  `when`(config.getStringOrNull("combat-mode")).thenReturn("both");assertTrue(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
  verify(target.persistentDataContainer,never()).keys
  `when`(config.getStringOrNull("combat-mode")).thenReturn("pve");assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
  `when`(config.getStringOrNull("combat-mode")).thenReturn("pvp");assertTrue(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
  `when`(config.getStringOrNull("combat-mode")).thenReturn("invalid");assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
 }
 @Test fun `PvP never bypasses teams NPC invulnerability game mode protection or world toggle`()=fixture{owner,target,config,board->
  `when`(config.getStringOrNull("combat-mode")).thenReturn("both")
  assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target,defensive=true)) // Incoming permission is attacker -> wearer.
  `when`(target.hasMetadata("NPC")).thenReturn(true);assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target));`when`(target.hasMetadata("NPC")).thenReturn(false)
  `when`(target.isInvulnerable).thenReturn(true);assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target));`when`(target.isInvulnerable).thenReturn(false)
  `when`(target.gameMode).thenReturn(GameMode.CREATIVE);assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target));`when`(target.gameMode).thenReturn(GameMode.SURVIVAL)
  val team=mock(Team::class.java);`when`(board.getEntryTeam(owner.name)).thenReturn(team);`when`(board.getEntryTeam(target.name)).thenReturn(team)
  assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target));`when`(board.getEntryTeam(target.name)).thenReturn(null)
  `when`(target.world.pvp).thenReturn(false);assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
 }
 @Test fun `PvE admits every spawn origin while preserving the player versus mob mode`()=fixture{owner,_,config,_->
  `when`(config.getStringOrNull("combat-mode")).thenReturn("pve")
  val target=mock(Zombie::class.java);val pdc=mock(PersistentDataContainer::class.java);val world=owner.world
  `when`(target.type).thenReturn(EntityType.ZOMBIE)
  `when`(target.uniqueId).thenReturn(UUID.randomUUID());`when`(target.world).thenReturn(world);`when`(target.isValid).thenReturn(true)
  `when`(target.persistentDataContainer).thenReturn(pdc);`when`(pdc.keys).thenReturn(emptySet());`when`(target.scoreboardTags).thenReturn(emptySet())
  // Register this additional exact protection outcome in the existing static mock.
  `when`(AntigriefManager.canInjure(owner,target)).thenReturn(true)
  for(reason in listOf(CreatureSpawnEvent.SpawnReason.NATURAL,CreatureSpawnEvent.SpawnReason.COMMAND,CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,CreatureSpawnEvent.SpawnReason.DISPENSE_EGG,CreatureSpawnEvent.SpawnReason.BREEDING)){
   `when`(target.entitySpawnReason).thenReturn(reason);assertTrue(OriginalRuntime.eligibleFor("sello_glacial",owner,target),reason.name)
  }
  `when`(target.entitySpawnReason).thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER);assertTrue(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
  `when`(target.entitySpawnReason).thenReturn(CreatureSpawnEvent.SpawnReason.COMMAND);`when`(config.getStringOrNull("combat-mode")).thenReturn("pvp");assertFalse(OriginalRuntime.eligibleFor("sello_glacial",owner,target))
 }
 @Test fun `kill filter rejects forced notifications and cancelled deaths`() {
  val player=mock(Player::class.java);val victim=mock(Zombie::class.java)
  val forced=mock(com.willfp.libreforge.triggers.TriggerData::class.java)
  assertFalse(FilterGenuineKill.isMet(forced,true,com.willfp.libreforge.NoCompileData))
  val event=mock(com.willfp.eco.core.events.EntityDeathByEntityEvent::class.java);val death=mock(org.bukkit.event.entity.EntityDeathEvent::class.java)
  `when`(event.killer).thenReturn(player);`when`(event.victim).thenReturn(victim);`when`(event.deathEvent).thenReturn(death)
  val actual=mock(com.willfp.libreforge.triggers.TriggerData::class.java)
  `when`(actual.player).thenReturn(player);`when`(actual.victim).thenReturn(victim);`when`(actual.event).thenReturn(event)
  assertTrue(FilterGenuineKill.isMet(actual,true,com.willfp.libreforge.NoCompileData))
  `when`(death.isCancelled).thenReturn(true);assertFalse(FilterGenuineKill.isMet(actual,true,com.willfp.libreforge.NoCompileData))
 }
}
