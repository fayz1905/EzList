package com.ezlist.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "ezlist_prefs")

class PersistenceManager(private val context: Context) {
    private val dataStore = context.dataStore
    private val CHECKLISTS_KEY = stringPreferencesKey("checklists")

    suspend fun saveChecklists(checklists: List<DemoChecklist>) {
        val json = Json.encodeToString(checklists)
        dataStore.edit { preferences ->
            preferences[CHECKLISTS_KEY] = json
        }
    }

    suspend fun loadChecklists(): List<DemoChecklist>? {
        return try {
            val preferences = dataStore.data.first()
            val json = preferences[CHECKLISTS_KEY]
            if (json != null) {
                Json.decodeFromString(json)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
