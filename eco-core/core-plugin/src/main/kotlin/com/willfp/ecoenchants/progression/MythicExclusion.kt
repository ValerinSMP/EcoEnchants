package com.willfp.ecoenchants.progression
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.entity.Entity
internal object MythicExclusion {
    fun isManaged(entity: Entity): Boolean = MythicBukkit.inst().apiHelper.isMythicMob(entity)
}
