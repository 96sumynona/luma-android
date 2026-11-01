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
 * Validation problems that the UI turns into inline error text.
 *
 * Validation lives in the model so that the same rules apply to the app, the transfer payload and
 * the eventual controller sync, instead of being re-implemented per screen.
 */
enum class LumaError {
    NAME_BLANK,
    LED_COUNT_OUT_OF_RANGE,
    PIXEL_LIMIT_OUT_OF_RANGE,
    LED_COUNT_EXCEEDS_PIXEL_LIMIT,
    GPIO_OUT_OF_RANGE,
    BUFFER_OUT_OF_RANGE,
    MODE_REQUIRED,
    NO_REPEAT_DAYS,
    TRANSITION_OUT_OF_RANGE,
    TIME_OUT_OF_RANGE,
    RAMP_INVALID,
    SSID_BLANK,
    PASSWORD_BLANK,
}

/**
 * Every product rule Luma enforces, in one place.
 *
 * Each `validate...` function returns the set of problems for that object, which is empty when the
 * object can be persisted.
 */
object LumaValidation {

    fun validateBoard(name: String, pixelLimit: Int, bufferSizeBytes: Int): Set<LumaError> = buildSet {
        if (name.isBlank()) add(LumaError.NAME_BLANK)
        if (pixelLimit !in LumaLimits.MIN_LEDS..LumaLimits.MAX_LEDS) add(LumaError.PIXEL_LIMIT_OUT_OF_RANGE)
        if (bufferSizeBytes !in LumaLimits.MIN_BUFFER_SIZE..LumaLimits.MAX_BUFFER_SIZE) {
            add(LumaError.BUFFER_OUT_OF_RANGE)
        }
    }

    fun validateStrip(
        name: String,
        gpio: Int,
        activeLeds: Int,
        boardPixelLimit: Int,
    ): Set<LumaError> = buildSet {
        if (name.isBlank()) add(LumaError.NAME_BLANK)
        if (gpio !in LumaLimits.MIN_GPIO..LumaLimits.MAX_GPIO) add(LumaError.GPIO_OUT_OF_RANGE)
        if (activeLeds !in LumaLimits.MIN_LEDS..LumaLimits.MAX_LEDS) add(LumaError.LED_COUNT_OUT_OF_RANGE)
        if (activeLeds > boardPixelLimit.coerceAtMost(LumaLimits.MAX_LEDS)) {
            add(LumaError.LED_COUNT_EXCEEDS_PIXEL_LIMIT)
        }
    }

    fun validateMode(name: String, ramp: ColorRamp, animation: ModeAnimation): Set<LumaError> = buildSet {
        if (name.isBlank()) add(LumaError.NAME_BLANK)
        if (!ramp.isValid) add(LumaError.RAMP_INVALID)
        if (animation.speed !in 0f..1f || animation.spread !in 0f..1f) add(LumaError.RAMP_INVALID)
    }

    fun validateSchedule(
        stripId: String?,
        modeId: String?,
        hour: Int,
        minute: Int,
        repeatDays: Set<Weekday>,
        transitionSeconds: Int,
    ): Set<LumaError> = buildSet {
        if (stripId.isNullOrBlank()) add(LumaError.MODE_REQUIRED)
        if (modeId.isNullOrBlank()) add(LumaError.MODE_REQUIRED)
        if (hour !in 0..23 || minute !in 0..59) add(LumaError.TIME_OUT_OF_RANGE)
        if (repeatDays.isEmpty()) add(LumaError.NO_REPEAT_DAYS)
        if (transitionSeconds !in LumaLimits.MIN_TRANSITION_SECONDS..LumaLimits.MAX_TRANSITION_SECONDS) {
            add(LumaError.TRANSITION_OUT_OF_RANGE)
        }
    }

    fun validateWifi(ssid: String, password: String): Set<LumaError> = buildSet {
        if (ssid.isBlank()) add(LumaError.SSID_BLANK)
        if (password.isBlank()) add(LumaError.PASSWORD_BLANK)
    }

    /** `true` when [activeLeds] fits both the app limit and the board's pixel limit. */
    fun ledsFitBoard(activeLeds: Int, boardPixelLimit: Int): Boolean =
        activeLeds in LumaLimits.MIN_LEDS..minOf(boardPixelLimit, LumaLimits.MAX_LEDS)
}
