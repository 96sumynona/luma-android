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

package com.luma.ledcontroller.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.luma.ledcontroller.core.database.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour ASC, minute ASC")
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE stripId = :stripId ORDER BY hour ASC, minute ASC")
    fun observeSchedulesForStrip(stripId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    fun observeSchedule(scheduleId: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getSchedule(scheduleId: String): ScheduleEntity?

    @Upsert
    suspend fun upsertSchedule(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :scheduleId")
    suspend fun updateEnabled(scheduleId: String, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: String)

    @Query("DELETE FROM schedules WHERE modeId = :modeId")
    suspend fun deleteSchedulesForMode(modeId: String)

    @Query("DELETE FROM schedules WHERE stripId = :stripId")
    suspend fun deleteSchedulesForStrip(stripId: String)
}
