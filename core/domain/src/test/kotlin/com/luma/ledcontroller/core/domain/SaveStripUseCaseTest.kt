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

import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaLimits
import com.luma.ledcontroller.core.testing.repository.TestBoardRepository
import com.luma.ledcontroller.core.testing.repository.TestStripRepository
import com.luma.ledcontroller.core.testing.util.TestIdGenerator
import com.luma.ledcontroller.core.testing.util.requireSaved
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveStripUseCaseTest {

    private val boardRepository = TestBoardRepository()
    private val stripRepository = TestStripRepository()
    private val useCase = SaveStripUseCase(
        stripRepository = stripRepository,
        boardRepository = boardRepository,
        idGenerator = TestIdGenerator(prefix = "strip"),
    )

    private suspend fun createBoard(pixelLimit: Int = 300) {
        boardRepository.upsertBoard(LumaBoard(id = "board-1", name = "Sample board", pixelLimit = pixelLimit))
    }

    @Test
    fun aStripIsSavedAgainstItsBoard() = runTest {
        createBoard()

        val result = useCase(
            stripId = null,
            boardId = "board-1",
            name = "Sofa cove",
            gpio = 5,
            activeLeds = 120,
            brightness = 0.5f,
            modeId = null,
        )

        val saved = result.requireSaved()
        assertEquals("board-1", saved.boardId)
        assertEquals(120, saved.activeLeds)
    }

    @Test
    fun aStripCannotBeLongerThanItsBoardsPixelLimit() = runTest {
        createBoard(pixelLimit = 150)

        val result = useCase(
            stripId = null,
            boardId = "board-1",
            name = "Sofa cove",
            gpio = 5,
            activeLeds = 151,
            brightness = 0.5f,
            modeId = null,
        )

        val invalid = assertIs<SaveResult.Invalid>(result)
        assertTrue(LumaError.LED_COUNT_EXCEEDS_PIXEL_LIMIT in invalid.errors)
    }

    @Test
    fun brightnessIsClampedToTheCeiling() = runTest {
        createBoard()

        val result = useCase(
            stripId = null,
            boardId = "board-1",
            name = "Sofa cove",
            gpio = 5,
            activeLeds = 30,
            brightness = 1.4f,
            modeId = null,
        )

        val saved = result.requireSaved()
        assertEquals(LumaLimits.MAX_BRIGHTNESS, saved.brightness)
    }

    @Test
    fun theAppLimitAppliesEvenWhenTheBoardAllowsMore() = runTest {
        createBoard(pixelLimit = LumaLimits.MAX_LEDS)

        val result = useCase(
            stripId = null,
            boardId = "board-1",
            name = "Too long",
            gpio = 5,
            activeLeds = LumaLimits.MAX_LEDS + 1,
            brightness = 0.2f,
            modeId = null,
        )

        assertTrue(LumaError.LED_COUNT_OUT_OF_RANGE in assertIs<SaveResult.Invalid>(result).errors)
    }

    @Test
    fun anUnknownBoardIsRejected() = runTest {
        val result = useCase(
            stripId = null,
            boardId = "missing",
            name = "Orphan",
            gpio = 5,
            activeLeds = 30,
            brightness = 0.5f,
            modeId = null,
        )

        assertIs<SaveResult.Invalid>(result)
    }
}
