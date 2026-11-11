package com.example.cocwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportParserTest {

    private val names = mapOf(1000008 to "Cannon", 28000000 to "Barbarian King")

    @Test
    fun `keeps only entries with a timer and converts to absolute finish time`() {
        val raw = """
            {
              "tag": "#ABC",
              "timestamp": 1000,
              "someNewField": {"ignored": true},
              "buildings": [
                {"data": 1000008, "lvl": 17, "cnt": 6},
                {"data": 1000008, "lvl": 16, "timer": 500}
              ],
              "heroes": [{"data": 28000000, "lvl": 70, "timer": 100}],
              "units": [{"data": 4000999, "lvl": 1, "timer": 0}]
            }
        """.trimIndent()

        val snap = ExportParser.parse(raw, nowEpochSec = 9_999) { names[it] }

        assertEquals("#ABC", snap.tag)
        assertTrue(snap.exportTimestampKnown)
        assertEquals(1000L, snap.exportedAtEpochSec)
        assertEquals(2, snap.upgrades.size)
        // Sorted soonest-first
        assertEquals("Barbarian King", snap.upgrades[0].name)
        assertEquals(1100L, snap.upgrades[0].finishAtEpochSec)
        assertEquals(UpgradeSource.HERO, snap.upgrades[0].source)
        assertEquals("Cannon", snap.upgrades[1].name)
        assertEquals(1500L, snap.upgrades[1].finishAtEpochSec)
    }

    @Test
    fun `real export - every active upgrade has a name from the bundled table`() {
        // Unit tests run with the module dir (app/) as working directory.
        val names: Map<String, String> = ExportParser.json.decodeFromString(
            java.io.File("src/main/assets/names.json").readText()
        )
        val raw = javaClass.getResource("/export_r9cvyljyj.json")!!.readText()

        val snap = ExportParser.parse(raw, nowEpochSec = 0) { names[it.toString()] }

        assertEquals("#R9CVYLJYJ", snap.tag)
        assertEquals(1794007698L, snap.exportedAtEpochSec)
        assertEquals(
            listOf(
                "Balloon", "Town Hall", "Hero Hall", "Dark Spell Factory",
                "Star Laboratory", "Eagle Artillery", "Reinforcement Camp",
            ),
            snap.upgrades.map { it.name },
        )
        assertEquals(1794007698L + 11879, snap.upgrades.first().finishAtEpochSec)
        assertEquals(UpgradeSource.BUILDER_BASE, snap.upgrades.last().source)
    }

    @Test
    fun `falls back to import time when export has no timestamp`() {
        val raw = """{"buildings": [{"data": 42, "lvl": 1, "timer": 60}]}"""

        val snap = ExportParser.parse(raw, nowEpochSec = 5_000) { null }

        assertFalse(snap.exportTimestampKnown)
        assertEquals(5_060L, snap.upgrades.single().finishAtEpochSec)
        assertEquals("Unknown #42", snap.upgrades.single().name)
    }
}
