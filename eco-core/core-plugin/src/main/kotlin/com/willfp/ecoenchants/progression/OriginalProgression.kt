package com.willfp.ecoenchants.progression

import com.willfp.ecoenchants.EcoEnchantsPlugin

/** First increment contains the bounded mechanic core only; never registers a placeholder effect. */
object OriginalProgression {
    const val BLOCKER = "original_progression_blocked: eco_resonante/sello_glacial need authoritative boss exclusion and confirmed-hit adapters; sello_glacial also needs projectile source-item identity. No executable effect registered."
    @JvmStatic fun diagnose(plugin: EcoEnchantsPlugin) {
        plugin.logger.warning(BLOCKER)
    }
}
