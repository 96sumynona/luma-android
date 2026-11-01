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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorRampTest {

    private val purple = ColorMath.rgb(122, 62, 255)
    private val pink = ColorMath.rgb(255, 94, 178)
    private val black = ColorMath.rgb(0, 0, 0)
    private val white = ColorMath.rgb(255, 255, 255)

    @Test
    fun solidRamp_returnsTheSameColourEverywhere() {
        val ramp = ColorRamp.solid(purple)

        assertEquals(purple, ramp.colorAt(0f))
        assertEquals(purple, ramp.colorAt(50f))
        assertEquals(purple, ramp.colorAt(100f))
        assertTrue(ramp.isSolid)
    }

    @Test
    fun stops_areOrderedByPosition_notByCreationOrder() {
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "stop-2", position = 80f, color = pink),
                ColorStop(id = "stop-0", position = 0f, color = purple),
                ColorStop(id = "stop-1", position = 40f, color = black),
            ),
        )

        assertEquals(listOf("stop-0", "stop-1", "stop-2"), ramp.sortedStops.map { it.id })
    }

    @Test
    fun endpointColours_extendToTheRampEdges() {
        // Both endpoints sit inside the range: 20% and 70%.
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "a", position = 20f, color = purple),
                ColorStop(id = "b", position = 70f, color = pink),
            ),
        )

        assertEquals(purple, ramp.colorAt(0f))
        assertEquals(purple, ramp.colorAt(19.9f))
        assertEquals(pink, ramp.colorAt(70f))
        assertEquals(pink, ramp.colorAt(100f))
        // There is never an undefined section of the ramp.
        val sampled = (0..100).map { ramp.colorAt(it.toFloat()) }
        assertTrue(sampled.all { ColorMath.alphaOf(it) == 255 })
    }

    @Test
    fun movingAnEndpoint_neverLeavesAnUndefinedSection() {
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "a", position = 0f, color = purple),
                ColorStop(id = "b", position = 60f, color = pink),
            ),
        )

        val moved = ramp.withStopMoved(stopId = "a", position = 100f)

        // "a" is pushed back to keep the minimum gap before "b".
        val movedStop = moved.stop("a")!!
        assertTrue(movedStop.position <= 60f - LumaLimits.MIN_STOP_GAP + 0.001f)
        assertEquals(moved.colorAt(movedStop.position), moved.colorAt(0f))
        assertTrue(moved.isValid)
    }

    @Test
    fun stopsCannotShareAPosition() {
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "a", position = 10f, color = purple),
                ColorStop(id = "b", position = 50f, color = pink),
            ),
        )

        val moved = ramp.withStopMoved(stopId = "a", position = 50f)

        val sorted = moved.sortedStops
        assertTrue(sorted[1].position - sorted[0].position >= LumaLimits.MIN_STOP_GAP)
        assertTrue(moved.isValid)
    }

    @Test
    fun addedStop_isNudgedToAFreePosition() {
        val ramp = ColorRamp.solid(purple)

        val added = ramp.withStopAdded(stopId = "new", position = 0f, color = pink)

        assertEquals(2, added.stops.size)
        assertTrue(added.isValid)
        assertTrue(abs(added.stop("new")!!.position) >= LumaLimits.MIN_STOP_GAP)
    }

    @Test
    fun rampSupportsUpToEightStops() {
        var ramp = ColorRamp.solid(purple)
        repeat(20) { index ->
            ramp = ramp.withStopAdded(stopId = "stop-$index", position = 10f + index * 4f, color = pink)
        }

        assertEquals(LumaLimits.MAX_STOPS, ramp.stops.size)
        assertTrue(ramp.isValid)
    }

    @Test
    fun rampAlwaysKeepsAtLeastOneStop() {
        val ramp = ColorRamp.solid(purple)

        assertEquals(1, ramp.withStopRemoved("stop-0").stops.size)
    }

    @Test
    fun recolouringAStopChangesThePreviewImmediately() {
        val ramp = ColorRamp.solid(purple)

        val recoloured = ramp.withStopColor("stop-0", pink)

        assertEquals(pink, recoloured.colorAt(42f))
    }

    @Test
    fun interpolationInOklabKeepsLightnessEven() {
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "black", position = 0f, color = black),
                ColorStop(id = "white", position = 100f, color = white),
            ),
        )

        val midpoint = ramp.colorAt(50f)
        val channel = ColorMath.redOf(midpoint)

        // A perceptual midpoint is a mid grey, not the dark grey linear sRGB interpolation produces.
        assertTrue(channel in 110..150, "Expected a mid grey, got $channel")
        assertEquals(ColorMath.redOf(midpoint), ColorMath.greenOf(midpoint))
        assertEquals(ColorMath.greenOf(midpoint), ColorMath.blueOf(midpoint))
    }

    @Test
    fun samplingProducesTheRequestedNumberOfColours() {
        val ramp = ColorRamp(
            listOf(
                ColorStop(id = "a", position = 0f, color = purple),
                ColorStop(id = "b", position = 100f, color = pink),
            ),
        )

        assertEquals(12, ramp.colors(12).size)
        assertEquals(purple, ramp.colors(12).first())
        assertEquals(pink, ramp.colors(12).last())
        assertEquals(1, ramp.colors(1).size)
    }

    @Test
    fun invalidRampsAreRejected() {
        assertFalse(ColorRamp(emptyList()).isValid)
        assertFalse(ColorRamp.solid(purple).copy(stops = listOf(ColorStop("a", 120f, purple))).isValid)
    }

    @Test
    fun hexRoundTrip() {
        val hex = ColorMath.toHex(pink)

        assertEquals(pink, ColorMath.fromHex(hex))
        assertEquals(null, ColorMath.fromHex("#GGGGGG"))
    }
}
