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

import com.luma.ledcontroller.core.model.LumaMode
import kotlinx.coroutines.flow.Flow

/**
 * Named, reusable modes. A mode owns its ramp and animation; strips and schedules only reference it.
 */
interface ModeRepository {
    val modes: Flow<List<LumaMode>>

    fun observeMode(modeId: String): Flow<LumaMode?>

    suspend fun getMode(modeId: String): LumaMode?

    /** Inserts or replaces a mode together with its stops. */
    suspend fun upsertMode(mode: LumaMode)

    /**
     * Deletes a mode. Strips that referenced it lose their mode, and schedules that referenced it
     * are deleted, so no dangling references are left behind.
     */
    suspend fun deleteMode(modeId: String)
}
