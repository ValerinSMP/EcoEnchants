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
}
