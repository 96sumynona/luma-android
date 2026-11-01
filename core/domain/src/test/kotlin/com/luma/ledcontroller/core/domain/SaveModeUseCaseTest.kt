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

import com.luma.ledcontroller.core.model.ColorMath
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.ModeAnimation
import com.luma.ledcontroller.core.testing.repository.TestModeRepository
import com.luma.ledcontroller.core.testing.util.TestClock
import com.luma.ledcontroller.core.testing.util.TestIdGenerator
import com.luma.ledcontroller.core.testing.util.requireSaved
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveModeUseCaseTest {

    private val repository = TestModeRepository()
    private val useCase = SaveModeUseCase(
        modeRepository = repository,
        idGenerator = TestIdGenerator(prefix = "mode"),
        clock = TestClock(),
    )
    private val ramp = ColorRamp.solid(ColorMath.rgb(120, 60, 255))

    @Test
    fun savingANewModeGeneratesAnIdAndTimestamps() = runTest {
        val result = useCase(
            modeId = null,
            name = "  Amethyst  ",
            ramp = ramp,
            animation = ModeAnimation(),
        )

        val saved = result.requireSaved()
        assertEquals("mode-0", saved.id)
        assertEquals("Amethyst", saved.name)
        assertEquals(1_700_000_000_000L, saved.createdAtEpochMillis)
    }

    @Test
    fun savingAnExistingModeKeepsItsIdAndCreationTime() = runTest {
        useCase(modeId = null, name = "Amethyst", ramp = ramp, animation = ModeAnimation())
        val existing = repository.getMode("mode-0")!!

        val result = useCase(
            modeId = existing.id,
            name = "Amethyst nights",
            ramp = ramp.withStopColor("stop-0", ColorMath.rgb(255, 0, 100)),
            animation = ModeAnimation(),
        )

        val saved = result.requireSaved()
        assertEquals(existing.id, saved.id)
        assertEquals(existing.createdAtEpochMillis, saved.createdAtEpochMillis)
        assertEquals("Amethyst nights", repository.getMode(existing.id)!!.name)
    }

    @Test
    fun saveAsNewModeCreatesASecondModeAndLeavesTheOriginalAlone() = runTest {
        useCase(modeId = null, name = "Amethyst", ramp = ramp, animation = ModeAnimation())

        val result = useCase(
            modeId = "mode-0",
            name = "Amethyst copy",
            ramp = ramp,
            animation = ModeAnimation(),
            saveAsNewMode = true,
        )

        val saved = result.requireSaved()
        assertEquals("mode-1", saved.id)
        assertEquals("Amethyst", repository.getMode("mode-0")!!.name)
        assertEquals("Amethyst copy", repository.getMode("mode-1")!!.name)
    }

    @Test
    fun anEmptyNameIsRejected() = runTest {
        val result = useCase(modeId = null, name = "   ", ramp = ramp, animation = ModeAnimation())

        val invalid = assertIs<SaveResult.Invalid>(result)
        assertTrue(LumaError.NAME_BLANK in invalid.errors)
    }

    @Test
    fun animationParametersAreNormalisedOnSave() = runTest {
        val result = useCase(
            modeId = null,
            name = "Overdriven",
            ramp = ramp,
            animation = ModeAnimation(speed = 4f, spread = -1f),
        )

        val saved = result.requireSaved()
        assertEquals(1f, saved.animation.speed)
        assertEquals(0f, saved.animation.spread)
    }
}
