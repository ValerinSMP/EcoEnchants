package com.willfp.ecoenchants.api

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.event.Event
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import java.util.UUID

/** Ordinary removal preflight only. Cancelling never authorizes a refund or XP grant. */
class EnchantRemovalPrepareEvent(val player: Player,val operationId: UUID,val removal: EnchantUpgradeAPI.Removal,item: ItemStack) : Event(), Cancellable {
    private val original = item.clone()
    fun getOriginalItem(): ItemStack = original.clone()
    private var cancelled = false
    override fun isCancelled() = cancelled
    override fun setCancelled(value: Boolean) { cancelled = value }
    override fun getHandlers(): HandlerList = HANDLERS
    companion object { private val HANDLERS: HandlerList = HandlerList(); @JvmStatic fun getHandlerList(): HandlerList = HANDLERS }
}

/** In-memory native mutation notification; not a durable delivery/credit receipt. */
class EnchantRemovalAppliedEvent(val player: Player,val operationId: UUID,val removal: EnchantUpgradeAPI.Removal,before: ItemStack,after: ItemStack) : Event() {
    private val original = before.clone()
    private val applied = after.clone()
    fun getOriginalItem(): ItemStack = original.clone()
    fun getAppliedItem(): ItemStack = applied.clone()
    override fun getHandlers(): HandlerList = HANDLERS
    companion object { private val HANDLERS: HandlerList = HandlerList(); @JvmStatic fun getHandlerList(): HandlerList = HANDLERS }
}
