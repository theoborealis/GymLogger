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
package com.theob.gymlogger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theob.gymlogger.data.WorkoutSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Section label, e.g. "EXERCISES" / "SESSIONS". */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** A single set rendered as a Material chip: weight in the accent color, then reps. */
@Composable
fun SetBadge(set: WorkoutSet, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.padding(end = 6.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (set.w == null && set.r == null && set.note != null) {
                Text(
                    set.note,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    formatWeight(set.w),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " × ${set.r ?: 0}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (set.note != null) {
                    Text(
                        "  ${set.note}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.Top,
        content = { content() }
    )
}

/** Drop a trailing ".0" so 135.0 shows as 135 but 22.5 stays 22.5. */
fun formatWeight(w: Double?): String {
    if (w == null) return ""
    return if (w % 1.0 == 0.0) w.toLong().toString() else w.toString()
}

fun formatDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso)
        val pattern = if (date.year != LocalDate.now().year) "EEE, MMM d, yyyy" else "EEE, MMM d"
        date.format(DateTimeFormatter.ofPattern(pattern))
    } catch (e: Exception) {
        iso
    }
}

fun formatVolume(lbs: Long): String =
    if (lbs >= 1000) "${"%.1f".format(lbs / 1000.0)}k lbs" else "$lbs lbs"
