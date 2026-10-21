package com.willfp.ecoenchants.progression

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.libreforge.*
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.triggers.*
import com.willfp.libreforge.triggers.Triggers

/** Uses the native forceHolders/conditions/placeholder path with the captured source item. */
internal object ConfirmedOriginalHit : Trigger("confirmed_original_hit") {
    override val parameters = setOf(TriggerParameter.PLAYER,TriggerParameter.VICTIM,TriggerParameter.ITEM,
        TriggerParameter.EVENT,TriggerParameter.LOCATION,TriggerParameter.VALUE)
}
internal class OriginalProcEffect(private val family: OriginalProcEngine.Family,id: String) : Effect<NoCompileData>(id) {
    override val description = "Runs a bounded original proc after a confirmed direct hit allowed by its PvP/PvE setting."
    override val categories = setOf("combat")
    override val parameters = ConfirmedOriginalHit.parameters
    override val arguments = arguments {
        require("amount","Specify the tier percentage.",description="Tier percentage.",type=ArgType.EXPRESSION)
        require("hits","Specify hit count.",description="Confirmed hits per item and target.",type=ArgType.INT)
        require("window","Specify hit window.",description="Fixed hit window in seconds.",type=ArgType.INT)
        require("proc_cooldown","Specify proc cooldown.",description="Player cooldown, independent of item swaps.",type=ArgType.INT)
    }
    override fun onTrigger(config: Config,data: TriggerData,compileData: NoCompileData): Boolean {
        val resonance=family==OriginalProcEngine.Family.ECO_RESONANTE
        val hits=config.getInt("hits");val window=config.getInt("window");val cooldown=config.getInt("proc_cooldown")
        OriginalRuntime.diagnostic(data,"effect_entry") { "family=$family hits=$hits window=$window cooldown=$cooldown" }
        if(hits!=(if(resonance)3 else 2)||window!=(if(resonance)5 else 4)||cooldown!=(if(resonance)8 else 10))return false
        return OriginalRuntime.execute(family,config.getDoubleFromExpression("amount",data),data)
    }
}
object OriginalProgression {
    private var owner:EcoEnchantsPlugin?=null
    @JvmStatic fun register() {
        NativeMiningProgression.register()
        NativeBurstProgression.register()
        NativeReactiveProgression.register()
        NativeUtilityProgression.register()
        NativeFirstHits.register()
        NativeSwarm.register()
        NativeCombatProgression.register()
        Triggers.register(ConfirmedOriginalHit)
        Effects.register(OriginalProcEffect(OriginalProcEngine.Family.ECO_RESONANTE,"resonant_pulse"))
        Effects.register(OriginalProcEffect(OriginalProcEngine.Family.SELLO_GLACIAL,"glacial_seal"))
    }
    @JvmStatic fun enable(plugin: EcoEnchantsPlugin) { owner=plugin;OriginalRuntime.enable(plugin);NativeMiningProgression.enable(plugin);NativeCombatProgression.enable(plugin);NativeBurstProgression.enable(plugin);NativeReactiveProgression.enable(plugin);NativeNormalDamage.enable(plugin) }
    @JvmStatic fun reload() { OriginalRuntime.reset();NativeMiningProgression.reset();NativeCombatProgression.reset();NativeBurstProgression.reset();NativeReactiveProgression.reset() }
    @JvmStatic fun disable() { owner?.let{NativeNormalDamage.close(it)};owner=null;NativeReactiveProgression.close();NativeBurstProgression.close();NativeCombatProgression.close();NativeMiningProgression.close();OriginalRuntime.close() }
}
