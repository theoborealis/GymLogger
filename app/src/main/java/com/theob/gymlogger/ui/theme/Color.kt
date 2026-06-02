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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Fallback Material You palette used below Android 12, where wallpaper-based
 * dynamic color is unavailable. A warm "training amber" seed, nodding to the
 * original GymLoga accent while staying within Material 3 tonal conventions.
 */
val FallbackDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF482900),
    primaryContainer = Color(0xFF673D00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFE1C1A4),
    onSecondary = Color(0xFF402C18),
    secondaryContainer = Color(0xFF59422C),
    onSecondaryContainer = Color(0xFFFEDDBE),
    tertiary = Color(0xFFB5CEA0),
    onTertiary = Color(0xFF223515),
    tertiaryContainer = Color(0xFF384B29),
    onTertiaryContainer = Color(0xFFD1EBBB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF18120B),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF18120B),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF504539),
    onSurfaceVariant = Color(0xFFD4C4B5),
    outline = Color(0xFF9C8E80),
    outlineVariant = Color(0xFF4F4539),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    inversePrimary = Color(0xFF885200),
)

/**
 * Remap the neutral surface ladder to near-black so OLED pixels actually switch
 * off, while preserving the (dynamic or fallback) accent roles. This is what
 * makes the theme genuinely AMOLED rather than "dark grey everywhere": cards and
 * sheets still read as distinct elevations, just against true black.
 */
fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242428),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF121215),
    surfaceContainerHigh = Color(0xFF1A1A1E),
    surfaceContainerHighest = Color(0xFF232328),
    surfaceVariant = Color(0xFF1C1C20),
    scrim = Color.Black,
)
