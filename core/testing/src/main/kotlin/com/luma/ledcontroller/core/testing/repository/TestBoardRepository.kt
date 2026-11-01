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

package com.luma.ledcontroller.core.testing.repository

import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.model.LumaBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [BoardRepository] for tests. */
class TestBoardRepository : BoardRepository {
    private val store = MutableStateFlow<List<LumaBoard>>(emptyList())

    override val boards: Flow<List<LumaBoard>> = store

    override fun observeBoard(boardId: String): Flow<LumaBoard?> =
        store.map { boards -> boards.firstOrNull { it.id == boardId } }

    override suspend fun getBoard(boardId: String): LumaBoard? =
        store.value.firstOrNull { it.id == boardId }

    override suspend fun upsertBoard(board: LumaBoard) {
        store.value = store.value.filterNot { it.id == board.id } + board
    }

    override suspend fun deleteBoard(boardId: String) {
        store.value = store.value.filterNot { it.id == boardId }
    }
}
