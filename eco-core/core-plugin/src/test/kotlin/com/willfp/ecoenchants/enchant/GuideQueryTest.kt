package com.willfp.ecoenchants.enchant

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GuideQueryTest {
    @Test fun `search accepts Spanish accents case and color formatted names`() {
        assertTrue(GuideQuery.matches("&aExcavación II", "EXCAVACION"))
        assertTrue(GuideQuery.matches("<red>Replantación</red>", "replantacion"))
        assertTrue(GuideQuery.matches("§x§f§f§0§0§0§0Filo V", "filo"))
    }
    @Test fun `search text is literal without regexp execution`() {
        assertFalse(GuideQuery.matches("Excavación", ".*"))
        assertTrue(GuideQuery.matches("Filo", ""))
    }
    @Test fun `input is bounded and removes controls`() {
        assertEquals(64, GuideQuery.input("a".repeat(100)).length)
        assertEquals("filo", GuideQuery.input("  fi\u0000lo\n "))
    }
    @Test fun `filters wrap in either direction and handle empty configuration`() {
        assertEquals(0, GuideQuery.next(15, 1, 16))
        assertEquals(15, GuideQuery.next(0, -1, 16))
        assertEquals(0, GuideQuery.next(0, 1, 0))
    }
    @Test fun `levels respect boundaries and do not overflow`() {
        assertEquals(1, GuideQuery.level(1, -1, 5))
        assertEquals(5, GuideQuery.level(5, 1, 5))
        assertEquals(4, GuideQuery.level(3, 1, 5))
        assertEquals(Int.MAX_VALUE, GuideQuery.level(Int.MAX_VALUE, 1, Int.MAX_VALUE))
    }
    @Test fun `equipment filters include universal and combined armor targets`() {
        assertTrue(GuideQuery.matchesTargets(setOf("all"), setOf("sword")))
        assertTrue(GuideQuery.matchesTargets(setOf("boots"), setOf("armor", "boots")))
        assertFalse(GuideQuery.matchesTargets(setOf("pickaxe"), setOf("bow")))
        assertTrue(GuideQuery.matchesTargets(setOf("pickaxe"), emptySet()))
    }
}
