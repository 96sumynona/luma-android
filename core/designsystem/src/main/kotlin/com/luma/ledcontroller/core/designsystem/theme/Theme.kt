/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luma.ledcontroller.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The Luma colour scheme.
 *
 * Luma is deliberately a single, brand-fixed dark scheme. The original Now in Android theming hooks
 * (dynamic colour, an "Android" brand, light/dark switching) are not carried over: almost-black
 * purple surfaces are part of the product identity, and a light variant would break the colour
 * previews the app is built around. This is the main theming deviation from the base app and it is
 * deliberate.
 */
private val LumaColorScheme = darkColorScheme(
    primary = LumaPurple,
    onPrimary = Color(0xFF1B0B2E),
    primaryContainer = LumaPurpleContainer,
    onPrimaryContainer = LumaOnPurpleContainer,
    inversePrimary = LumaPurpleDeep,
    secondary = LumaPink,
    onSecondary = Color(0xFF3A0C22),
    secondaryContainer = LumaPinkContainer,
    onSecondaryContainer = LumaOnPinkContainer,
    tertiary = LumaTertiary,
    onTertiary = Color(0xFF2A0F3B),
    tertiaryContainer = LumaTertiaryContainer,
    onTertiaryContainer = LumaOnTertiaryContainer,
    error = LumaError,
    onError = Color(0xFF3B0906),
    errorContainer = LumaErrorContainer,
    onErrorContainer = LumaOnErrorContainer,
    background = LumaInk,
    onBackground = LumaOnSurface,
    surface = LumaSurface,
    onSurface = LumaOnSurface,
    surfaceVariant = LumaSurfaceVariant,
    onSurfaceVariant = LumaOnSurfaceVariant,
    surfaceTint = LumaPurple,
    inverseSurface = LumaInverseSurface,
    inverseOnSurface = LumaInverseOnSurface,
    outline = LumaOutline,
    outlineVariant = LumaOutlineVariant,
    scrim = Color.Black,
)

/**
 * Luma theme.
 *
 * Provides the Material 3 colour scheme plus the background, gradient and tint composition locals
 * that the design system components read.
 */
@Composable
fun LumaTheme(content: @Composable () -> Unit) {
    val gradientColors = GradientColors(
        top = LumaPurple.copy(alpha = 0.22f),
        bottom = LumaPink.copy(alpha = 0.16f),
        container = LumaInk,
    )
    val backgroundTheme = BackgroundTheme(
        color = LumaInk,
        tonalElevation = 0.dp,
    )
    val tintTheme = TintTheme(iconTint = LumaPurple)

    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = LumaColorScheme,
            typography = LumaTypography,
            content = content,
        )
    }
}

/** Surface colour at a given elevation, used by cards so they stack visually. */
@Composable
fun lumaSurfaceAtElevation(elevation: Int): Color =
    MaterialTheme.colorScheme.surfaceColorAtElevation(elevation.dp)
