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
import com.luma.ledcontroller.core.model.LedLayout
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaValidation
import javax.inject.Inject

/** Creates or updates a board after checking the product limits. */
class SaveBoardUseCase @Inject constructor(
    private val boardRepository: BoardRepository,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        boardId: String?,
        name: String,
        pixelLimit: Int,
        bufferSizeBytes: Int,
        layout: LedLayout,
    ): SaveResult<LumaBoard> {
        val errors = LumaValidation.validateBoard(name, pixelLimit, bufferSizeBytes)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        val board = LumaBoard(
            id = boardId ?: idGenerator.newId(),
            name = name.trim(),
            pixelLimit = pixelLimit,
            bufferSizeBytes = bufferSizeBytes,
            layout = layout,
        )
        boardRepository.upsertBoard(board)
        return SaveResult.Saved(board)
    }
}

/** Outcome of a save operation, shared by the editor flows. */
sealed interface SaveResult<out T> {
    data class Saved<T>(val value: T) : SaveResult<T>

    data class Invalid(val errors: Set<LumaError>) : SaveResult<Nothing>
}

/**
 * The saved value, or `null` when the input was rejected.
 *
 * Editors use this instead of matching on [SaveResult.Saved] directly, which keeps call sites free
 * of generic type arguments and makes "save, then navigate away" a single expression.
 */
@Suppress("UNCHECKED_CAST")
fun <T> SaveResult<T>.savedValue(): T? = (this as? SaveResult.Saved<T>)?.value

/** The validation errors, or an empty set when the save succeeded. */
fun SaveResult<*>.errors(): Set<LumaError> =
    (this as? SaveResult.Invalid)?.errors ?: emptySet()
