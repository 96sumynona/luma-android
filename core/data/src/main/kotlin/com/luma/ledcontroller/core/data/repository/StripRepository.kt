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

import com.luma.ledcontroller.core.model.LumaStrip
import kotlinx.coroutines.flow.Flow

/** Strip configuration: one strip belongs to one board and references one mode. */
interface StripRepository {
    val strips: Flow<List<LumaStrip>>

    fun observeStripsForBoard(boardId: String): Flow<List<LumaStrip>>

    fun observeStrip(stripId: String): Flow<LumaStrip?>

    suspend fun getStrip(stripId: String): LumaStrip?

    suspend fun upsertStrip(strip: LumaStrip)

    suspend fun setBrightness(stripId: String, brightness: Float)

    suspend fun setMode(stripId: String, modeId: String?)

    suspend fun deleteStrip(stripId: String)
}
