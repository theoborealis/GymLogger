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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.DataLogic
import com.theob.gymlogger.data.Session
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.GymView
import com.theob.gymlogger.ui.components.formatDate
import com.theob.gymlogger.ui.components.formatWeight
import kotlin.math.roundToLong

@Composable
fun PRsView(viewModel: GymLoggerViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val prs = DataLogic.getAllPRs(sessions)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(8.dp)) }

        if (prs.isEmpty()) {
            item {
                Text(
                    "Log some sessions to see your PRs here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        items(prs, key = { it.name }) { pr ->
            Card(
                onClick = {
                    viewModel.selectedExerciseName = pr.name
                    viewModel.exerciseHistorySource = GymView.PRS
                    viewModel.currentView = GymView.EXERCISE_HISTORY
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(pr.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${pr.totalSessions} session${if (pr.totalSessions != 1) "s" else ""} · ${pr.totalSets} set${if (pr.totalSets != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatBlock(
                            label = "HEAVIEST",
                            value = formatWeight(pr.bestW),
                            sub = "× ${pr.bestWR} · ${formatDate(pr.bestWDate)}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatBlock(
                            label = "EST 1RM",
                            value = pr.bestE1rm.roundToLong().toString(),
                            sub = "${formatWeight(pr.bestE1rmW)}×${pr.bestE1rmR} · ${formatDate(pr.bestE1rmDate)}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatBlock(label: String, value: String, sub: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
