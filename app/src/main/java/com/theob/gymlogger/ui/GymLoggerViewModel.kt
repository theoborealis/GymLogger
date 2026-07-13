package com.theob.gymlogger.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theob.gymlogger.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.time.LocalDate

sealed class ExportImportEvent {
    object ExportSuccess : ExportImportEvent()
    data class ExportFailure(val message: String) : ExportImportEvent()
    data class ImportSuccess(val added: Int) : ExportImportEvent()
    data class ImportFailure(val message: String) : ExportImportEvent()
}

enum class GymView { LOG, HISTORY, PRS, SESSION_DETAIL, EXERCISE_HISTORY, ADD_EXERCISE, MANAGE_EXERCISES }

class GymLoggerViewModel(private val repository: SessionRepository) : ViewModel() {
    private val _exportImportEvents = MutableSharedFlow<ExportImportEvent>()
    val exportImportEvents: SharedFlow<ExportImportEvent> = _exportImportEvents.asSharedFlow()

    val sessions: StateFlow<List<Session>> = repository.sessionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val exerciseDefinitions: StateFlow<List<ExerciseDefinition>> = repository.exerciseDefinitionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var currentView by mutableStateOf(GymView.LOG)

    // The session the Log screen is currently bound to. When null, the next edit
    // creates a fresh session for `aDate`. There is no "save" — every mutation
    // upserts this session immediately (see persist()).
    var editSessionId by mutableStateOf<String?>(null)

    // Log form state — the live draft for the day being edited.
    var aDate by mutableStateOf(LocalDate.now().toString())
    var aLabel by mutableStateOf("")
    var aNote by mutableStateOf("")
    val aExercises = mutableStateListOf<Exercise>()

    var curName by mutableStateOf("")
    var curDefinitionId by mutableStateOf<String?>(null)
    var curSet by mutableStateOf("")
    var curExNote by mutableStateOf("")
    var showNoteInput by mutableStateOf(false)
    var editDefinitionId by mutableStateOf<String?>(null)

    // Whether the "add exercise" bottom sheet is open (driven by the Log FAB).
    var pickerOpen by mutableStateOf(false)

    // Detail / navigation state
    var selectedSession by mutableStateOf<Session?>(null)
    var selectedExerciseName by mutableStateOf<String?>(null)
    var exerciseHistorySource by mutableStateOf(GymView.HISTORY)

    val isEditingToday: Boolean
        get() = aDate == LocalDate.now().toString()

    init {
        // On launch, bind the Log screen to today's existing session (if any)
        // so the user resumes exactly where they left off.
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = repository.sessionsFlow.first().find { it.date == today }
            if (existing != null && editSessionId == null &&
                aExercises.isEmpty() && aLabel.isBlank() && aNote.isBlank()
            ) {
                bindSession(existing)
            }
        }
    }

    // ── Auto-save ────────────────────────────────────────────────────────────

    /**
     * Write the current draft straight into storage. Called after every edit.
     * If the day has no content left, any session we created for it is removed.
     */
    private fun persist() {
        val exercises = aExercises.toList()
        val hasContent = exercises.isNotEmpty() || aLabel.isNotBlank() || aNote.isNotBlank()
        val currentId = editSessionId

        if (!hasContent) {
            if (currentId != null) {
                editSessionId = null
                viewModelScope.launch { repository.deleteSession(currentId) }
            }
            return
        }

        val id = currentId ?: java.util.UUID.randomUUID().toString().also { editSessionId = it }
        val session = Session(
            id = id,
            date = aDate,
            label = aLabel.trim(),
            note = aNote.trim(),
            exercises = exercises
        )
        viewModelScope.launch { repository.upsertSession(session) }
    }

    private var persistJob: Job? = null

    /** Persist now, cancelling any pending debounced write. Used for discrete taps. */
    private fun persistNow() {
        persistJob?.cancel()
        persistJob = null
        persist()
    }

    /**
     * Coalesce rapid text edits (label / notes) into a single write ~300ms after
     * typing stops, instead of serializing the whole dataset on every keystroke.
     */
    private fun debouncedPersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(300)
            persist()
        }
    }

    private fun bindSession(session: Session) {
        aDate = session.date
        aLabel = session.label
        aNote = session.note
        aExercises.clear()
        aExercises.addAll(session.exercises)
        editSessionId = session.id
        resetCurrent()
    }

    private fun resetCurrent() {
        curName = ""
        curDefinitionId = null
        curSet = ""
        curExNote = ""
        showNoteInput = false
    }

    /** Switch the Log editor to a given day, loading its session or starting fresh. */
    fun loadDay(date: String) {
        persistNow() // flush the current draft before switching days
        val existing = sessions.value.find { it.date == date }
        if (existing != null) {
            bindSession(existing)
        } else {
            aDate = date
            aLabel = ""
            aNote = ""
            aExercises.clear()
            editSessionId = null
            resetCurrent()
        }
    }

    /** Bottom-nav "Today": jump back to today unless already editing it. */
    fun goToToday() {
        val today = LocalDate.now().toString()
        if (aDate != today) loadDay(today)
        currentView = GymView.LOG
    }

    fun updateLabel(value: String) {
        aLabel = value
        debouncedPersist()
    }

    fun updateNote(value: String) {
        aNote = value
        debouncedPersist()
    }

    // ── Exercise editing ───────────────────────────────────────────────────────

    fun addSet() {
        if (curName.isBlank() || curSet.isBlank()) return
        val sets = DataLogic.parseSets(curSet)
        if (sets.isEmpty()) return

        val existingIndex = aExercises.indexOfFirst { it.name.lowercase() == curName.trim().lowercase() }
        if (existingIndex >= 0) {
            val ex = aExercises[existingIndex]
            aExercises[existingIndex] = ex.copy(sets = ex.sets + sets)
        } else {
            aExercises.add(Exercise(name = curName.trim(), sets = sets, definitionId = curDefinitionId))
        }
        curSet = ""
        persistNow()
    }

    fun selectExercise(name: String, definitionId: String? = null) {
        val trimmed = name.trim()
        curName = trimmed
        curDefinitionId = definitionId
        curSet = ""
        curExNote = ""
        showNoteInput = false
        if (aExercises.none { it.name.lowercase() == trimmed.lowercase() }) {
            aExercises.add(Exercise(name = trimmed, sets = emptyList(), definitionId = definitionId))
            persistNow()
        }
    }

    fun clearCurrentExercise() {
        val removed = aExercises.removeAll { it.name.lowercase() == curName.lowercase() && it.sets.isEmpty() }
        resetCurrent()
        if (removed) persistNow()
    }

    fun addExNote() {
        if (curName.isBlank() || curExNote.isBlank()) return
        val existingIndex = aExercises.indexOfFirst { it.name.lowercase() == curName.trim().lowercase() }
        if (existingIndex >= 0) {
            val oldNote = aExercises[existingIndex].note
            val newNote = if (oldNote.isEmpty()) curExNote.trim() else "$oldNote\n${curExNote.trim()}"
            aExercises[existingIndex] = aExercises[existingIndex].copy(note = newNote)
            persistNow()
        }
        curExNote = ""
        showNoteInput = false
    }

    fun removeLastSet(exerciseId: String) {
        val index = aExercises.indexOfFirst { it.id == exerciseId }
        if (index >= 0) {
            val ex = aExercises[index]
            if (ex.sets.size > 1) {
                aExercises[index] = ex.copy(sets = ex.sets.dropLast(1))
            } else {
                aExercises.removeAt(index)
            }
            persistNow()
        }
    }

    fun deleteExercise(exerciseId: String) {
        val removed = aExercises.removeAll { it.id == exerciseId }
        if (removed) persistNow()
    }

    // ── Exercise definitions ─────────────────────────────────────────────────

    fun setExerciseActive(defId: String, active: Boolean) {
        viewModelScope.launch { repository.setExerciseDefinitionActive(defId, active) }
    }

    fun updateExerciseDefinition(defId: String, newName: String, newCategory: String) {
        val existing = exerciseDefinitions.value.find { it.id == defId } ?: return
        viewModelScope.launch {
            repository.updateExerciseDefinition(defId, newName.trim(), newCategory.trim(), existing.name)
        }
    }

    fun addExerciseDefinition(name: String, category: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = exerciseDefinitions.value
            if (existing.none { it.name.lowercase() == trimmed.lowercase() }) {
                repository.saveExerciseDefinitions(existing + ExerciseDefinition(name = trimmed, category = category.trim()))
            }
            selectExercise(trimmed)
            currentView = GymView.LOG
        }
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    /** Open a past (or any) session in the Log editor; edits continue to auto-save. */
    fun editSession(session: Session) {
        persistNow() // flush the current draft before binding another session
        bindSession(session)
        currentView = GymView.LOG
    }

    fun deleteSession(sessionId: String) {
        persistJob?.cancel() // drop any pending write so it can't resurrect the session
        viewModelScope.launch { repository.deleteSession(sessionId) }
        if (editSessionId == sessionId) {
            // We just deleted the day the editor was bound to — reset to a blank today.
            aDate = LocalDate.now().toString()
            aLabel = ""
            aNote = ""
            aExercises.clear()
            editSessionId = null
            resetCurrent()
        }
        currentView = GymView.HISTORY
    }

    // ── Export / Import ──────────────────────────────────────────────────────

    fun exportToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri)?.use { repository.exportToStream(it) }
                _exportImportEvents.emit(ExportImportEvent.ExportSuccess)
            } catch (e: Exception) {
                Log.e("GymLoggerViewModel", "Export failed", e)
                _exportImportEvents.emit(ExportImportEvent.ExportFailure(e.message ?: "Unknown error"))
            }
        }
    }

    fun importFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = contentResolver.openInputStream(uri)?.use { repository.importFromStream(it) }
                    ?: run {
                        _exportImportEvents.emit(ExportImportEvent.ImportFailure("Could not open file"))
                        return@launch
                    }
                val existingIds = sessions.value.map { it.id }.toSet()
                val newSessions = imported.filter { it.id !in existingIds }
                repository.saveSessions(sessions.value + newSessions)
                _exportImportEvents.emit(ExportImportEvent.ImportSuccess(newSessions.size))
            } catch (e: SerializationException) {
                Log.e("GymLoggerViewModel", "Import failed", e)
                _exportImportEvents.emit(ExportImportEvent.ImportFailure("Invalid file format"))
            } catch (e: Exception) {
                Log.e("GymLoggerViewModel", "Import failed", e)
                _exportImportEvents.emit(ExportImportEvent.ImportFailure(e.message ?: "Unknown error"))
            }
        }
    }
}
