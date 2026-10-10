package com.willfp.ecoenchants.mechanics

// Backport of eco 0bbce66b/ea957496; this fork retains its native EcoEnchants anvil.
internal const val COMBINE_REPAIR_COST = 2
internal fun computeAnvilCost(priorWork: Int, renameCost: Int, mergePrice: Int): Int =
    priorWork + renameCost + mergePrice
