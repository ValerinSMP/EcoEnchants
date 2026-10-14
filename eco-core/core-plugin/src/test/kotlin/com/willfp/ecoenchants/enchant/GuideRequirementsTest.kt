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
}
