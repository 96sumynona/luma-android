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

import androidx.room.withTransaction
import com.luma.ledcontroller.core.data.model.asEntity
import com.luma.ledcontroller.core.data.model.asExternalModel
import com.luma.ledcontroller.core.database.LumaDatabase
import com.luma.ledcontroller.core.database.dao.BoardDao
import com.luma.ledcontroller.core.database.dao.ScheduleDao
import com.luma.ledcontroller.core.database.dao.StripDao
import com.luma.ledcontroller.core.model.LumaBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultBoardRepository @Inject constructor(
    private val boardDao: BoardDao,
    private val stripDao: StripDao,
    private val scheduleDao: ScheduleDao,
    private val database: LumaDatabase,
) : BoardRepository {

    override val boards: Flow<List<LumaBoard>> =
        boardDao.observeBoards().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeBoard(boardId: String): Flow<LumaBoard?> =
        boardDao.observeBoard(boardId).map { it?.asExternalModel() }

    override suspend fun getBoard(boardId: String): LumaBoard? =
        boardDao.getBoard(boardId)?.asExternalModel()

    override suspend fun upsertBoard(board: LumaBoard) {
        boardDao.upsertBoard(board.asEntity())
    }

    override suspend fun deleteBoard(boardId: String) {
        // Strips are deleted with their board so no strip can exist without a parent, and their
        // schedules go too so nothing is left pointing at a strip that is gone.
        database.withTransaction {
            stripDao.getStripsForBoard(boardId).forEach { strip ->
                scheduleDao.deleteSchedulesForStrip(strip.id)
            }
            stripDao.deleteStripsForBoard(boardId)
            boardDao.deleteBoard(boardId)
        }
    }
}
