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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.ExerciseDefinition
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.GymView

@Composable
fun ManageExercisesView(viewModel: GymLoggerViewModel) {
    val definitions by viewModel.exerciseDefinitions.collectAsState()
    val sorted = remember(definitions) {
        definitions.sortedWith(compareBy({ !it.active }, { it.name.lowercase() }))
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Button(
                onClick = {
                    viewModel.editDefinitionId = null
                    viewModel.currentView = GymView.ADD_EXERCISE
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Define new exercise")
            }
        }

        if (definitions.isEmpty()) {
            item {
                Text(
                    "No exercises defined yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        items(sorted, key = { it.id }) { def ->
            ExerciseRow(
                def = def,
                onEdit = {
                    viewModel.editDefinitionId = def.id
                    viewModel.currentView = GymView.ADD_EXERCISE
                },
                onToggleActive = { active -> viewModel.setExerciseActive(def.id, active) }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ExerciseRow(
    def: ExerciseDefinition,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    def.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (def.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (def.category.isNotEmpty()) {
                    Text(def.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit exercise", tint = MaterialTheme.colorScheme.primary)
            }
            Switch(checked = def.active, onCheckedChange = onToggleActive)
        }
    }
}
