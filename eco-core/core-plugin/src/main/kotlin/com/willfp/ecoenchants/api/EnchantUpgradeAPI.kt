package com.willfp.ecoenchants.api

import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.wrap
import com.willfp.ecoenchants.enchant.conflictsWithDeep
import com.willfp.libreforge.SimpleProvidedHolder
import com.willfp.libreforge.refreshHolders
import com.willfp.libreforge.toDispatcher
import org.bukkit.Bukkit
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.UUID

/** MAIN-only native validation/application. No prices, payment, XP, synthetic EnchantItemEvent or durable purchase proof. */
object EnchantUpgradeAPI {
    const val CONTRACT_VERSION = 1
    const val REPLACEMENT_CONTRACT_VERSION = 1
    const val REMOVAL_CONTRACT_VERSION = 1
    enum class LevelPolicy { NATIVE_MAXIMUM, VANILLA_ONE_ABOVE }
    enum class Status { APPLIED, APPLIED_CALLBACK_REVIEW, UNKNOWN_APPLY_REVIEW, DENIED, ITEM_CHANGED, CANCELLED }
    data class Validation(val allowed: Boolean, val reason: String?, val currentLevel: Int, val targetLevel: Int)
    data class Result(val status: Status, val reason: String?, val operationId: UUID)
    @JvmStatic fun validateUpgrade(player: Player, item: ItemStack, enchantment: Enchantment, expectedFrom: Int, target: Int, policy: LevelPolicy): Validation {
        check(Bukkit.isPrimaryThread())
        fun denied(reason: String) = Validation(false, reason, expectedFrom, target)
        if (!player.isOnline || item.type.isAir || item.amount != 1) return denied("player_or_single_item_unavailable")
        val meta = item.itemMeta ?: return denied("metadata_absent")
        val current = if (meta is EnchantmentStorageMeta) meta.getStoredEnchantLevel(enchantment) else meta.getEnchantLevel(enchantment)
        if (current != expectedFrom) return denied("original_level_changed")
        val native = enchantment.wrap()
        if (!upgradeLevelAllowed(current,target,native.maximumLevel,native is EcoEnchant,policy)) return denied("invalid_upgrade_level")
        val validationItem = item.clone()
        val validationMeta = validationItem.itemMeta ?: return denied("metadata_absent")
        if (validationMeta is EnchantmentStorageMeta) validationMeta.removeStoredEnchant(enchantment) else validationMeta.removeEnchant(enchantment)
        validationItem.itemMeta = validationMeta
        if (!native.canEnchantItem(validationItem)) return denied("native_target_conflict_prerequisite_or_limit")
        if (native is EcoEnchant) {
            if (!cachedSkillRequirementsKnown(player,native)) return denied("skill_requirement_unavailable")
            // Most upstream conditions control WHEN an effect activates (health, sneaking, etc.).
            // Only the existing native skill unlock is an acquisition requirement; retain all others on their holders.
            val dispatcher = player.toDispatcher()
            val holder = SimpleProvidedHolder(native.getLevel(target))
            if (!native.conditions.filter { isPurchaseUnlock(it.condition.id) }.all { it.isMet(dispatcher,holder) }) return denied("native_skill_unlock_not_met")
        }
        return Validation(true,null,current,target)
    }
    @JvmStatic fun applyUpgrade(player: Player, slot: Int, expectedItem: ItemStack, enchantment: Enchantment,
        expectedFrom: Int, target: Int, policy: LevelPolicy, operationId: UUID): Result {
        check(Bukkit.isPrimaryThread())
        if (slot !in 0 until player.inventory.storageContents.size) return Result(Status.DENIED,"invalid_storage_slot",operationId)
        if (expectedFrom < 0 || target <= expectedFrom || target > 32767) return Result(Status.DENIED,"invalid_upgrade_level",operationId)
        val original = expectedItem.clone()
        val upgraded = original.clone()
        val meta = upgraded.itemMeta ?: return Result(Status.DENIED,"metadata_absent",operationId)
        val changed = if (meta is EnchantmentStorageMeta) meta.addStoredEnchant(enchantment,target,true) else meta.addEnchant(enchantment,target,true)
        if (!changed) return Result(Status.DENIED,"native_metadata_not_changed",operationId)
        upgraded.itemMeta = meta
        val outcome = executeUpgrade(
            unchanged = { player.inventory.getItem(slot) == original },
            validate = { validateUpgrade(player,original,enchantment,expectedFrom,target,policy).reason },
            before = {
                val event = EnchantUpgradePrepareEvent(player,operationId,enchantment,expectedFrom,target,original)
                Bukkit.getPluginManager().callEvent(event)
                !event.isCancelled
            },
            replace = { player.inventory.setItem(slot,upgraded) },
            after = {
                player.toDispatcher().refreshHolders()
                Bukkit.getPluginManager().callEvent(EnchantUpgradeAppliedEvent(player,operationId,enchantment,expectedFrom,target,original,upgraded))
            }
        )
        return Result(outcome.first,outcome.second,operationId)
    }
    /** MAIN-only item-in-custody inspection; empty conflicts do not authorize an upgrade. */
    @JvmStatic fun getEffectiveConflicts(item: ItemStack, target: Enchantment): List<Removal> {
        check(Bukkit.isPrimaryThread())
        val meta = item.itemMeta ?: return emptyList()
        val entries = meta.enchants.map { Triple(it.key, it.value, false) } +
            if (meta is EnchantmentStorageMeta) meta.storedEnchants.map { Triple(it.key, it.value, true) } else emptyList()
        val conflicts = entries.filter { it.first.key != target.key && it.first.conflictsWithDeep(target) }
            .map { Removal(it.first.key.toString(), it.second, it.third) }
            .sortedWith(compareBy({ it.enchantmentKey }, { it.stored }, { it.level }))
        return java.util.Collections.unmodifiableList(ArrayList(conflicts))
    }

    /** Exact removal list computed by EcoEnchants, never a caller bypass/ignore flag. */
    data class Removal(val enchantmentKey: String, val level: Int, val stored: Boolean)
    class ReplacementPlan internal constructor(
        val playerId: UUID, val slot: Int, expected: ItemStack, val enchantmentKey: String,
        val currentLevel: Int, val targetLevel: Int, val levelPolicy: LevelPolicy,
        val operationId: UUID, removals: List<Removal>
    ) {
        private val original = expected.clone()
        val removals: List<Removal> = java.util.Collections.unmodifiableList(ArrayList(removals))
        fun getExpectedItem(): ItemStack = original.clone()
    }
    data class ReplacementValidation(val allowed: Boolean, val reason: String?, val plan: ReplacementPlan?)

    @JvmStatic fun planReplacement(player: Player, slot: Int, expectedItem: ItemStack,
        enchantment: Enchantment, expectedFrom: Int, target: Int, policy: LevelPolicy,
        operationId: UUID): ReplacementValidation {
        check(Bukkit.isPrimaryThread())
        fun deny(reason: String) = ReplacementValidation(false, reason, null)
        if (slot !in 0 until player.inventory.storageContents.size) return deny("invalid_storage_slot")
        if (player.inventory.getItem(slot) != expectedItem) return deny("original_item_changed")
        if (!player.isOnline || expectedItem.type.isAir || expectedItem.amount != 1) return deny("player_or_single_item_unavailable")
        val meta = expectedItem.itemMeta ?: return deny("metadata_absent")
        val entries = meta.enchants.map { Triple(it.key,it.value,false) } +
            if(meta is EnchantmentStorageMeta) meta.storedEnchants.map { Triple(it.key,it.value,true) } else emptyList()
        val removals = getEffectiveConflicts(expectedItem, enchantment)
        val removedEntries = removals.toSet()
        val conflicts = entries.filter { Removal(it.first.key.toString(), it.second, it.third) in removedEntries }
        for ((existing, _, _) in conflicts) {
            val wrapped = existing.wrap()
            if (reservedReplacementConflict(existing.key.key, wrapped is EcoEnchant, wrapped.type.id))
                return deny("reserved_conflict:${existing.key}")
        }
        val candidate = expectedItem.clone()
        val clean = candidate.itemMeta ?: return deny("metadata_absent")
        for ((existing, _, stored) in conflicts) {
            if(stored) (clean as EnchantmentStorageMeta).removeStoredEnchant(existing) else clean.removeEnchant(existing)
        }
        candidate.itemMeta = clean
        val validation = validateUpgrade(player,candidate,enchantment,expectedFrom,target,policy)
        if(!validation.allowed) return deny(validation.reason ?: "native_validation_failed")
        val resulting = (entries.map { it.first }.filter { old -> removals.none { it.enchantmentKey == old.key.toString() } } + enchantment).distinctBy { it.key }
        for(existing in resulting) {
            val native=existing.wrap()
            if(native is EcoEnchant && !native.hasRequiredEnchantments(resulting)) return deny("remaining_prerequisite_missing:${existing.key}")
            if(resulting.any { it.key != existing.key && existing.conflictsWithDeep(it) }) return deny("resulting_conflict:${existing.key}")
        }
        return ReplacementValidation(true,null,ReplacementPlan(player.uniqueId,slot,expectedItem,enchantment.key.toString(),expectedFrom,target,policy,operationId,removals))
    }

    /** Recomputes the exact plan before and after the existing cancellable native callback. */
    @JvmStatic fun applyReplacement(player: Player, plan: ReplacementPlan): Result {
        check(Bukkit.isPrimaryThread())
        if(plan.slot !in 0 until player.inventory.storageContents.size) return Result(Status.DENIED,"invalid_storage_slot",plan.operationId)
        if(player.uniqueId != plan.playerId) return Result(Status.DENIED,"plan_player_changed",plan.operationId)
        val key=org.bukkit.NamespacedKey.fromString(plan.enchantmentKey)
        val enchantment=key?.let { Enchantment.getByKey(it) } ?: return Result(Status.DENIED,"enchantment_unavailable",plan.operationId)
        val original=plan.getExpectedItem()
        val upgraded=original.clone()
        val meta=upgraded.itemMeta ?: return Result(Status.DENIED,"metadata_absent",plan.operationId)
        for(removal in plan.removals) {
            val removed=org.bukkit.NamespacedKey.fromString(removal.enchantmentKey)?.let { Enchantment.getByKey(it) }
                ?: return Result(Status.DENIED,"removal_definition_unavailable",plan.operationId)
            if(removal.stored) (meta as? EnchantmentStorageMeta)?.removeStoredEnchant(removed) else meta.removeEnchant(removed)
        }
        val changed=if(meta is EnchantmentStorageMeta) meta.addStoredEnchant(enchantment,plan.targetLevel,true) else meta.addEnchant(enchantment,plan.targetLevel,true)
        if(!changed) return Result(Status.DENIED,"native_metadata_not_changed",plan.operationId)
        upgraded.itemMeta=meta
        val outcome=executeUpgrade(
            unchanged={player.inventory.getItem(plan.slot)==original},
            validate={
                val fresh=planReplacement(player,plan.slot,original,enchantment,plan.currentLevel,plan.targetLevel,plan.levelPolicy,plan.operationId)
                if(!fresh.allowed) fresh.reason else if(fresh.plan?.removals != plan.removals) "replacement_plan_changed" else null
            },
            before={
                val event=EnchantUpgradePrepareEvent(player,plan.operationId,enchantment,plan.currentLevel,plan.targetLevel,original,plan.removals)
                Bukkit.getPluginManager().callEvent(event);!event.isCancelled
            },
            replace={player.inventory.setItem(plan.slot,upgraded)},
            after={
                player.toDispatcher().refreshHolders()
                Bukkit.getPluginManager().callEvent(EnchantUpgradeAppliedEvent(player,plan.operationId,enchantment,plan.currentLevel,plan.targetLevel,original,upgraded))
                check(player.inventory.getItem(plan.slot) == upgraded) { "Item changed after native application; review delivery without reapplying" }
            }
        )
        return Result(outcome.first,outcome.second,plan.operationId)
    }

    /** Session-bound, defensive before image. operationId correlates; it is not durable proof. */
    class RemovalPlan internal constructor(
        private val connection: Player, val slot: Int, expected: ItemStack,
        val removal: Removal, val operationId: UUID
    ) {
        val playerId: UUID = connection.uniqueId
        private val original = expected.clone()
        fun getExpectedItem(): ItemStack = original.clone()
        internal fun matchesSession(player: Player): Boolean =
            player === connection && Bukkit.getPlayer(playerId) === connection
    }
    data class RemovalValidation(val allowed: Boolean, val reason: String?, val plan: RemovalPlan?)

    /** No skill unlock/payment/XP check: only native removal restrictions and remaining prerequisites. */
    @JvmStatic fun planRemoval(player: Player, slot: Int, expectedItem: ItemStack,
        enchantment: Enchantment, expectedLevel: Int, operationId: UUID): RemovalValidation {
        check(Bukkit.isPrimaryThread())
        fun deny(reason: String) = RemovalValidation(false,reason,null)
        if (!player.isOnline || player.isDead || Bukkit.getPlayer(player.uniqueId) !== player) return deny("player_session_unavailable")
        if (slot !in 0 until player.inventory.storageContents.size) return deny("invalid_storage_slot")
        if (expectedItem.type.isAir || expectedItem.amount != 1) return deny("single_item_unavailable")
        if (player.inventory.getItem(slot) != expectedItem) return deny("original_item_changed")
        val meta = expectedItem.itemMeta ?: return deny("metadata_absent")
        val direct = meta.getEnchantLevel(enchantment)
        val stored = (meta as? EnchantmentStorageMeta)?.getStoredEnchantLevel(enchantment) ?: 0
        if (direct > 0 && stored > 0) return deny("ambiguous_enchantment_storage")
        val current = if (stored > 0) stored else direct
        if (expectedLevel <= 0 || current != expectedLevel) return deny("original_level_changed_or_absent")
        if (Enchantment.getByKey(enchantment.key) == null) return deny("enchantment_unavailable")
        val native = enchantment.wrap()
        if (reservedReplacementConflict(enchantment.key.key,native is EcoEnchant,native.type.id)) return deny("reserved_removal:${enchantment.key}")
        val candidate = removalCandidate(expectedItem,enchantment,stored > 0) ?: return deny("native_metadata_not_changed")
        val remainingMeta = candidate.itemMeta ?: return deny("metadata_absent")
        val remaining = (remainingMeta.enchants.keys +
            if (remainingMeta is EnchantmentStorageMeta) remainingMeta.storedEnchants.keys else emptySet()).distinctBy { it.key }
        for (existing in remaining) {
            val remainingNative = existing.wrap()
            if (remainingNative is EcoEnchant && !remainingNative.hasRequiredEnchantments(remaining))
                return deny("remaining_prerequisite_missing:${existing.key}")
        }
        return RemovalValidation(true,null,RemovalPlan(player,slot,expectedItem,Removal(enchantment.key.toString(),current,stored > 0),operationId))
    }

    /** Revalidates after the cancellable removal event; never emits upgrade/purchase/XP callbacks. */
    @JvmStatic fun applyRemoval(player: Player, plan: RemovalPlan): Result {
        check(Bukkit.isPrimaryThread())
        if (!plan.matchesSession(player)) return Result(Status.DENIED,"plan_session_changed",plan.operationId)
        if (plan.slot !in 0 until player.inventory.storageContents.size) return Result(Status.DENIED,"invalid_storage_slot",plan.operationId)
        val key = org.bukkit.NamespacedKey.fromString(plan.removal.enchantmentKey)
        val enchantment = key?.let { Enchantment.getByKey(it) } ?: return Result(Status.DENIED,"enchantment_unavailable",plan.operationId)
        val original = plan.getExpectedItem()
        val removed = removalCandidate(original,enchantment,plan.removal.stored)
            ?: return Result(Status.DENIED,"native_metadata_not_changed",plan.operationId)
        val outcome = try {
            executeUpgrade(
                unchanged = { plan.matchesSession(player) && player.inventory.getItem(plan.slot) == original },
                validate = {
                    if (!plan.matchesSession(player)) "plan_session_changed" else {
                        val fresh = planRemoval(player,plan.slot,original,enchantment,plan.removal.level,plan.operationId)
                        if (!fresh.allowed) fresh.reason else if (fresh.plan?.removal != plan.removal) "removal_plan_changed" else null
                    }
                },
                before = {
                    val event = EnchantRemovalPrepareEvent(player,plan.operationId,plan.removal,original)
                    Bukkit.getPluginManager().callEvent(event)
                    !event.isCancelled
                },
                replace = { player.inventory.setItem(plan.slot,removed) },
                after = {
                    player.toDispatcher().refreshHolders()
                    Bukkit.getPluginManager().callEvent(EnchantRemovalAppliedEvent(player,plan.operationId,plan.removal,original,removed))
                    check(plan.matchesSession(player) && player.inventory.getItem(plan.slot) == removed) { "Removal delivery changed; review without reapplying" }
                }
            )
        } catch (_: Exception) {
            Status.UNKNOWN_APPLY_REVIEW to "removal_validation_or_prepare_uncertain_do_not_reapply"
        }
        return Result(outcome.first,outcome.second,plan.operationId)
    }

    private fun cachedSkillRequirementsKnown(player: Player,enchant: EcoEnchant): Boolean {
        val skills = enchant.config.getSubsections("conditions").filter { it.getString("id") == "has_skill_level" }.map { it.getString("args.skill") }.toSet()
        if (skills.isEmpty()) return true
        val plugin = Bukkit.getPluginManager().getPlugin("EcoSkills")?.takeIf { it.isEnabled } ?: return false
        val snapshot = try {
            val api = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI",false,plugin.javaClass.classLoader)
            api.getMethod("getCachedSnapshot",UUID::class.java).invoke(null,player.uniqueId) as? Map<*, *>
        } catch (_: ReflectiveOperationException) { null } catch (_: LinkageError) { null } ?: return false
        if (snapshot["schemaVersion"] != 1 || snapshot["playerUuid"] != player.uniqueId.toString() || snapshot["status"] !in setOf("available","partial")) return false
        val rows = (snapshot["skills"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: return false
        return skills.all { skill -> rows.singleOrNull { it["id"] == skill }?.get("level") is Number }
    }
}
internal fun isPurchaseUnlock(conditionId: String): Boolean = conditionId == "has_skill_level"
internal fun upgradeLevelAllowed(current: Int,target: Int,maximum: Int,custom: Boolean,policy: EnchantUpgradeAPI.LevelPolicy): Boolean =
    current >= 0 && target > current && target.toLong() <= maximum.toLong() + if (!custom && policy == EnchantUpgradeAPI.LevelPolicy.VANILLA_ONE_ABOVE) 1 else 0

/** Compare again AFTER callbacks/conditions: they may change the actual inventory or configuration. */
internal fun executeUpgrade(unchanged: () -> Boolean,validate: () -> String?,before: () -> Boolean,replace: () -> Unit,after: () -> Unit): Pair<EnchantUpgradeAPI.Status,String?> {
    if (!unchanged()) return EnchantUpgradeAPI.Status.ITEM_CHANGED to "original_item_changed"
    validate()?.let { return EnchantUpgradeAPI.Status.DENIED to it }
    if (!before()) return EnchantUpgradeAPI.Status.CANCELLED to "native_prepare_cancelled"
    validate()?.let { return EnchantUpgradeAPI.Status.DENIED to it }
    if (!unchanged()) return EnchantUpgradeAPI.Status.ITEM_CHANGED to "item_changed_during_callbacks"
    try { replace() } catch (_: Exception) { return EnchantUpgradeAPI.Status.UNKNOWN_APPLY_REVIEW to "inventory_apply_uncertain" }
    try { after() } catch (_: Exception) { return EnchantUpgradeAPI.Status.APPLIED_CALLBACK_REVIEW to "callback_uncertain_do_not_reapply" }
    return EnchantUpgradeAPI.Status.APPLIED to null
}

internal fun reservedReplacementConflict(id: String, custom: Boolean, type: String): Boolean =
    id in setOf("indestructible","soulbound","one_for_all","ultimate_wise","repairing","bastion","crimson_slayer") || custom && type != "normal"

/** Clone the complete item; mutate exactly the selected native metadata entry. */
internal fun removalCandidate(original: ItemStack,enchantment: Enchantment,stored: Boolean): ItemStack? {
    val candidate = original.clone()
    val meta = candidate.itemMeta ?: return null
    val changed = if (stored) (meta as? EnchantmentStorageMeta)?.removeStoredEnchant(enchantment) == true else meta.removeEnchant(enchantment)
    return if (changed && candidate.setItemMeta(meta)) candidate else null
}
