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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrightnessCurveTest {

    @Test
    fun sliderEndsMapToZeroAndTheCeiling() {
        assertEquals(0f, BrightnessCurve.brightnessForSlider(0f))
        assertEquals(LumaLimits.MAX_BRIGHTNESS, BrightnessCurve.brightnessForSlider(1f))
    }

    @Test
    fun brightnessNeverExceedsTheCeiling() {
        (0..100).forEach { step ->
            val brightness = BrightnessCurve.brightnessForSlider(step / 100f)
            assertTrue(brightness <= LumaLimits.MAX_BRIGHTNESS)
        }
        assertEquals(LumaLimits.MAX_BRIGHTNESS, BrightnessCurve.coerce(1f))
    }

    @Test
    fun theLowEndIsHeavilyExponential() {
        // The bottom quarter of the slider must stay in the dim, useful range.
        assertTrue(BrightnessCurve.brightnessForSlider(0.25f) < 0.01f)
        // ...and the top quarter still spans a big range of output.
        assertTrue(BrightnessCurve.brightnessForSlider(0.75f) > 0.1f)
    }

    @Test
    fun curveIsMonotonic() {
        var previous = -1f
        (0..100).forEach { step ->
            val brightness = BrightnessCurve.brightnessForSlider(step / 100f)
            assertTrue(brightness >= previous)
            previous = brightness
        }
    }

    @Test
    fun sliderRoundTripsThroughPersistedBrightness() {
        listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 1f).forEach { slider ->
            val brightness = BrightnessCurve.brightnessForSlider(slider)
            val restored = BrightnessCurve.sliderForBrightness(brightness)
            assertTrue(abs(restored - slider) < 0.001f, "Round trip failed for $slider -> $restored")
        }
    }

    @Test
    fun percentIsRelativeToFullOutput() {
        assertEquals(0, BrightnessCurve.percentOf(0f))
        assertEquals(50, BrightnessCurve.percentOf(0.5f))
        assertEquals(80, BrightnessCurve.percentOf(LumaLimits.MAX_BRIGHTNESS))
        assertEquals(80, BrightnessCurve.percentOf(1f))
    }

    @Test
    fun fixedPointUsesEightBitOutputPlusSixFractionalBits() {
        assertEquals(0, BrightnessCurve.toFixedPoint8_6(0f))
        assertEquals(255 shl 6, BrightnessCurve.toFixedPoint8_6(LumaLimits.MAX_BRIGHTNESS))
        assertTrue(BrightnessCurve.toFixedPoint8_6(0.8f) % 64 == 0)
        assertEquals(0.8f, BrightnessCurve.fromFixedPoint8_6(BrightnessCurve.toFixedPoint8_6(0.8f)))
    }
}
