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
import com.luma.ledcontroller.core.database.model.StripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StripDao {
    @Query("SELECT * FROM strips ORDER BY name COLLATE NOCASE ASC")
    fun observeStrips(): Flow<List<StripEntity>>

    @Query("SELECT * FROM strips WHERE boardId = :boardId ORDER BY name COLLATE NOCASE ASC")
    fun observeStripsForBoard(boardId: String): Flow<List<StripEntity>>

    @Query("SELECT * FROM strips WHERE boardId = :boardId")
    suspend fun getStripsForBoard(boardId: String): List<StripEntity>

    @Query("SELECT * FROM strips WHERE id = :stripId")
    fun observeStrip(stripId: String): Flow<StripEntity?>

    @Query("SELECT * FROM strips WHERE id = :stripId")
    suspend fun getStrip(stripId: String): StripEntity?

    @Upsert
    suspend fun upsertStrip(strip: StripEntity)

    @Query("UPDATE strips SET brightness = :brightness WHERE id = :stripId")
    suspend fun updateBrightness(stripId: String, brightness: Float)

    @Query("UPDATE strips SET modeId = :modeId WHERE id = :stripId")
    suspend fun updateMode(stripId: String, modeId: String?)

    @Query("UPDATE strips SET modeId = NULL WHERE modeId = :modeId")
    suspend fun clearMode(modeId: String)

    @Query("DELETE FROM strips WHERE id = :stripId")
    suspend fun deleteStrip(stripId: String)

    @Query("DELETE FROM strips WHERE boardId = :boardId")
    suspend fun deleteStripsForBoard(boardId: String)
}
