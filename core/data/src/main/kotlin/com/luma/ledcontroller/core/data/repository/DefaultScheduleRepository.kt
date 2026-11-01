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

import com.luma.ledcontroller.core.data.model.asEntity
import com.luma.ledcontroller.core.data.model.asExternalModel
import com.luma.ledcontroller.core.database.dao.ScheduleDao
import com.luma.ledcontroller.core.model.LumaSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
) : ScheduleRepository {

    override val schedules: Flow<List<LumaSchedule>> =
        scheduleDao.observeSchedules().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeSchedulesForStrip(stripId: String): Flow<List<LumaSchedule>> =
        scheduleDao.observeSchedulesForStrip(stripId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override fun observeSchedule(scheduleId: String): Flow<LumaSchedule?> =
        scheduleDao.observeSchedule(scheduleId).map { it?.asExternalModel() }

    override suspend fun getSchedule(scheduleId: String): LumaSchedule? =
        scheduleDao.getSchedule(scheduleId)?.asExternalModel()

    override suspend fun upsertSchedule(schedule: LumaSchedule) {
        scheduleDao.upsertSchedule(schedule.asEntity())
    }

    override suspend fun setEnabled(scheduleId: String, enabled: Boolean) {
        scheduleDao.updateEnabled(scheduleId, enabled)
    }

    override suspend fun deleteSchedule(scheduleId: String) {
        scheduleDao.deleteSchedule(scheduleId)
    }

    override suspend fun deleteSchedulesForStrip(stripId: String) {
        scheduleDao.deleteSchedulesForStrip(stripId)
    }
}
