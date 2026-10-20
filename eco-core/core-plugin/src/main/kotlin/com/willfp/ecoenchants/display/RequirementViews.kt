package com.willfp.ecoenchants.display
import com.willfp.eco.core.display.DisplayViewUpdates
import com.willfp.eco.core.fast.fast
import com.willfp.ecoenchants.enchant.EcoEnchant
object RequirementViews {
 @JvmStatic fun register(){
  DisplayViewUpdates.register("EcoEnchants") { item, dependencies ->
   item.fast().getEnchants(true).any { (enchant,tier) ->
    enchant is EcoEnchant && enchant.getLevel(tier).conditions.any { block ->
     block.condition.id=="has_skill_level" && "ecoskills:${block.config.getString("skill").lowercase()}" in dependencies
    }
   }
  }
 }
 @JvmStatic fun close(){DisplayViewUpdates.unregister("EcoEnchants")}
}
