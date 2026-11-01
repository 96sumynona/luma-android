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

package com.luma.ledcontroller.core.data.transfer

import com.luma.ledcontroller.core.model.BrightnessCurve
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import kotlinx.serialization.Serializable

/**
 * The complete Luma configuration, shaped so it can be serialised, sent to the controller and
 * persisted there across board restarts.
 *
 * Everything is plain, stable data: ids are strings, colours are packed ARGB, repeat days are a bit
 * mask and brightness has an 8.6 fixed point form. Nothing here depends on Android, Room or the UI,
 * which is what lets the same configuration be stored locally now and transferred later.
 */
@Serializable
data class LumaConfigPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val boards: List<LumaBoard> = emptyList(),
    val strips: List<LumaStrip> = emptyList(),
    val modes: List<LumaMode> = emptyList(),
    val schedules: List<LumaSchedule> = emptyList(),
) {
    /** Total active pixels across every strip, used for buffer sizing checks. */
    val totalActiveLeds: Int
        get() = strips.sumOf { it.activeLeds }

    /**
     * Checks that the payload is self-consistent: every strip has its board, every reference
     * resolves and no product limit is exceeded.
     */
    fun validate(): PayloadValidation {
        val problems = mutableListOf<String>()
        val boardIds = boards.map { it.id }.toSet()
        val modeIds = modes.map { it.id }.toSet()
        val stripIds = strips.map { it.id }.toSet()

        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            problems += "Unsupported schema version $schemaVersion"
        }
        strips.forEach { strip ->
            if (strip.boardId !in boardIds) problems += "Strip ${strip.name} has no board"
            if (strip.modeId != null && strip.modeId !in modeIds) {
                problems += "Strip ${strip.name} references a missing mode"
            }
            if (strip.activeLeds !in LumaLimits.MIN_LEDS..LumaLimits.MAX_LEDS) {
                problems += "Strip ${strip.name} has an out of range LED count"
            }
            if (strip.brightness !in 0f..LumaLimits.MAX_BRIGHTNESS) {
                problems += "Strip ${strip.name} is brighter than the ceiling"
            }
        }
        schedules.forEach { schedule ->
            if (schedule.stripId !in stripIds) problems += "Schedule ${schedule.name} has no strip"
            if (schedule.modeId !in modeIds) problems += "Schedule ${schedule.name} has no mode"
            if (!schedule.isValidTime) problems += "Schedule ${schedule.name} has an invalid time"
        }
        modes.forEach { mode ->
            if (!mode.ramp.isValid) problems += "Mode ${mode.name} has an invalid ramp"
        }
        boards.forEach { board ->
            if (board.stripsPixelTotal(strips) > board.effectivePixelLimit) {
                problems += "Board ${board.name} is over its pixel limit"
            }
        }
        return PayloadValidation(problems = problems)
    }

    companion object {
        /** Bumped whenever the transfer contract changes. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/** The result of [LumaConfigPayload.validate]. */
data class PayloadValidation(val problems: List<String>) {
    val isValid: Boolean
        get() = problems.isEmpty()
}

/** Brightness for a strip in the 8.6 fixed point form the controller expects. */
fun LumaStrip.brightnessFixedPoint(): Int = BrightnessCurve.toFixedPoint8_6(brightness)

/** Sum of the active LED counts of the strips that belong to this board. */
fun LumaBoard.stripsPixelTotal(strips: List<LumaStrip>): Int =
    strips.filter { it.boardId == id }.sumOf { it.activeLeds }
