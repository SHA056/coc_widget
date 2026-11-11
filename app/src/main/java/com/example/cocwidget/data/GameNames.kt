package com.example.cocwidget.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Game data ID → display name, loaded from assets/names.json.
 * The bundled file is a PARTIAL starter set — replace it with a full,
 * community-maintained table (see README).
 */
class GameNames private constructor(private val names: Map<Int, String>) {

    fun nameFor(id: Int): String? = names[id]

    companion object {
        @Volatile private var instance: GameNames? = null

        fun get(context: Context): GameNames = instance ?: synchronized(this) {
            instance ?: load(context).also { instance = it }
        }

        private fun load(context: Context): GameNames {
            val text = context.assets.open("names.json").bufferedReader().use { it.readText() }
            val raw: Map<String, String> = Json.decodeFromString(text)
            return GameNames(raw.mapKeys { it.key.toInt() })
        }
    }
}
