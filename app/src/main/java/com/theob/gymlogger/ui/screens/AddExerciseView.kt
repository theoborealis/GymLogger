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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.GymView

@Composable
fun AddExerciseView(viewModel: GymLoggerViewModel) {
    val defId = viewModel.editDefinitionId
    val isEdit = defId != null
    val existingDef = remember(defId) {
        if (defId != null) viewModel.exerciseDefinitions.value.find { it.id == defId } else null
    }

    var name by remember(defId) { mutableStateOf(existingDef?.name ?: "") }
    var category by remember(defId) { mutableStateOf(existingDef?.category ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise name") },
            placeholder = { Text("e.g. Bench Press") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (optional)") },
            placeholder = { Text("push, pull, legs, cardio…") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isEdit && defId != null) {
                    viewModel.updateExerciseDefinition(defId, name, category)
                    viewModel.editDefinitionId = null
                    viewModel.currentView = GymView.MANAGE_EXERCISES
                } else {
                    viewModel.addExerciseDefinition(name, category)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (isEdit) "Save changes" else "Add exercise")
        }
    }
}
