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
package com.theob.gymlogger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.ui.components.formatDate
import com.theob.gymlogger.ui.screens.*
import com.theob.gymlogger.ui.theme.GymLoggerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymLoggerApp(viewModel: GymLoggerViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

    GymLoggerTheme {
        val view = viewModel.currentView
        val isTopLevel = view == GymView.LOG || view == GymView.HISTORY || view == GymView.PRS

        val title = when (view) {
            GymView.LOG -> if (viewModel.isEditingToday) "Today" else formatDate(viewModel.aDate)
            GymView.HISTORY -> "History"
            GymView.PRS -> "Personal records"
            GymView.SESSION_DETAIL -> viewModel.selectedSession?.let { formatDate(it.date) } ?: "Session"
            GymView.EXERCISE_HISTORY -> viewModel.selectedExerciseName ?: "Exercise"
            GymView.ADD_EXERCISE -> if (viewModel.editDefinitionId != null) "Edit exercise" else "New exercise"
            GymView.MANAGE_EXERCISES -> "Exercises"
        }

        val onBack: () -> Unit = {
            when (view) {
                GymView.SESSION_DETAIL -> viewModel.currentView = GymView.HISTORY
                GymView.EXERCISE_HISTORY -> viewModel.currentView = viewModel.exerciseHistorySource
                GymView.ADD_EXERCISE -> {
                    val toManage = viewModel.editDefinitionId != null
                    viewModel.editDefinitionId = null
                    viewModel.currentView = if (toManage) GymView.MANAGE_EXERCISES else GymView.LOG
                }
                GymView.MANAGE_EXERCISES -> viewModel.currentView = GymView.LOG
                else -> {}
            }
        }

        BackHandler(enabled = !isTopLevel) { onBack() }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    )
                )
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        NavigationBarItem(
                            selected = view == GymView.LOG,
                            onClick = { viewModel.goToToday() },
                            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                            label = { Text("Today") }
                        )
                        NavigationBarItem(
                            selected = view == GymView.HISTORY,
                            onClick = { viewModel.currentView = GymView.HISTORY },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text("History") }
                        )
                        NavigationBarItem(
                            selected = view == GymView.PRS,
                            onClick = { viewModel.currentView = GymView.PRS },
                            icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            label = { Text("PRs") }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (view == GymView.LOG) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.pickerOpen = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Exercise") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                when (view) {
                    GymView.LOG -> LogView(viewModel)
                    GymView.HISTORY -> HistoryView(viewModel, snackbarHostState)
                    GymView.PRS -> PRsView(viewModel)
                    GymView.SESSION_DETAIL -> SessionDetailView(viewModel)
                    GymView.EXERCISE_HISTORY -> ExerciseHistoryView(viewModel)
                    GymView.ADD_EXERCISE -> AddExerciseView(viewModel)
                    GymView.MANAGE_EXERCISES -> ManageExercisesView(viewModel)
                }
            }
        }
    }
}
