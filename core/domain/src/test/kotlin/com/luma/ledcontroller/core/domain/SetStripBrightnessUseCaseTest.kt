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

import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.testing.repository.TestStripRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SetStripBrightnessUseCaseTest {

    private val stripRepository = TestStripRepository()
    private val useCase = SetStripBrightnessUseCase(stripRepository)

    @Test
    fun brightnessNeverGoesAboveTheCeiling() = runTest {
        stripRepository.upsertStrip(
            LumaStrip(id = "strip-1", boardId = "board-1", name = "Shelf", gpio = 18, activeLeds = 60),
        )

        useCase(stripId = "strip-1", brightness = 1f)

        assertEquals(LumaLimits.MAX_BRIGHTNESS, stripRepository.getStrip("strip-1")!!.brightness)
    }

    @Test
    fun brightnessNeverGoesBelowZero() = runTest {
        stripRepository.upsertStrip(
            LumaStrip(id = "strip-1", boardId = "board-1", name = "Shelf", gpio = 18, activeLeds = 60),
        )

        useCase(stripId = "strip-1", brightness = -2f)

        assertEquals(0f, stripRepository.getStrip("strip-1")!!.brightness)
    }
}
