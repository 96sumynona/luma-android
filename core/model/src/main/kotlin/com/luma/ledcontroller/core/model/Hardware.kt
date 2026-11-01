/*
 * Copyright 2025 The Android Open Source Project
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

package com.luma.ledcontroller.core.model

import kotlinx.serialization.Serializable

/** How the pixels of a strip are laid out in space. Stored for the controller, not yet used. */
@Serializable
enum class LedLayout {
    LINEAR,
    SERPENTINE,
}

/**
 * A controller board.
 *
 * One board can drive several [LumaStrip]s; every strip belongs to exactly one board. The pixel
 * limit lives on the board because it is the board that allocates the buffer, while the active LED
 * count lives on the strip because each strip has its own length.
 *
 * @param pixelLimit maximum pixel count this board is configured to drive, also capped by
 *   [LumaLimits.MAX_LEDS].
 * @param bufferSizeBytes buffer size stored for the controller firmware.
 * @param layout pixel layout stored for the controller firmware.
 */
@Serializable
data class LumaBoard(
    val id: String,
    val name: String,
    val pixelLimit: Int = LumaLimits.DEFAULT_PIXEL_LIMIT,
    val bufferSizeBytes: Int = LumaLimits.DEFAULT_BUFFER_SIZE,
    val layout: LedLayout = LedLayout.LINEAR,
) {
    /** The effective pixel ceiling for strips on this board. */
    val effectivePixelLimit: Int
        get() = pixelLimit.coerceIn(LumaLimits.MIN_LEDS, LumaLimits.MAX_LEDS)
}

/**
 * A single addressable LED strip wired to a [LumaBoard].
 *
 * @param gpio the GPIO the strip's data line is wired to.
 * @param activeLeds how many pixels of the strip are actually driven.
 * @param brightness current output level, `0f..LumaLimits.MAX_BRIGHTNESS`.
 * @param modeId the mode this strip is currently showing, or `null` when the user has not picked
 *   one. The strip deliberately stores a reference, never a copy of the mode.
 */
@Serializable
data class LumaStrip(
    val id: String,
    val boardId: String,
    val name: String,
    val gpio: Int,
    val activeLeds: Int,
    val brightness: Float = LumaLimits.DEFAULT_BRIGHTNESS,
    val modeId: String? = null,
) {
    val brightnessPercent: Int
        get() = BrightnessCurve.percentOf(brightness)
}
