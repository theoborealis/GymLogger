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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.DataLogic
import com.theob.gymlogger.data.Session
import com.theob.gymlogger.ui.ExportImportEvent
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.GymView
import com.theob.gymlogger.ui.components.FlowRow
import com.theob.gymlogger.ui.components.SectionHeader
import com.theob.gymlogger.ui.components.formatDate
import com.theob.gymlogger.ui.components.formatVolume

@Composable
fun HistoryView(viewModel: GymLoggerViewModel, snackbarHostState: SnackbarHostState) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    val allNames = remember(sessions) { DataLogic.getAllExerciseNames(sessions) }
    val sortedSessions = remember(sessions) { sessions.sortedByDescending { it.date } }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportToUri(it, context.contentResolver) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFromUri(it, context.contentResolver) } }

    LaunchedEffect(Unit) {
        viewModel.exportImportEvents.collect { event ->
            val message = when (event) {
                is ExportImportEvent.ExportSuccess -> "Exported successfully"
                is ExportImportEvent.ExportFailure -> "Export failed: ${event.message}"
                is ExportImportEvent.ImportSuccess -> "Imported ${event.added} new session(s)"
                is ExportImportEvent.ImportFailure -> "Import failed: ${event.message}"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("gymlogger-export.json") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Export") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Import") }
            }
        }

        if (allNames.isNotEmpty()) {
            item {
                SectionHeader("EXERCISES", modifier = Modifier.padding(bottom = 8.dp))
                FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    allNames.forEach { name ->
                        AssistChip(
                            onClick = {
                                viewModel.selectedExerciseName = name
                                viewModel.exerciseHistorySource = GymView.HISTORY
                                viewModel.currentView = GymView.EXERCISE_HISTORY
                            },
                            label = { Text(name) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }

        item { SectionHeader("SESSIONS", modifier = Modifier.padding(bottom = 10.dp)) }

        if (sessions.isEmpty()) {
            item {
                Text(
                    "No sessions yet. Head to Today and start logging.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        items(sortedSessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                onClick = {
                    viewModel.selectedSession = session
                    viewModel.currentView = GymView.SESSION_DETAIL
                }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(session.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                val vol = DataLogic.getSessionVolume(session)
                Text(
                    "${session.exercises.size} ex" + if (vol > 0) " · ${formatVolume(vol)}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (session.label.isNotEmpty()) {
                Text(
                    session.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (session.exercises.isNotEmpty()) {
                Text(
                    session.exercises.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (session.note.isNotEmpty()) {
                Text(
                    session.note,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
