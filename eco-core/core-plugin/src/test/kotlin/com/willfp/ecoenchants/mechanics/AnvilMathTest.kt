package com.willfp.ecoenchants.mechanics

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class AnvilMathTest {
    @Test fun `rename does not charge an enchant merge twice`() {
        assertEquals(1, computeAnvilCost(0, 1, 0))
        assertEquals(5, computeAnvilCost(3, 0, 2))
    }
    @Test fun `fresh durability merge is not a free no-op`() {
        assertEquals(2, computeAnvilCost(0, 0, COMBINE_REPAIR_COST))
    }
}
