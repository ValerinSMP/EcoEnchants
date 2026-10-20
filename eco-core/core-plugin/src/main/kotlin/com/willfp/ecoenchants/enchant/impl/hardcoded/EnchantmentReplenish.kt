package com.willfp.ecoenchants.enchant.impl.hardcoded

import com.willfp.ecoenchants.enchant.EcoEnchant
import com.willfp.ecoenchants.enchant.impl.HardcodedEcoEnchant
import com.willfp.ecoenchants.target.EnchantFinder.hasEnchantActive
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object EnchantmentReplenish : HardcodedEcoEnchant(
    "replenish"
) {
    private var handler = ReplenishHandler(this)

    override fun onRegister() {
        handler.activate()
        plugin.eventManager.registerListener(handler)
    }

    override fun onRemove() {
        handler.deactivate()
        plugin.eventManager.unregisterListener(handler)
    }

    private class ReplenishHandler(
        private val enchant: EcoEnchant
    ) : Listener {
        private var active=false
        private var generation=0L
        private var task:org.bukkit.scheduler.BukkitTask?=null
        private val pending=linkedMapOf<org.bukkit.block.Block,()->Unit>()
        private var failureLogged=false
        fun activate(){active=true;generation++;failureLogged=false}
        fun deactivate(){active=false;generation++;task?.cancel();task=null;pending.clear()}
        private fun schedule(){
            if(task!=null||!active)return
            task=Bukkit.getScheduler().runTask(enchant.plugin,Runnable{
                task=null
                if(!active)return@Runnable
                val count=(enchant.config.getIntOrNull("replant-per-tick")?:64).coerceIn(1,256)
                val jobs=mutableListOf<()->Unit>();val iterator=pending.entries.iterator()
                while(iterator.hasNext()&&jobs.size<count){jobs.add(iterator.next().value);iterator.remove()}
                for(job in jobs)try{job()}catch(problem:RuntimeException){
                    if(!failureLogged){failureLogged=true;enchant.plugin.logger.log(java.util.logging.Level.SEVERE,"Replanting operation failed",problem)}
                }
                if(pending.isNotEmpty())schedule()
            })
        }
        @EventHandler(
            ignoreCancelled = true
        )
        fun handle(event: BlockBreakEvent) {
            val player = event.player

            if (!player.hasEnchantActive(enchant)) {
                return
            }

            val block = event.block
            val type = block.type

            if (type in arrayOf(
                    Material.GLOW_BERRIES,
                    Material.SWEET_BERRY_BUSH,
                    Material.CACTUS,
                    Material.BAMBOO,
                    Material.CHORUS_FLOWER,
                    Material.SUGAR_CANE
                )
            ) {
                return
            }

            val data = block.blockData

            if (data !is Ageable) {
                return
            }

            // Check maturity before reserving any seeds or changing drop behavior.
            if(data.age!=data.maximumAge&&enchant.config.getBool("only-fully-grown"))return
            if(!active||block in pending||pending.size>=1024)return
            val seed=ItemStack(when(type){
                Material.WHEAT->Material.WHEAT_SEEDS
                Material.POTATOES->Material.POTATO
                Material.CARROTS->Material.CARROT
                Material.BEETROOTS->Material.BEETROOT_SEEDS
                Material.COCOA->Material.COCOA_BEANS
                Material.NETHER_WART->Material.NETHER_WART
                else->return
            })
            val consume=enchant.config.getBool("consume-seeds")
            if(consume&&!player.inventory.containsAtLeast(seed,1))return
            if(data.age!=data.maximumAge){event.isDropItems=false;event.expToDrop=0}
            data.age=0
            val epoch=generation
            val session=com.willfp.ecoenchants.progression.OriginalRuntime.session(player)?:return
            pending[block]={
                val current=active&&epoch==generation&&!event.isCancelled&&player.isOnline&&!player.isDead&&
                    Bukkit.getPlayer(player.uniqueId)===player&&player.world==block.world&&
                    com.willfp.ecoenchants.progression.OriginalRuntime.sameSession(player.uniqueId,session)
                if(current&&block.world.isChunkLoaded(block.x shr 4,block.z shr 4)&&block.type.isAir&&
                    player.hasEnchantActive(enchant)&&(!consume||player.inventory.containsAtLeast(seed,1))&&block.canPlace(data)){
                    val replaced=block.state
                    block.setType(type,false);block.setBlockData(data,false)
                    try{
                        // Native placement compatibility is post-placement; rollback our exact block on veto.
                        val placed=BlockPlaceEvent(block,replaced,block.getRelative(BlockFace.DOWN),seed.clone(),player,true,EquipmentSlot.HAND)
                        Bukkit.getPluginManager().callEvent(placed)
                        val accepted=active&&epoch==generation&&!placed.isCancelled&&placed.canBuild()&&
                            player.isOnline&&!player.isDead&&player.world==block.world&&
                            com.willfp.ecoenchants.progression.OriginalRuntime.sameSession(player.uniqueId,session)&&
                            block.blockData==data&&player.hasEnchantActive(enchant)
                        if(!accepted||consume&&player.inventory.removeItem(seed.clone()).isNotEmpty()){
                            if(block.blockData==data)replaced.update(true,false)
                        }
                    }catch(problem:RuntimeException){if(block.blockData==data)replaced.update(true,false);throw problem}
                }
            }
            schedule()
        }
    }
}
