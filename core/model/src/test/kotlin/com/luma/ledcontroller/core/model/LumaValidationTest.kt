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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LumaValidationTest {

    private val ramp = ColorRamp.solid(ColorMath.rgb(1, 2, 3))

    @Test
    fun ledCountIsCappedAtOneThousand() {
        assertTrue(LumaValidation.validateStrip("Kitchen", 5, 1_000, 1_000).isEmpty())
        assertEquals(
            setOf(LumaError.LED_COUNT_OUT_OF_RANGE),
            LumaValidation.validateStrip("Kitchen", 5, 1_001, 1_000),
        )
        assertEquals(
            setOf(LumaError.LED_COUNT_OUT_OF_RANGE),
            LumaValidation.validateStrip("Kitchen", 5, 0, 1_000),
        )
    }

    @Test
    fun stripCannotExceedItsBoardsPixelLimit() {
        assertEquals(
            setOf(LumaError.LED_COUNT_EXCEEDS_PIXEL_LIMIT),
            LumaValidation.validateStrip("Shelf", 4, 300, boardPixelLimit = 150),
        )
    }

    @Test
    fun boardFieldsAreBounded() {
        assertTrue(LumaValidation.validateBoard("Living room", 500, 512).isEmpty())
        assertEquals(
            setOf(LumaError.PIXEL_LIMIT_OUT_OF_RANGE),
            LumaValidation.validateBoard("Living room", 2_000, 512),
        )
        assertEquals(
            setOf(LumaError.BUFFER_OUT_OF_RANGE),
            LumaValidation.validateBoard("Living room", 500, 0),
        )
        assertEquals(
            setOf(LumaError.NAME_BLANK),
            LumaValidation.validateBoard("  ", 500, 512),
        )
    }

    @Test
    fun modesNeedAnameAndAValidRamp() {
        assertTrue(LumaValidation.validateMode("Amethyst", ramp, ModeAnimation()).isEmpty())
        assertEquals(
            setOf(LumaError.NAME_BLANK),
            LumaValidation.validateMode("", ramp, ModeAnimation()),
        )
        assertEquals(
            setOf(LumaError.RAMP_INVALID),
            LumaValidation.validateMode("Amethyst", ColorRamp(emptyList()), ModeAnimation()),
        )
    }

    @Test
    fun schedulesNeedAStripAModeAndRepeatDays() {
        assertTrue(
            LumaValidation.validateSchedule(
                stripId = "strip",
                modeId = "mode",
                hour = 7,
                minute = 30,
                repeatDays = setOf(Weekday.MONDAY),
                transitionSeconds = 0,
            ).isEmpty(),
        )
        val errors = LumaValidation.validateSchedule(
            stripId = null,
            modeId = null,
            hour = 7,
            minute = 30,
            repeatDays = emptySet(),
            transitionSeconds = 10,
        )
        assertTrue(LumaError.MODE_REQUIRED in errors)
        assertTrue(LumaError.NO_REPEAT_DAYS in errors)
    }

    @Test
    fun wifiCredentialsCannotBeBlank() {
        assertEquals(
            setOf(LumaError.SSID_BLANK, LumaError.PASSWORD_BLANK),
            LumaValidation.validateWifi("", ""),
        )
        assertTrue(LumaValidation.validateWifi("Home", "hunter2").isEmpty())
    }

    @Test
    fun ledsFitBoardMatchesTheProductLimits() {
        assertTrue(LumaValidation.ledsFitBoard(activeLeds = 150, boardPixelLimit = 150))
        assertTrue(!LumaValidation.ledsFitBoard(activeLeds = 151, boardPixelLimit = 150))
        assertTrue(!LumaValidation.ledsFitBoard(activeLeds = 1_001, boardPixelLimit = 1_000))
    }
}
