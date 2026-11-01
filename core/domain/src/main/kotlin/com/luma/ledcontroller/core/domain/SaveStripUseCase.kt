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
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.model.BrightnessCurve
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.LumaValidation
import javax.inject.Inject

/**
 * Creates or updates a strip.
 *
 * A strip always belongs to a board, so the board's pixel limit is part of validation: a strip can
 * never be configured longer than the board it is wired to.
 */
class SaveStripUseCase @Inject constructor(
    private val stripRepository: StripRepository,
    private val boardRepository: BoardRepository,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        stripId: String?,
        boardId: String,
        name: String,
        gpio: Int,
        activeLeds: Int,
        brightness: Float,
        modeId: String?,
    ): SaveResult<LumaStrip> {
        val board = boardRepository.getBoard(boardId)
            ?: return SaveResult.Invalid(setOf(LumaError.MODE_REQUIRED))

        val errors = LumaValidation.validateStrip(
            name = name,
            gpio = gpio,
            activeLeds = activeLeds,
            boardPixelLimit = board.effectivePixelLimit,
        )
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        val existing = stripId?.let { stripRepository.getStrip(it) }
        val strip = LumaStrip(
            id = existing?.id ?: idGenerator.newId(),
            boardId = boardId,
            name = name.trim(),
            gpio = gpio,
            activeLeds = activeLeds,
            brightness = BrightnessCurve.coerce(brightness),
            modeId = modeId ?: existing?.modeId,
        )
        stripRepository.upsertStrip(strip)
        return SaveResult.Saved(strip)
    }
}
