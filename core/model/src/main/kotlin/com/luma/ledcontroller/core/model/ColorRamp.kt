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
import kotlin.math.abs

/**
 * A single colour position on a [ColorRamp].
 *
 * @param id stable identity so the editor can keep a stop selected while it is dragged past a
 *   neighbour. Ids are app generated and are not used by the controller payload.
 * @param position position along the ramp, `0f..100f`. Endpoint stops are movable too, they are not
 *   pinned to the edges of the range.
 * @param color packed ARGB colour, see [ColorMath].
 */
@Serializable
data class ColorStop(
    val id: String,
    val position: Float,
    val color: Long,
)

/**
 * An ordered set of colour stops that defines a mode's colour output.
 *
 * The ramp follows Blender's gradient ramp behaviour:
 * - one stop produces a solid colour,
 * - multiple stops interpolate between adjacent stops (in OKLab, see [ColorMath]),
 * - stops are ordered by their position, not by the order the user created them,
 * - the colour of the nearest endpoint is extended beyond that endpoint to the ramp edge, so there
 *   is never an undefined section of the ramp, no matter where the endpoints sit,
 * - two stops may not share a position. The editor keeps a minimum gap
 *   ([LumaLimits.MIN_STOP_GAP]) between neighbours so a ramp can never collapse or become
 *   ambiguous.
 */
@Serializable
data class ColorRamp(val stops: List<ColorStop>) {

    /** Stops in ramp order. */
    val sortedStops: List<ColorStop>
        get() = stops.sortedWith(compareBy({ it.position }, { it.id }))

    /** A ramp with a single stop is a solid colour. */
    val isSolid: Boolean
        get() = stops.size == 1

    /** `true` when the ramp can be rendered and transferred. */
    val isValid: Boolean
        get() = stops.size in LumaLimits.MIN_STOPS..LumaLimits.MAX_STOPS &&
            stops.all { it.position in 0f..100f } &&
            sortedStops.zipWithNext().all { (a, b) -> b.position - a.position >= LumaLimits.MIN_STOP_GAP }

    /** The colour produced at [position] (0..100). */
    fun colorAt(position: Float): Long {
        if (stops.isEmpty()) return ColorMath.OPAQUE_BLACK
        val ordered = sortedStops
        val clamped = position.coerceIn(0f, 100f)
        val first = ordered.first()
        val last = ordered.last()
        // Extend the endpoint colours outwards, as Blender does.
        if (clamped <= first.position) return first.color
        if (clamped >= last.position) return last.color

        for (index in 0 until ordered.lastIndex) {
            val from = ordered[index]
            val to = ordered[index + 1]
            if (clamped < from.position || clamped > to.position) continue
            val span = to.position - from.position
            if (span <= 0f) return to.color
            return ColorMath.mix(from.color, to.color, (clamped - from.position) / span)
        }
        return last.color
    }

    /**
     * Samples the ramp at [count] evenly spaced positions, which is exactly what the preview strips
     * and the eventual per-pixel payload need.
     */
    fun colors(count: Int): List<Long> {
        if (count <= 1) return listOf(colorAt(50f))
        return (0 until count).map { index ->
            colorAt(index * 100f / (count - 1))
        }
    }

    fun stop(stopId: String): ColorStop? = stops.firstOrNull { it.id == stopId }

    /** Moves a stop, keeping it inside 0..100 and clear of its neighbours. */
    fun withStopMoved(stopId: String, position: Float): ColorRamp {
        if (stops.none { it.id == stopId }) return this
        val requested = position.coerceIn(0f, 100f)
        val others = stops.filterNot { it.id == stopId }
        val lowestNeighbour = others.filter { it.position < requested }.maxOfOrNull { it.position }
        val highestNeighbour = others.filter { it.position > requested }.minOfOrNull { it.position }

        val minPosition = maxOf((lowestNeighbour ?: -100f) + LumaLimits.MIN_STOP_GAP, 0f)
        val maxPosition = minOf((highestNeighbour ?: 200f) - LumaLimits.MIN_STOP_GAP, 100f)
        val target = if (minPosition <= maxPosition) {
            requested.coerceIn(minPosition, maxPosition)
        } else {
            freePositionNear(requested)
        }
        return copy(stops = stops.map { if (it.id == stopId) it.copy(position = target) else it })
    }

    /** Recolours a stop. The preview updates immediately because the ramp is immutable state. */
    fun withStopColor(stopId: String, color: Long): ColorRamp =
        copy(stops = stops.map { if (it.id == stopId) it.copy(color = color) else it })

    /** Adds a stop if the ramp has room. The position is nudged to the nearest free slot. */
    fun withStopAdded(stopId: String, position: Float, color: Long): ColorRamp {
        if (stops.size >= LumaLimits.MAX_STOPS) return this
        if (stops.any { it.id == stopId }) return this
        return copy(stops = stops + ColorStop(id = stopId, position = freePositionNear(position), color = color))
    }

    /** Removes a stop. A ramp always keeps at least one stop. */
    fun withStopRemoved(stopId: String): ColorRamp {
        if (stops.size <= LumaLimits.MIN_STOPS) return this
        return copy(stops = stops.filterNot { it.id == stopId })
    }

    /** Finds the closest position to [position] that no other stop occupies. */
    fun freePositionNear(position: Float): Float {
        val requested = position.coerceIn(0f, 100f)
        if (isFree(requested)) return requested
        var delta = LumaLimits.MIN_STOP_GAP
        while (delta <= 100f) {
            val above = requested + delta
            if (above <= 100f && isFree(above)) return above
            val below = requested - delta
            if (below >= 0f && isFree(below)) return below
            delta += LumaLimits.MIN_STOP_GAP
        }
        return requested
    }

    private fun isFree(candidate: Float): Boolean =
        stops.none { abs(it.position - candidate) < LumaLimits.MIN_STOP_GAP }

    companion object {
        /** A ramp made of a single stop: one solid colour. */
        fun solid(color: Long, stopId: String = "stop-0"): ColorRamp =
            ColorRamp(listOf(ColorStop(id = stopId, position = 0f, color = color)))

        /** The default ramp for a brand new mode. */
        fun default(): ColorRamp = ColorRamp(
            listOf(
                ColorStop(id = "stop-0", position = 0f, color = ColorMath.rgb(122, 62, 255)),
                ColorStop(id = "stop-1", position = 100f, color = ColorMath.rgb(255, 94, 178)),
            ),
        )
    }
}
