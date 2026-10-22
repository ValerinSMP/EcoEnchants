package com.willfp.ecoenchants.api
import com.willfp.ecoenchants.enchant.effectiveConflict
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EffectiveCompatibilityTest {
 @Test fun approvedPairIsSymmetricExplicitAndDisabledByDefault(){
  for((a,b) in listOf("minecraft:sharpness" to "minecraft:smite","minecraft:smite" to "minecraft:sharpness")){
   assertTrue(effectiveConflict(a,b,{false},{true}))
   assertFalse(effectiveConflict(a,b,{true},{error("Approved pair must not mutate or depend on the vanilla registry")}))
  }
 }
 @Test fun unrelatedVanillaNamespacesAndReservedPairsKeepTheirNativeDecision(){
  for((a,b) in listOf("minecraft:sharpness" to "minecraft:bane_of_arthropods","minecraft:smite" to "minecraft:bane_of_arthropods","other:sharpness" to "minecraft:smite","ecoenchants:soulbound" to "minecraft:smite")){
   assertTrue(effectiveConflict(a,b,{error("Unrelated pairs do not even consult the exception flag")},{true}))
   assertFalse(effectiveConflict(a,b,{true},{false}))
  }
 }
 @Test fun callbackPolicyChangeCannotApplyAStaleCoexistenceOrRemovalPlan(){
  for(initial in listOf(true,false)){
   var enabled=initial;var writes=0;var callbacks=0
   fun removals()=if(effectiveConflict("minecraft:sharpness","minecraft:smite",{enabled},{true}))listOf(EnchantUpgradeAPI.Removal("minecraft:sharpness",5,false)) else emptyList()
   val original=removals()
   val result=executeUpgrade({true},{if(removals()==original)null else "replacement_plan_changed"},{callbacks++;enabled=!enabled;true},{writes++},{})
   assertEquals(EnchantUpgradeAPI.Status.DENIED,result.first);assertEquals(0,writes);assertEquals(1,callbacks)
  }
 }
 @Test fun custodyQueryIsPublicAndDoesNotRequireAPlayerSlot(){
  val method=EnchantUpgradeAPI::class.java.getMethod("getEffectiveConflicts",org.bukkit.inventory.ItemStack::class.java,org.bukkit.enchantments.Enchantment::class.java)
  assertTrue(java.lang.reflect.Modifier.isStatic(method.modifiers));assertEquals(java.util.List::class.java,method.returnType)
 }

}
