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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.DataLogic
import com.theob.gymlogger.data.Session
import com.theob.gymlogger.ui.GymLoggerViewModel
import com.theob.gymlogger.ui.components.FlowRow
import com.theob.gymlogger.ui.components.SetBadge
import com.theob.gymlogger.ui.components.formatDate
import com.theob.gymlogger.ui.components.formatWeight
import kotlin.math.roundToLong

@Composable
fun ExerciseHistoryView(viewModel: GymLoggerViewModel, sessions: List<Session>) {
    val name = viewModel.selectedExerciseName ?: return
    val history = DataLogic.getExerciseHistory(sessions, name)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text("No data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val bestEntry = history.maxByOrNull { it.bestW }
            if (bestEntry != null && bestEntry.bestW > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("BEST", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${formatWeight(bestEntry.bestW)} × ${bestEntry.bestR}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "est 1RM ${(bestEntry.bestW * (1 + bestEntry.bestR.toDouble() / 30)).roundToLong()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            E1rmChart(history)

            history.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDate(entry.date), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            if (entry.label.isNotEmpty()) {
                                Text(entry.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (entry.sets.isNotEmpty()) {
                            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                                entry.sets.forEach { SetBadge(it) }
                            }
                        }
                        if (entry.note.isNotEmpty()) {
                            Text(
                                entry.note,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun E1rmChart(history: List<DataLogic.ExerciseHistoryEntry>) {
    val chronological = history.reversed().filter { it.bestW > 0 }
    if (chronological.size < 2) return

    val e1rms = chronological.map { it.bestW * (1 + it.bestR.toDouble() / 30.0) }
    val minVal = e1rms.min()
    val maxVal = e1rms.max()
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(140.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
                val w = size.width
                val h = size.height
                val step = w / (e1rms.size - 1).toFloat()

                fun xOf(i: Int) = i * step
                fun yOf(v: Double) = h - ((v - minVal) / range * h).toFloat()

                val path = Path()
                e1rms.forEachIndexed { i, v ->
                    val x = xOf(i); val y = yOf(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                e1rms.forEachIndexed { i, v ->
                    drawCircle(color = pointColor, radius = 3.5.dp.toPx(), center = Offset(xOf(i), yOf(v)))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDate(chronological.first().date), style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(formatDate(chronological.last().date), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
            Column(
                modifier = Modifier.fillMaxHeight().padding(start = 8.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${maxVal.roundToLong()}", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text("${minVal.roundToLong()}", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}
