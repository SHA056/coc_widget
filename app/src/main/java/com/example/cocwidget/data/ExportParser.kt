package com.example.cocwidget.data

import kotlinx.serialization.json.Json

/** Pure parsing logic — no Android dependencies, so it is unit-testable on the JVM. */
object ExportParser {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * @param raw the text the user pasted.
     * @param nowEpochSec used as the export time when the export has no `timestamp`.
     * @param nameFor maps a game data ID to a display name, or null if unknown.
     */
    fun parse(raw: String, nowEpochSec: Long, nameFor: (Int) -> String?): Snapshot {
        val export = json.decodeFromString<VillageExport>(raw.trim())
        val exportedAt = export.timestamp ?: nowEpochSec

        fun collect(entries: List<ExportEntry>, source: UpgradeSource) =
            entries.mapNotNull { entry ->
                val timer = entry.timer ?: return@mapNotNull null
                if (timer <= 0) return@mapNotNull null
                Upgrade(
                    id = entry.data,
                    name = nameFor(entry.data) ?: "Unknown #${entry.data}",
                    source = source,
                    level = entry.lvl,
                    finishAtEpochSec = exportedAt + timer,
                )
            }

        val upgrades = buildList {
            addAll(collect(export.buildings, UpgradeSource.BUILDER))
            addAll(collect(export.traps, UpgradeSource.BUILDER))
            addAll(collect(export.heroes, UpgradeSource.HERO))
            addAll(collect(export.units, UpgradeSource.LAB))
            addAll(collect(export.spells, UpgradeSource.LAB))
            addAll(collect(export.siegeMachines, UpgradeSource.LAB))
            addAll(collect(export.pets, UpgradeSource.PET))
            addAll(collect(export.equipment, UpgradeSource.BLACKSMITH))
            addAll(collect(export.buildings2, UpgradeSource.BUILDER_BASE))
            addAll(collect(export.traps2, UpgradeSource.BUILDER_BASE))
            addAll(collect(export.heroes2, UpgradeSource.BUILDER_BASE))
            addAll(collect(export.units2, UpgradeSource.BUILDER_BASE_LAB))
        }.sortedBy { it.finishAtEpochSec }

        return Snapshot(
            tag = export.tag,
            exportedAtEpochSec = exportedAt,
            exportTimestampKnown = export.timestamp != null,
            importedAtEpochSec = nowEpochSec,
            upgrades = upgrades,
        )
    }
}
