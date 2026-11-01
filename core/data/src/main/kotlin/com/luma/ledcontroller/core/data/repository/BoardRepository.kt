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

package com.luma.ledcontroller.core.data.repository

import com.luma.ledcontroller.core.model.LumaBoard
import kotlinx.coroutines.flow.Flow

/** Board configuration: the hardware a strip is wired to. */
interface BoardRepository {
    val boards: Flow<List<LumaBoard>>

    fun observeBoard(boardId: String): Flow<LumaBoard?>

    suspend fun getBoard(boardId: String): LumaBoard?

    suspend fun upsertBoard(board: LumaBoard)

    /** Deletes a board and every strip that belongs to it. */
    suspend fun deleteBoard(boardId: String)
}
