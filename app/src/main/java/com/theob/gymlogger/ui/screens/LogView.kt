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
package com.theob.gymlogger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.Exercise
import com.theob.gymlogger.data.ExerciseDefinition
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.GymView
import com.theob.gymlogger.ui.components.FlowRow
import com.theob.gymlogger.ui.components.SectionHeader
import com.theob.gymlogger.ui.components.SetBadge
import com.theob.gymlogger.ui.components.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogView(viewModel: GymLoggerViewModel) {
    val scrollState = rememberScrollState()
    val exerciseDefinitions by viewModel.exerciseDefinitions.collectAsState()
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        Spacer(Modifier.height(8.dp))

        // Day selector + auto-save status. There is no save button: every edit
        // is written immediately, so we just reassure the user it's handled.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    Text(if (viewModel.isEditingToday) "Today · ${formatDate(viewModel.aDate)}" else formatDate(viewModel.aDate))
                },
                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Auto-saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.aLabel,
            onValueChange = { viewModel.updateLabel(it) },
            label = { Text("Label") },
            placeholder = { Text("recovery, pull, legs…") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.aNote,
            onValueChange = { viewModel.updateNote(it) },
            label = { Text("Session notes") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("EXERCISES")
        Spacer(Modifier.height(12.dp))

        if (viewModel.aExercises.isEmpty()) {
            EmptyExercises()
        }

        viewModel.aExercises.forEach { ex ->
            val isActive = viewModel.curName.isNotBlank() && ex.name.lowercase() == viewModel.curName.lowercase()
            ExerciseCard(
                ex = ex,
                isActive = isActive,
                onActivate = { viewModel.selectExercise(ex.name, ex.definitionId) },
                onUndo = { viewModel.removeLastSet(ex.id) },
                onDelete = { viewModel.deleteExercise(ex.id) },
                curSet = viewModel.curSet,
                onCurSetChange = { viewModel.curSet = it },
                onAddSet = { viewModel.addSet(); focusManager.clearFocus() },
                showNoteInput = viewModel.showNoteInput,
                onShowNote = { viewModel.showNoteInput = true },
                curExNote = viewModel.curExNote,
                onCurExNoteChange = { viewModel.curExNote = it },
                onAddNote = { viewModel.addExNote() }
            )
        }

        // Clearance for the FAB.
        Spacer(Modifier.height(96.dp))
    }

    if (showDatePicker) {
        DayPickerDialog(
            currentIso = viewModel.aDate,
            onDismiss = { showDatePicker = false },
            onPick = { viewModel.loadDay(it) }
        )
    }

    if (viewModel.pickerOpen) {
        ExercisePickerSheet(
            definitions = exerciseDefinitions,
            onSelect = { name, defId ->
                viewModel.selectExercise(name, defId)
                viewModel.pickerOpen = false
            },
            onDefineNew = {
                viewModel.pickerOpen = false
                viewModel.currentView = GymView.ADD_EXERCISE
            },
            onManage = {
                viewModel.pickerOpen = false
                viewModel.currentView = GymView.MANAGE_EXERCISES
            },
            onDismiss = { viewModel.pickerOpen = false }
        )
    }
}

@Composable
private fun EmptyExercises() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No exercises yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap the + Exercise button to start logging. Everything saves automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCard(
    ex: Exercise,
    isActive: Boolean,
    onActivate: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
    curSet: String,
    onCurSetChange: (String) -> Unit,
    onAddSet: () -> Unit,
    showNoteInput: Boolean,
    onShowNote: () -> Unit,
    curExNote: String,
    onCurExNoteChange: (String) -> Unit,
    onAddNote: () -> Unit
) {
    Card(
        onClick = onActivate,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ex.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove exercise", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (ex.sets.isNotEmpty()) {
                FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                    ex.sets.forEach { SetBadge(it) }
                }
            } else if (!isActive) {
                Text("No sets yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (ex.note.isNotEmpty()) {
                Text(
                    ex.note,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (isActive) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = curSet,
                        onValueChange = onCurSetChange,
                        placeholder = { Text("135x5 or 20x10x2") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAddSet() }),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = onAddSet,
                        enabled = curSet.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Add")
                    }
                }
                Text(
                    "135x5 = one set · 20x10x2 = two sets · 30s = freeform",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ex.sets.isNotEmpty()) {
                        TextButton(onClick = onUndo) { Text("Undo last set") }
                    }
                    if (!showNoteInput) {
                        TextButton(onClick = onShowNote) { Text("Add note") }
                    }
                }

                if (showNoteInput) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = curExNote,
                            onValueChange = onCurExNoteChange,
                            placeholder = { Text("ezpz, slow bar, etc") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onAddNote() }),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = onAddNote, shape = MaterialTheme.shapes.medium) { Text("OK") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(currentIso: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val initialMillis = remember(currentIso) {
        runCatching {
            LocalDate.parse(currentIso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(date.toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    definitions: List<ExerciseDefinition>,
    onSelect: (name: String, defId: String) -> Unit,
    onDefineNew: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var filter by remember { mutableStateOf("") }
    val active = definitions.filter { it.active }
    val matches = if (filter.isBlank()) active
        else active.filter { it.name.lowercase().contains(filter.lowercase()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Add exercise", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                matches.forEach { def ->
                    ListItem(
                        headlineContent = { Text(def.name) },
                        trailingContent = {
                            if (def.category.isNotEmpty()) {
                                Text(def.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(def.name, def.id) }
                    )
                }
                if (matches.isEmpty()) {
                    Text(
                        "No matching exercises.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Define new exercise", color = MaterialTheme.colorScheme.primary) },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onDefineNew() }
            )
            ListItem(
                headlineContent = { Text("Manage exercises") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { onManage() }
            )
        }
    }
}
