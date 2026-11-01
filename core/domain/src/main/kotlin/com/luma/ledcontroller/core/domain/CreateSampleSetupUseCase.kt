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

package com.luma.ledcontroller.core.domain

import com.luma.ledcontroller.core.data.IdGenerator
import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.model.AnimationKind
import com.luma.ledcontroller.core.model.BrightnessCurve
import com.luma.ledcontroller.core.model.ColorMath
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.ColorStop
import com.luma.ledcontroller.core.model.LedLayout
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.ModeAnimation
import com.luma.ledcontroller.core.model.Weekday
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Adds a small, clearly labelled sample setup.
 *
 * This exists so the interface can be evaluated on a fresh install. It is a user action in Settings,
 * never seeded automatically, and everything it creates can be edited or deleted like any other
 * configuration.
 */
class CreateSampleSetupUseCase @Inject constructor(
    private val boardRepository: BoardRepository,
    private val stripRepository: StripRepository,
    private val modeRepository: ModeRepository,
    private val scheduleRepository: ScheduleRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(): String {
        val now = clock.now().toEpochMilliseconds()

        val amethyst = LumaMode(
            id = idGenerator.newId(),
            name = "Amethyst drift",
            ramp = ColorRamp(
                listOf(
                    ColorStop("amethyst-0", 0f, ColorMath.rgb(96, 42, 214)),
                    ColorStop("amethyst-1", 55f, ColorMath.rgb(196, 86, 255)),
                    ColorStop("amethyst-2", 100f, ColorMath.rgb(255, 118, 186)),
                ),
            ),
            animation = ModeAnimation(kind = AnimationKind.CHASE, speed = 0.28f, spread = 0.6f),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val sunset = LumaMode(
            id = idGenerator.newId(),
            name = "Sunset",
            ramp = ColorRamp(
                listOf(
                    ColorStop("sunset-0", 0f, ColorMath.rgb(255, 170, 92)),
                    ColorStop("sunset-1", 50f, ColorMath.rgb(255, 96, 132)),
                    ColorStop("sunset-2", 100f, ColorMath.rgb(122, 62, 255)),
                ),
            ),
            animation = ModeAnimation(kind = AnimationKind.BREATHE, speed = 0.18f),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val focus = LumaMode(
            id = idGenerator.newId(),
            name = "Focus",
            ramp = ColorRamp.solid(ColorMath.rgb(255, 214, 240)),
            animation = ModeAnimation(kind = AnimationKind.SOLID),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        listOf(amethyst, sunset, focus).forEach { modeRepository.upsertMode(it) }

        val board = LumaBoard(
            id = idGenerator.newId(),
            name = "Sample board",
            pixelLimit = 300,
            bufferSizeBytes = 512,
            layout = LedLayout.LINEAR,
        )
        boardRepository.upsertBoard(board)

        val sofaCove = LumaStrip(
            id = idGenerator.newId(),
            boardId = board.id,
            name = "Sofa cove",
            gpio = 5,
            activeLeds = 120,
            brightness = BrightnessCurve.brightnessForSlider(0.55f),
            modeId = sunset.id,
        )
        val shelf = LumaStrip(
            id = idGenerator.newId(),
            boardId = board.id,
            name = "Shelf",
            gpio = 18,
            activeLeds = 60,
            brightness = BrightnessCurve.brightnessForSlider(0.35f),
            modeId = amethyst.id,
        )
        listOf(sofaCove, shelf).forEach { stripRepository.upsertStrip(it) }

        scheduleRepository.upsertSchedule(
            LumaSchedule(
                id = idGenerator.newId(),
                name = "Weeknight glow",
                stripId = sofaCove.id,
                modeId = sunset.id,
                hour = 18,
                minute = 30,
                repeatDays = setOf(
                    Weekday.MONDAY,
                    Weekday.TUESDAY,
                    Weekday.WEDNESDAY,
                    Weekday.THURSDAY,
                    Weekday.FRIDAY,
                ),
                transitionSeconds = 45,
            ),
        )
        scheduleRepository.upsertSchedule(
            LumaSchedule(
                id = idGenerator.newId(),
                name = "Wind down",
                stripId = shelf.id,
                modeId = focus.id,
                hour = 22,
                minute = 45,
                repeatDays = Weekday.Weekdays.toSet(),
                transitionSeconds = 120,
            ),
        )
        return "Sample setup added"
    }
}
