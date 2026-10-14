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
    @Test fun `inventory title shows a counter only when there are multiple pages`() {
        assertEquals("Guía de encantamientos", GuideQuery.title("Guía de encantamientos", 1, 1))
        assertEquals("Guía de encantamientos (2/6)", GuideQuery.title("Guía de encantamientos", 2, 6))
        assertEquals("Filo V", GuideQuery.plain("&#FFDA29<bold>Filo V</bold>"))
    }
    @Test fun `filter preview includes current selection and stays inside configured definitions`() {
        for (size in listOf(1, 5, 16)) for (index in 0 until size) {
            val window = GuideQuery.filterWindow(index, size)
            assertTrue(index in window)
            assertTrue(window.all { it in 0 until size })
            assertTrue(window.count() <= 8)
        }
        assertTrue(GuideQuery.filterWindow(0, 0).isEmpty())
    }

    @Test fun `cancel and timeout retain the exact query filter group and page`() {
        val state = GuideState("Filo", 4, "normal", 3)
        assertEquals(state, GuideQuery.searchResult(state, null))
        assertNull(GuideQuery.searchInput("cancelar"))
        assertNull(GuideQuery.searchInput("  CANCELAR  "))
        assertEquals(state.copy(query = "Fortuna", page = 1), GuideQuery.searchResult(state, "Fortuna"))
        assertEquals(state.copy(query = "", page = 1), GuideQuery.searchResult(state, ""))
    }
    @Test fun `search returns to default views independently of inventory type but never steals another view`() {
        val survivalView = Any()
        val creativeView = Any()
        assertTrue(GuideQuery.canResumeSearch(true, true, survivalView, survivalView))
        assertTrue(GuideQuery.canResumeSearch(true, true, creativeView, creativeView))
        assertFalse(GuideQuery.canResumeSearch(true, true, creativeView, Any()))
        assertFalse(GuideQuery.canResumeSearch(false, true, creativeView, creativeView))
        assertFalse(GuideQuery.canResumeSearch(true, false, creativeView, creativeView))
    }

}
