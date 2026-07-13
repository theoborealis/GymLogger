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
package com.theob.gymlogger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** True-black AMOLED surfaces carrying the restored original GymLoga accents. */
private val AmoledBrandScheme = BrandDarkScheme.toAmoled()

/**
 * GymLogger is hardcoded to a single look: a true-black AMOLED dark theme built
 * around the original hand-picked accent palette. Wallpaper-based dynamic color
 * is intentionally not used, so the brand accents (gold / green / red) look the
 * same on every device. There is no light theme and no theme picker.
 */
@Composable
fun GymLoggerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AmoledBrandScheme,
        typography = GymTypography,
        shapes = GymShapes,
        content = content,
    )
}
