package com.willfp.ecoenchants.enchant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
class GuideRequirementsTest {
    @Test fun `direct conditions combine using maximum and preserve missing`() {
        val gate = GuideRequirements.parse(listOf("5", "14"), false)
        assertEquals(14, gate.level)
        assertEquals("missing", GuideRequirements.status(gate, 10))
        assertEquals("met", GuideRequirements.status(gate, 14))
        assertEquals("unavailable", GuideRequirements.status(gate, null))
        assertEquals("none", GuideRequirements.status(GuideRequirements.parse(emptyList(), false), null))
    }
    @Test fun `expressions and invalid values do not invent thresholds`() {
        for (value in listOf("5 + 2", "%level%", "-1", "2147483648", ""))
            assertEquals("unsupported", GuideRequirements.status(GuideRequirements.parse(listOf(value), false), 50))
    }
    @Test fun `999 alone is not a disable declaration`() {
        assertEquals("missing", GuideRequirements.status(GuideRequirements.parse(listOf("999"), false), 50))
        assertEquals("disabled", GuideRequirements.status(GuideRequirements.parse(listOf("999"), true), null))
        assertEquals("missing", GuideRequirements.status(GuideRequirements.parse(listOf("14"), true), 10))
    }
    @Test fun `snapshot checks schema identity availability and numeric integrity`() {
        val uuid = UUID.randomUUID()
        fun snapshot(level: Any?) = mapOf("schemaVersion" to 1, "playerUuid" to uuid.toString(), "status" to "partial", "skills" to listOf(mapOf("id" to "enchanting", "level" to level)))
        assertEquals(10, GuideRequirements.snapshotLevel(snapshot(10), uuid))
        assertEquals(0, GuideRequirements.snapshotLevel(snapshot(0), uuid))
        for (value in listOf(null, -1, 0.02, Double.NaN, Double.POSITIVE_INFINITY, "10")) assertNull(GuideRequirements.snapshotLevel(snapshot(value), uuid))
        assertNull(GuideRequirements.snapshotLevel(snapshot(10), UUID.randomUUID()))
        assertNull(GuideRequirements.snapshotLevel(snapshot(10) + ("status" to "unavailable"), uuid))
        assertNull(GuideRequirements.snapshotLevel(snapshot(10) + ("schemaVersion" to 2), uuid))
        assertNull(GuideRequirements.snapshotLevel(null, uuid))
    }
    private val templates = mapOf(
        "heading" to listOf("&#FFDA29Requisitos:"),
        "missing" to listOf("&#FF3300Requiere Encantador %required_level% (%current_level%)"),
        "unavailable" to listOf("&#FF9F0FRequiere Encantador %required_level% (No disponible)"),
        "previous" to listOf("&7Encantamientos previos: &f%previous%")
    )
    @Test fun `met and no gate hide entire requirement section without preceding enchants`() {
        val gate = GuideRequirements.parse(listOf("14"), false)
        for (level in listOf(14, 50)) assertTrue(GuideRequirements.lines(gate, level, null) { templates[it] ?: emptyList() }.isEmpty())
        assertTrue(GuideRequirements.lines(GuideRequirements.Gate(), null, "") { templates[it] ?: emptyList() }.isEmpty())
    }
    @Test fun `missing displays one colored line and independent viewer levels`() {
        val gate = GuideRequirements.parse(listOf("14"), false)
        val low = GuideRequirements.lines(gate, 10, null) { templates[it] ?: emptyList() }
        assertEquals(listOf("", "&#FFDA29Requisitos:", "&#FF3300Requiere Encantador 14 (10)"), low)
        assertTrue(GuideRequirements.lines(gate, 20, null) { templates[it] ?: emptyList() }.isEmpty())
        assertEquals(low, GuideRequirements.lines(gate, 10, null) { templates[it] ?: emptyList() })
    }
    @Test fun `cold level and preceding enchant retain meaningful section without fabricated values`() {
        val gate = GuideRequirements.parse(listOf("14"), false)
        assertEquals("&#FF9F0FRequiere Encantador 14 (No disponible)", GuideRequirements.lines(gate, null, null) { templates[it] ?: emptyList() }.last())
        assertEquals(listOf("", "&#FFDA29Requisitos:", "&7Encantamientos previos: &fFilo"), GuideRequirements.lines(gate, 20, "Filo") { templates[it] ?: emptyList() })
    }
    @Test fun `manual requirement removed before wrapping without removing unrelated prose or cost`() {
        val raw = listOf("Da un bono", "&7Req: &cEncantador Nv14 ☄", "&7Requisito: Encantador Nv 14", "&7Costo: 25 mana", "Encantador mejora el arma")
        assertEquals(listOf(raw[0], raw[3], raw[4]), GuideRequirements.stripManualRequirement(raw, true))
        assertEquals(raw, GuideRequirements.stripManualRequirement(raw, false))
    }
}
