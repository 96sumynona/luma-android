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
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.luma.ledcontroller.core.database.model.ModeEntity
import com.luma.ledcontroller.core.database.model.ModeStopEntity
import com.luma.ledcontroller.core.database.model.PopulatedModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Transaction
    @Query("SELECT * FROM modes ORDER BY name COLLATE NOCASE ASC")
    fun observeModes(): Flow<List<PopulatedModeEntity>>

    @Transaction
    @Query("SELECT * FROM modes WHERE id = :modeId")
    fun observeMode(modeId: String): Flow<PopulatedModeEntity?>

    @Transaction
    @Query("SELECT * FROM modes WHERE id = :modeId")
    suspend fun getMode(modeId: String): PopulatedModeEntity?

    @Upsert
    suspend fun upsertMode(mode: ModeEntity)

    @Insert
    suspend fun insertStops(stops: List<ModeStopEntity>)

    @Query("DELETE FROM mode_stops WHERE modeId = :modeId")
    suspend fun deleteStops(modeId: String)

    @Query("DELETE FROM modes WHERE id = :modeId")
    suspend fun deleteMode(modeId: String)
}
