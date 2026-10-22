package com.willfp.ecoenchants.api
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class EnchantUpgradeTest {
    @Test fun `vanilla extra tier is explicit and never bypasses custom maximum or permits downgrade`() {
        assertFalse(upgradeLevelAllowed(5,6,5,false,EnchantUpgradeAPI.LevelPolicy.NATIVE_MAXIMUM))
        assertTrue(upgradeLevelAllowed(5,6,5,false,EnchantUpgradeAPI.LevelPolicy.VANILLA_ONE_ABOVE))
        assertFalse(upgradeLevelAllowed(5,6,5,true,EnchantUpgradeAPI.LevelPolicy.VANILLA_ONE_ABOVE))
        assertFalse(upgradeLevelAllowed(5,5,5,false,EnchantUpgradeAPI.LevelPolicy.VANILLA_ONE_ABOVE))
        assertFalse(upgradeLevelAllowed(5,7,5,false,EnchantUpgradeAPI.LevelPolicy.VANILLA_ONE_ABOVE))
    }
    @Test fun `prepare callback cannot replace an item or grant stale metadata`() {
        var same = true;var replacements = 0
        val result = executeUpgrade({same},{null},{same=false;true},{replacements++},{})
        assertEquals(EnchantUpgradeAPI.Status.ITEM_CHANGED,result.first);assertEquals(0,replacements)
    }
    @Test fun `changed requirements after callback prevent application`() {
        var reason: String? = null;var writes = 0
        val result = executeUpgrade({true},{reason},{reason="requirement_changed";true},{writes++},{})
        assertEquals(EnchantUpgradeAPI.Status.DENIED,result.first);assertEquals(0,writes)
    }
    @Test fun `callback failure after inventory change is applied review and never retries replacement`() {
        var writes = 0
        val result = executeUpgrade({true},{null},{true},{writes++},{error("uncertain native callback")})
        assertEquals(EnchantUpgradeAPI.Status.APPLIED_CALLBACK_REVIEW,result.first);assertEquals(1,writes)
    }
    @Test fun `cancelled preflight never changes item or holders`() {
        var writes = 0;var holders = 0
        assertEquals(EnchantUpgradeAPI.Status.CANCELLED,executeUpgrade({true},{null},{false},{writes++},{holders++}).first)
        assertEquals(0,writes);assertEquals(0,holders)
    }
    @Test fun `activation conditions never become artificial shop requirements`() {
        assertTrue(isPurchaseUnlock("has_skill_level"))
        assertFalse(isPurchaseUnlock("below_health_percent"))
        assertFalse(isPurchaseUnlock("is_sneaking"))
    }
    @Test fun `replacement never silently removes reserved or special enchants`() {
        for(id in listOf("indestructible","soulbound","one_for_all","ultimate_wise","repairing","bastion","crimson_slayer")) assertTrue(reservedReplacementConflict(id,true,"normal"))
        assertTrue(reservedReplacementConflict("other_special",true,"special"))
        assertFalse(reservedReplacementConflict("sharpness",false,"normal"))
        assertFalse(reservedReplacementConflict("smite",false,"normal"))
    }
    @Test fun `exact removal list and complete expected item remain defensive copies`() {
        val first=org.mockito.Mockito.mock(org.bukkit.inventory.ItemStack::class.java)
        val captured=org.mockito.Mockito.mock(org.bukkit.inventory.ItemStack::class.java)
        val exported=org.mockito.Mockito.mock(org.bukkit.inventory.ItemStack::class.java)
        org.mockito.Mockito.`when`(first.clone()).thenReturn(captured)
        org.mockito.Mockito.`when`(captured.clone()).thenReturn(exported)
        val removals=mutableListOf(EnchantUpgradeAPI.Removal("minecraft:sharpness",5,false))
        val plan=EnchantUpgradeAPI.ReplacementPlan(java.util.UUID.randomUUID(),0,first,"minecraft:smite",0,5,EnchantUpgradeAPI.LevelPolicy.NATIVE_MAXIMUM,java.util.UUID.randomUUID(),removals)
        removals.clear();assertEquals(listOf(EnchantUpgradeAPI.Removal("minecraft:sharpness",5,false)),plan.removals)
        assertSame(exported,plan.getExpectedItem());assertNotSame(first,plan.getExpectedItem())
        assertThrows(UnsupportedOperationException::class.java){(plan.removals as MutableList).clear()}
        assertEquals(0,plan.currentLevel);assertEquals(5,plan.targetLevel)
    }
}
