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
import com.luma.ledcontroller.core.data.model.stopsAsEntities
import com.luma.ledcontroller.core.database.LumaDatabase
import com.luma.ledcontroller.core.database.dao.ModeDao
import com.luma.ledcontroller.core.database.dao.ScheduleDao
import com.luma.ledcontroller.core.database.dao.StripDao
import com.luma.ledcontroller.core.model.LumaMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultModeRepository @Inject constructor(
    private val modeDao: ModeDao,
    private val stripDao: StripDao,
    private val scheduleDao: ScheduleDao,
    private val database: LumaDatabase,
) : ModeRepository {

    override val modes: Flow<List<LumaMode>> =
        modeDao.observeModes().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeMode(modeId: String): Flow<LumaMode?> =
        modeDao.observeMode(modeId).map { it?.asExternalModel() }

    override suspend fun getMode(modeId: String): LumaMode? =
        modeDao.getMode(modeId)?.asExternalModel()

    override suspend fun upsertMode(mode: LumaMode) {
        // The mode row and its stops are always written together, so a ramp can never be half saved.
        database.withTransaction {
            modeDao.upsertMode(mode.asEntity())
            modeDao.deleteStops(mode.id)
            modeDao.insertStops(mode.stopsAsEntities())
        }
    }

    override suspend fun deleteMode(modeId: String) {
        database.withTransaction {
            stripDao.clearMode(modeId)
            scheduleDao.deleteSchedulesForMode(modeId)
            modeDao.deleteMode(modeId)
        }
    }
}
