package com.willfp.ecoenchants.enchant

/** One exact opt-in pair; native registry decisions remain authoritative for everything else. */
internal fun effectiveConflict(first:String,second:String,enabled:()->Boolean,native:()->Boolean):Boolean {
    val approved = first == "minecraft:sharpness" && second == "minecraft:smite" ||
        first == "minecraft:smite" && second == "minecraft:sharpness"
    return if (approved && enabled()) false else native()
}
