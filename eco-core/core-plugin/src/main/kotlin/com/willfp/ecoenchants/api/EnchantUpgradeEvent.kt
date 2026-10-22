package com.willfp.ecoenchants.api
import org.bukkit.entity.Player
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.event.Event
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import java.util.UUID
class EnchantUpgradePrepareEvent(val player: Player,val operationId: UUID,val enchantment: Enchantment,val fromLevel: Int,val toLevel: Int,item: ItemStack) : Event(), Cancellable {
    private var removed: List<EnchantUpgradeAPI.Removal> = emptyList()
    internal constructor(player: Player,operationId: UUID,enchantment: Enchantment,fromLevel: Int,toLevel: Int,item: ItemStack,removals: List<EnchantUpgradeAPI.Removal>) : this(player,operationId,enchantment,fromLevel,toLevel,item) {
        removed=java.util.Collections.unmodifiableList(ArrayList(removals))
    }
    fun getRemovals(): List<EnchantUpgradeAPI.Removal> = removed
    private val original = item.clone()
    fun getOriginalItem(): ItemStack = original.clone()
    private var cancelled = false
    override fun isCancelled() = cancelled
    override fun setCancelled(value: Boolean) { cancelled = value }
    override fun getHandlers(): HandlerList = HANDLERS
    companion object { private val HANDLERS: HandlerList = HandlerList(); @JvmStatic fun getHandlerList(): HandlerList = HANDLERS }
}
class EnchantUpgradeAppliedEvent(val player: Player,val operationId: UUID,val enchantment: Enchantment,val fromLevel: Int,val toLevel: Int,before: ItemStack,after: ItemStack) : Event() {
    private val original = before.clone(); private val applied = after.clone()
    fun getOriginalItem(): ItemStack = original.clone()
    fun getAppliedItem(): ItemStack = applied.clone()
    override fun getHandlers(): HandlerList = HANDLERS
    companion object { private val HANDLERS: HandlerList = HandlerList(); @JvmStatic fun getHandlerList(): HandlerList = HANDLERS }
}
