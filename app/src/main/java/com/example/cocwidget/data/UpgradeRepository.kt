package com.example.cocwidget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "village")
private val SNAPSHOT_KEY = stringPreferencesKey("snapshot")

class UpgradeRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = Json { ignoreUnknownKeys = true }

    val snapshot: Flow<Snapshot?> = appContext.dataStore.data.map { prefs ->
        prefs[SNAPSHOT_KEY]?.let { runCatching { store.decodeFromString<Snapshot>(it) }.getOrNull() }
    }

    suspend fun current(): Snapshot? = snapshot.first()

    /** Parses and saves an export. Throws if the text isn't a valid export. */
    suspend fun import(raw: String): Snapshot {
        val names = GameNames.get(appContext)
        val parsed = ExportParser.parse(raw, nowEpochSec(), names::nameFor)
        appContext.dataStore.edit { it[SNAPSHOT_KEY] = store.encodeToString(Snapshot.serializer(), parsed) }
        return parsed
    }

    suspend fun clear() {
        appContext.dataStore.edit { it.remove(SNAPSHOT_KEY) }
    }
}

fun nowEpochSec(): Long = System.currentTimeMillis() / 1000
