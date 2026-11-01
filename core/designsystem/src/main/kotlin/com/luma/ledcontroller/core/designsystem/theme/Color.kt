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

import androidx.compose.ui.graphics.Color

/**
 * Luma palette.
 *
 * Almost-black purple surfaces with a purple/pink accent pair. The darkest values are close to black
 * on purpose: the app is a controller for light, so the interface should get out of the way of the
 * colour previews instead of competing with them.
 */
internal val LumaInk = Color(0xFF070509)
internal val LumaSurfaceDim = Color(0xFF0A0710)
internal val LumaSurface = Color(0xFF0E0A16)
internal val LumaSurfaceContainerLow = Color(0xFF14101F)
internal val LumaSurfaceContainer = Color(0xFF171222)
internal val LumaSurfaceContainerHigh = Color(0xFF1D1730)
internal val LumaSurfaceContainerHighest = Color(0xFF251D3A)
internal val LumaSurfaceVariant = Color(0xFF221B33)

internal val LumaOnSurface = Color(0xFFF2ECFA)
internal val LumaOnSurfaceVariant = Color(0xFFBCAECE)
internal val LumaOutline = Color(0xFF4B3F5F)
internal val LumaOutlineVariant = Color(0xFF2B2340)

/** Primary: a lit violet, the colour of the app's own accent glow. */
internal val LumaPurple = Color(0xFFC08CFF)
internal val LumaPurpleDeep = Color(0xFF8A4DFF)
internal val LumaPurpleContainer = Color(0xFF3A1F63)
internal val LumaOnPurpleContainer = Color(0xFFEFDCFF)

/** Secondary: the pink that pairs with the violet in every Luma gradient. */
internal val LumaPink = Color(0xFFF77FBA)
internal val LumaPinkContainer = Color(0xFF4C1734)
internal val LumaOnPinkContainer = Color(0xFFFFD9E9)

internal val LumaTertiary = Color(0xFFE4A6FF)
internal val LumaTertiaryContainer = Color(0xFF3F1B57)
internal val LumaOnTertiaryContainer = Color(0xFFF6DBFF)

internal val LumaError = Color(0xFFFFB4AB)
internal val LumaErrorContainer = Color(0xFF5A1512)
internal val LumaOnErrorContainer = Color(0xFFFFDAD6)

internal val LumaInverseSurface = Color(0xFFEDE6F2)
internal val LumaInverseOnSurface = Color(0xFF241C2E)

/** The purple/pink pair used for accent gradients, previews and the home hero. */
val LumaAccentGradient = listOf(LumaPurpleDeep, LumaPurple, LumaPink)
