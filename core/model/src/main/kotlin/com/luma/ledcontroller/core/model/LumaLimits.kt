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

/**
 * Product limits for Luma.
 *
 * These are the constraints the UI enforces. They exist because the eventual WS2812B controller
 * has to fit the same configuration into a small, fixed-size buffer, so the app never lets a user
 * build a configuration the firmware could not accept.
 */
object LumaLimits {
    /** Minimum active LED count for a strip. */
    const val MIN_LEDS: Int = 1

    /**
     * Hard UI limit for both the active LED count of a strip and the pixel limit of a board.
     *
     * This is an app-side limit only; it is not a claim about what any particular board can drive.
     */
    const val MAX_LEDS: Int = 1_000

    /**
     * Sanity bound for the GPIO field. The valid pin map is a property of the controller firmware,
     * so the app only refuses obviously impossible values.
     */
    const val MIN_GPIO: Int = 0
    const val MAX_GPIO: Int = 99

    /** A ramp is either a single solid colour or a gradient of up to eight stops. */
    const val MIN_STOPS: Int = 1
    const val MAX_STOPS: Int = 8

    /** Smallest gap (in ramp percent) two stops may have, so ramps never collapse. */
    const val MIN_STOP_GAP: Float = 0.5f

    /** Brightness ceiling. Luma never drives LEDs above 80% of full output. */
    const val MAX_BRIGHTNESS: Float = 0.8f

    /** Brightness applied to a freshly created strip. */
    const val DEFAULT_BRIGHTNESS: Float = 0.5f

    /** Sanity bound for the controller buffer size, in bytes. */
    const val MIN_BUFFER_SIZE: Int = 1
    const val MAX_BUFFER_SIZE: Int = 65_535

    /** Pixel limit a new board starts with. */
    const val DEFAULT_PIXEL_LIMIT: Int = MAX_LEDS

    /** Buffer size a new board starts with. Placeholder until the firmware contract is fixed. */
    const val DEFAULT_BUFFER_SIZE: Int = 512

    /** Active LED count a new strip starts with. UI seed value only. */
    const val DEFAULT_ACTIVE_LEDS: Int = 60

    /** Maximum transition length, in seconds. */
    const val MIN_TRANSITION_SECONDS: Int = 0
    const val MAX_TRANSITION_SECONDS: Int = 600

    /** Number of fractional bits used by the eventual controller's 8.6 fixed point output. */
    const val OUTPUT_FRACTIONAL_BITS: Int = 6
}
