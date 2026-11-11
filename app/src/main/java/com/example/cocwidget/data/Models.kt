package com.example.cocwidget.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of the in-game "copy data" export. Only the fields we need are declared;
 * everything else is ignored (Supercell adds fields regularly).
 */
@Serializable
data class VillageExport(
    val tag: String? = null,
    /** Epoch seconds when the export was taken. `timer` values are relative to this. */
    val timestamp: Long? = null,
    val buildings: List<ExportEntry> = emptyList(),
    val traps: List<ExportEntry> = emptyList(),
    val heroes: List<ExportEntry> = emptyList(),
    val units: List<ExportEntry> = emptyList(),
    val spells: List<ExportEntry> = emptyList(),
    @SerialName("siege_machines") val siegeMachines: List<ExportEntry> = emptyList(),
    val pets: List<ExportEntry> = emptyList(),
    val equipment: List<ExportEntry> = emptyList(),
    // Builder Base
    val buildings2: List<ExportEntry> = emptyList(),
    val traps2: List<ExportEntry> = emptyList(),
    val heroes2: List<ExportEntry> = emptyList(),
    val units2: List<ExportEntry> = emptyList(),
)

@Serializable
data class ExportEntry(
    /** Game data ID, e.g. 1000001 = Town Hall. */
    val data: Int,
    val lvl: Int = 0,
    val cnt: Int = 1,
    /** Seconds remaining on an in-progress upgrade at export time; absent if idle. */
    val timer: Long? = null,
)

enum class UpgradeSource(val label: String) {
    BUILDER("Builder"),
    LAB("Lab"),
    HERO("Hero"),
    PET("Pet House"),
    BLACKSMITH("Blacksmith"),
    BUILDER_BASE("Builder Base"),
    BUILDER_BASE_LAB("Star Lab"),
}

/** One in-progress upgrade, stored with an absolute finish time. */
@Serializable
data class Upgrade(
    val id: Int,
    val name: String,
    val source: UpgradeSource,
    /** Level being upgraded from (as reported in the export). */
    val level: Int,
    val finishAtEpochSec: Long,
)

/** Everything we persist from one import. */
@Serializable
data class Snapshot(
    val tag: String?,
    val exportedAtEpochSec: Long,
    /** False when the export had no `timestamp` and we assumed import time. */
    val exportTimestampKnown: Boolean,
    val importedAtEpochSec: Long,
    /** Sorted soonest-first. */
    val upgrades: List<Upgrade>,
)
