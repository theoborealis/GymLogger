/*
* Copyright (C) 2026 Michael Bosse
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.theob.gymlogger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.InputStream
import java.io.OutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymlogger_prefs")

class SessionRepository(private val context: Context) {
    private val SESSIONS_KEY = stringPreferencesKey("gymlogger:sessions")

    private fun parseData(json: String): GymLogaData {
        val element = Json.parseToJsonElement(json)
        val data = if (element is JsonArray) {
            GymLogaData(sessions = Json.decodeFromJsonElement(element))
        } else {
            Json.decodeFromJsonElement(element)
        }
        return when {
            data.exerciseDefinitions.isNotEmpty() -> data
            data.sessions.isNotEmpty() -> {
                // Migrate: seed definitions from existing session history
                val seeded = DataLogic.getAllExerciseNames(data.sessions).map { ExerciseDefinition(name = it) }
                data.copy(exerciseDefinitions = seeded)
            }
            else -> {
                // Fresh install: provide a starter set
                val defaults = listOf("Bench Press", "Overhead Press", "Power Clean", "Deadlift", "Back Squat")
                    .map { ExerciseDefinition(name = it) }
                data.copy(exerciseDefinitions = defaults)
            }
        }
    }

    /** Read the current persisted data inside a transaction, tolerating corruption. */
    private fun readPrefs(preferences: Preferences): GymLogaData =
        try {
            preferences[SESSIONS_KEY]?.let { parseData(it) } ?: GymLogaData(sessions = emptyList())
        } catch (e: Exception) {
            GymLogaData(sessions = emptyList())
        }

    private val dataFlow: Flow<GymLogaData> = context.dataStore.data.map { preferences ->
        val json = preferences[SESSIONS_KEY] ?: "[]"
        try {
            parseData(json)
        } catch (e: SerializationException) {
            android.util.Log.e("SessionRepository", "Failed to deserialize data", e)
            GymLogaData(sessions = emptyList())
        }
    }

    val sessionsFlow: Flow<List<Session>> = dataFlow.map { it.sessions }

    val exerciseDefinitionsFlow: Flow<List<ExerciseDefinition>> = dataFlow.map { it.exerciseDefinitions }

    suspend fun saveSessions(sessions: List<Session>) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(sessions = sessions))
        }
    }

    /**
     * Insert or replace a single session by id, atomically. This is the backbone
     * of auto-save: the read-modify-write happens inside the DataStore edit
     * transaction, so rapid successive edits never race or duplicate.
     */
    suspend fun upsertSession(session: Session) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            val index = current.sessions.indexOfFirst { it.id == session.id }
            val updated = if (index >= 0) {
                current.sessions.toMutableList().also { it[index] = session }
            } else {
                listOf(session) + current.sessions
            }
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(sessions = updated))
        }
    }

    /** Remove a session by id, atomically. */
    suspend fun deleteSession(id: String) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            preferences[SESSIONS_KEY] =
                Json.encodeToString(current.copy(sessions = current.sessions.filter { it.id != id }))
        }
    }

    suspend fun saveExerciseDefinitions(defs: List<ExerciseDefinition>) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = defs))
        }
    }

    suspend fun renameExerciseDefinition(defId: String, oldName: String, newName: String) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(name = newName) else it
            }
            val updatedSessions = DataLogic.applyRename(current.sessions, defId, oldName, newName)
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs, sessions = updatedSessions))
        }
    }

    suspend fun updateExerciseDefinition(defId: String, newName: String, newCategory: String, oldName: String) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            val nameChanged = newName != oldName
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(name = newName, category = newCategory) else it
            }
            val updatedSessions = if (nameChanged)
                DataLogic.applyRename(current.sessions, defId, oldName, newName)
            else
                current.sessions
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs, sessions = updatedSessions))
        }
    }

    suspend fun setExerciseDefinitionActive(defId: String, active: Boolean) {
        context.dataStore.edit { preferences ->
            val current = readPrefs(preferences)
            val updatedDefs = current.exerciseDefinitions.map {
                if (it.id == defId) it.copy(active = active) else it
            }
            preferences[SESSIONS_KEY] = Json.encodeToString(current.copy(exerciseDefinitions = updatedDefs))
        }
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val data = dataFlow.first()
        val prettyJson = kotlinx.serialization.json.Json { prettyPrint = true }
        outputStream.bufferedWriter().use { it.write(prettyJson.encodeToString(data)) }
    }

    fun importFromStream(inputStream: InputStream): List<Session> {
        val text = inputStream.bufferedReader().use { it.readText() }
        val element = Json.parseToJsonElement(text)
        return if (element is JsonArray) {
            Json.decodeFromJsonElement(element)
        } else {
            Json.decodeFromJsonElement<GymLogaData>(element).sessions
        }
    }
}
