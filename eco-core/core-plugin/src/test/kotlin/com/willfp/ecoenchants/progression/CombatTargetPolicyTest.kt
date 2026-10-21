package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import org.bukkit.scoreboard.Team
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.mockito.Answers
import java.util.UUID

@Suppress("UNCHECKED_CAST")
private fun targetConfig(values: Map<String, Any?>): Config = mock(Config::class.java) { call ->
    val path = call.arguments.firstOrNull() as? String
    val value = values[path]
    when (call.method.name) {
        "has" -> values.containsKey(path)
        "getKeys" -> values.keys.toList()
        "getStringOrNull" -> value as? String
        "getBoolOrNull" -> value as? Boolean
        "getStringsOrNull" -> (value as? List<*>)?.takeIf { it.all { v -> v is String } }
        "getSubsectionOrNull" -> (value as? Map<String, Any?>)?.let { targetConfig(it) }
        else -> Answers.RETURNS_DEFAULTS.answer(call)
    }
}
class CombatTargetPolicyTest {
    private fun policy(values: Map<String, Any?> = emptyMap(), errors: MutableList<String> = mutableListOf()) =
        CombatTargetPolicy.compile(targetConfig(values)) { errors.add(it) }
    @Test fun `legacy YAML inherits all living attackable species and PvP`() {
        val policy = policy()
        for (type in listOf(EntityType.COW, EntityType.SHEEP, EntityType.ZOMBIE, EntityType.PLAYER, EntityType.WARDEN))
            assertTrue(policy.selects(type, type == EntityType.PLAYER), type.name)
    }
    @Test fun `configured species narrow both primary and secondary selection`() {
        val policy = policy(mapOf("combat-targets" to mapOf("include-types" to listOf("cow", "minecraft:sheep", "PLAYER"), "exclude-types" to listOf("SHEEP"))))
        assertTrue(policy.selects(EntityType.COW, false)); assertTrue(policy.selects(EntityType.PLAYER, true))
        assertFalse(policy.selects(EntityType.SHEEP, false)); assertFalse(policy.selects(EntityType.ZOMBIE, false))
    }
    @Test fun `combat mode cannot be bypassed and exclusion wins`() {
        val policy = policy(mapOf("combat-mode" to "PvE", "combat-targets" to mapOf("pvp" to true, "exclude-types" to listOf("COW"))))
        assertFalse(policy.selects(EntityType.PLAYER, true)); assertFalse(policy.selects(EntityType.COW, false)); assertTrue(policy.selects(EntityType.ZOMBIE, false))
    }
    @Test fun `invalid and contradictory configuration diagnoses and fails closed`() {
        val bad = listOf(
            mapOf("pvp" to "yes"), mapOf("pvp" to false, "pve" to false), mapOf("include-types" to listOf("BOGUS")),
            mapOf("include-types" to listOf("COW"), "exclude-types" to listOf("COW")), mapOf("include-types" to listOf("ARMOR_STAND")),
            mapOf("include-types" to "cow"), mapOf("includes" to listOf("COW")), mapOf("include-types" to emptyList<String>())
        )
        for (section in bad) {
            val errors = mutableListOf<String>(); val policy = policy(mapOf("combat-targets" to section), errors)
            assertTrue(errors.isNotEmpty(), section.toString()); assertFalse(policy.selects(EntityType.COW, false)); assertFalse(policy.selects(EntityType.PLAYER, true))
        }
    }
    @Test fun `reload replaces prepared choices without altering prior immutable policy`() {
        val old = policy(); val replacement = policy(mapOf("combat-targets" to mapOf("pvp" to false)))
        assertTrue(old.selects(EntityType.PLAYER, true)); assertFalse(replacement.selects(EntityType.PLAYER, true))
    }
    private fun fixture(type: EntityType, test: (CombatTargetPolicy, Player, LivingEntity, World, Scoreboard) -> Unit) {
        val actor = mock(Player::class.java); val target = mock(if (type == EntityType.PLAYER) Player::class.java else LivingEntity::class.java)
        val world = mock(World::class.java); val board = mock(Scoreboard::class.java); val manager = mock(ScoreboardManager::class.java)
        `when`(actor.uniqueId).thenReturn(UUID.randomUUID()); `when`(target.uniqueId).thenReturn(UUID.randomUUID())
        `when`(actor.isOnline).thenReturn(true); `when`(actor.world).thenReturn(world); `when`(actor.scoreboard).thenReturn(board); `when`(actor.name).thenReturn("actor")
        `when`(target.type).thenReturn(type); `when`(target.world).thenReturn(world); `when`(target.isValid).thenReturn(true)
        `when`(target.persistentDataContainer).thenReturn(mock(PersistentDataContainer::class.java)); `when`(manager.mainScoreboard).thenReturn(board)
        if (target is Player) { `when`(target.isOnline).thenReturn(true); `when`(target.gameMode).thenReturn(GameMode.SURVIVAL); `when`(target.name).thenReturn("target") }
        `when`(world.pvp).thenReturn(true)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
            bukkit.`when`<Player> { Bukkit.getPlayer(actor.uniqueId) }.thenReturn(actor)
            bukkit.`when`<ScoreboardManager> { Bukkit.getScoreboardManager() }.thenReturn(manager)
            mockStatic(AntigriefManager::class.java).use { protection ->
                protection.`when`<Boolean> { AntigriefManager.canInjure(actor, target) }.thenReturn(true)
                test(policy(), actor, target, world, board)
            }
        }
    }
    @Test fun `cow sheep zombie player and boss are actual native recipients not Enemy-only`() {
        for (type in listOf(EntityType.COW, EntityType.SHEEP, EntityType.ZOMBIE, EntityType.PLAYER, EntityType.WARDEN)) fixture(type) { policy, actor, target, _, _ ->
            assertTrue(policy.allows(actor, target)); verify(target, never()).entitySpawnReason; verify(target, never()).scoreboardTags
            verify(target, never()).customName(); verify(target.persistentDataContainer, never()).keys
        }
    }
    @Test fun `real protections self invulnerability death and NPC always deny`() = fixture(EntityType.COW) { policy, actor, target, _, _ ->
        assertFalse(policy.allows(actor, actor)); `when`(target.isInvulnerable).thenReturn(true); assertFalse(policy.allows(actor, target))
        `when`(target.isInvulnerable).thenReturn(false); `when`(target.hasMetadata("NPC")).thenReturn(true); assertFalse(policy.allows(actor, target))
        `when`(target.hasMetadata("NPC")).thenReturn(false); `when`(target.isDead).thenReturn(true); assertFalse(policy.allows(actor, target))
        assertTrue(policy.allows(actor, target, allowDead = true)); `when`(target.isDead).thenReturn(false)
        `when`(AntigriefManager.canInjure(actor, target)).thenReturn(false); assertFalse(policy.allows(actor, target))
    }
    @Test fun `PvP off creative spectator and ally deny`() = fixture(EntityType.PLAYER) { policy, actor, target, world, board ->
        target as Player
        `when`(world.pvp).thenReturn(false); assertFalse(policy.allows(actor, target)); `when`(world.pvp).thenReturn(true)
        for (mode in listOf(GameMode.CREATIVE, GameMode.SPECTATOR)) { `when`(target.gameMode).thenReturn(mode); assertFalse(policy.allows(actor, target)) }
        `when`(target.gameMode).thenReturn(GameMode.SURVIVAL)
        val team = mock(Team::class.java); `when`(board.getEntryTeam(actor.name)).thenReturn(team); `when`(board.getEntryTeam(target.name)).thenReturn(team)
        assertFalse(policy.allows(actor, target))
    }
    @Test fun `known owned pet marker denies without rejecting arbitrary PDC`() = fixture(EntityType.COW) { policy, actor, target, _, _ ->
        `when`(target.persistentDataContainer.has(NamespacedKey("vpets", "pet-owner"), PersistentDataType.STRING)).thenReturn(true)
        assertFalse(policy.allows(actor, target))
    }
    @Test fun `posthumous recipient permission does not authorize new live attacks by dead actors`() = fixture(EntityType.COW) { policy, actor, target, _, _ ->
        `when`(actor.isDead).thenReturn(true)
        assertFalse(policy.allows(actor, target)); assertTrue(policy.allows(actor, target, posthumous = true))
        `when`(target.isInvulnerable).thenReturn(true); assertFalse(policy.allows(actor, target, posthumous = true))
    }
    @Test fun `mode narrowing emits a diagnostic instead of granting a bypass`() {
        val errors = mutableListOf<String>()
        val restricted = policy(mapOf("combat-mode" to "pve", "combat-targets" to mapOf("pvp" to true)), errors)
        assertTrue(errors.isNotEmpty()); assertFalse(restricted.selects(EntityType.PLAYER, true)); assertTrue(restricted.selects(EntityType.COW, false))
    }

}
