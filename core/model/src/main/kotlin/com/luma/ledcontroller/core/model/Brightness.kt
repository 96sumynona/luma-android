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

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Maps the brightness slider to an output level.
 *
 * LEDs are extremely non-linear to the eye, so a linear slider spends almost all of its travel in
 * the top few percent of output. Luma uses a heavy exponential curve instead: the bottom half of the
 * slider covers the dim end where small changes are actually visible, and the ceiling is
 * [LumaLimits.MAX_BRIGHTNESS] (80%), never full output.
 */
object BrightnessCurve {

    /** Higher values make the low end finer. */
    private const val CURVE_STRENGTH: Double = 6.0

    /** Converts a slider position in `0f..1f` to a brightness in `0f..MAX_BRIGHTNESS`. */
    fun brightnessForSlider(sliderPosition: Float): Float {
        val t = sliderPosition.coerceIn(0f, 1f).toDouble()
        val normalised = (exp(CURVE_STRENGTH * t) - 1.0) / (exp(CURVE_STRENGTH) - 1.0)
        return (normalised * LumaLimits.MAX_BRIGHTNESS).toFloat().coerceIn(0f, LumaLimits.MAX_BRIGHTNESS)
    }

    /** Converts a brightness back to a slider position, so persisted values restore correctly. */
    fun sliderForBrightness(brightness: Float): Float {
        val normalised =
            brightness.coerceIn(0f, LumaLimits.MAX_BRIGHTNESS).toDouble() / LumaLimits.MAX_BRIGHTNESS
        if (normalised <= 0.0) return 0f
        return (ln(1.0 + normalised * (exp(CURVE_STRENGTH) - 1.0)) / CURVE_STRENGTH)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    fun coerce(brightness: Float): Float = brightness.coerceIn(0f, LumaLimits.MAX_BRIGHTNESS)

    /** Percentage of full output (`0..80`), which is what the UI shows. */
    fun percentOf(brightness: Float): Int = (coerce(brightness) * 100f).roundToInt().coerceIn(0, 80)

    /** 8-bit output level, used by the preview swatches. */
    fun toPwmLevel(brightness: Float): Int = (coerce(brightness) * 255f).roundToInt().coerceIn(0, 255)

    /**
     * The value the eventual controller needs: 8-bit output plus 6 fractional bits (8.6 fixed
     * point), so dithering has something to dither with.
     */
    fun toFixedPoint8_6(brightness: Float): Int =
        (coerce(brightness) * (255 shl LumaLimits.OUTPUT_FRACTIONAL_BITS)).roundToInt()

    /** The inverse of [toFixedPoint8_6]. */
    fun fromFixedPoint8_6(value: Int): Float =
        (value.toFloat() / (255 shl LumaLimits.OUTPUT_FRACTIONAL_BITS)).coerceIn(0f, LumaLimits.MAX_BRIGHTNESS)
}
