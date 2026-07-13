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

/*
 * The original GymLoga accent palette, restored. The Material You redesign had
 * swapped these for wallpaper-derived dynamic color (and a warm amber fallback);
 * here we go back to the hand-picked brand accents so the app looks the same on
 * every device, regardless of wallpaper.
 *
 *   Gold  — the dominant accent (titles, active borders, primary buttons, FAB,
 *           set weights, dates, chart line)            -> primary
 *   Green — personal records / positive / chart points -> tertiary
 *   Red   — clear / delete / destructive               -> error
 *   Blue  — kept from the original palette for completeness
 */
private val BrandGold  = Color(0xFFC9983A)
private val BrandGreen = Color(0xFF4D8F5F)
private val BrandRed   = Color(0xFFB8483A)
@Suppress("unused")
private val BrandBlue  = Color(0xFF5080AA)

/**
 * Fixed dark scheme seeded from the original GymLoga accents. Neutral surfaces
 * here are placeholders — [toAmoled] remaps the whole surface ladder to true
 * black before the scheme reaches MaterialTheme.
 */
val BrandDarkScheme: ColorScheme = darkColorScheme(
    primary = BrandGold,
    onPrimary = Color(0xFF2A1C00),
    primaryContainer = Color(0xFF534009),
    onPrimaryContainer = Color(0xFFF4DBA6),
    inversePrimary = Color(0xFF7A5A12),

    // Muted-gold sibling so tonal buttons stay on-brand rather than drifting blue.
    secondary = Color(0xFFD8BE93),
    onSecondary = Color(0xFF3A2E12),
    secondaryContainer = Color(0xFF4A3D26),
    onSecondaryContainer = Color(0xFFEFD9BC),

    tertiary = BrandGreen,
    onTertiary = Color(0xFF06250F),
    tertiaryContainer = Color(0xFF20402A),
    onTertiaryContainer = Color(0xFFA9DDB8),

    error = BrandRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5C1A12),
    onErrorContainer = Color(0xFFFFDAD3),

    background = Color(0xFF14110C),
    onBackground = Color(0xFFE7E1D8),
    surface = Color(0xFF14110C),
    onSurface = Color(0xFFE7E1D8),
    surfaceVariant = Color(0xFF494136),
    onSurfaceVariant = Color(0xFFADA398),
    outline = Color(0xFF8C8478),
    outlineVariant = Color(0xFF3B362E),
    inverseSurface = Color(0xFFE7E1D8),
    inverseOnSurface = Color(0xFF332F28),
)

/**
 * Remap the neutral surface ladder to near-black so OLED pixels actually switch
 * off, while preserving the accent roles. This is what makes the theme genuinely
 * AMOLED rather than "dark grey everywhere": cards and sheets still read as
 * distinct elevations, just against true black.
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
