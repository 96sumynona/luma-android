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

import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.model.LumaMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ModeRepository] for tests. */
class TestModeRepository : ModeRepository {
    private val store = MutableStateFlow<List<LumaMode>>(emptyList())

    override val modes: Flow<List<LumaMode>> = store

    override fun observeMode(modeId: String): Flow<LumaMode?> =
        store.map { modes -> modes.firstOrNull { it.id == modeId } }

    override suspend fun getMode(modeId: String): LumaMode? =
        store.value.firstOrNull { it.id == modeId }

    override suspend fun upsertMode(mode: LumaMode) {
        store.value = store.value.filterNot { it.id == mode.id } + mode
    }

    override suspend fun deleteMode(modeId: String) {
        store.value = store.value.filterNot { it.id == modeId }
    }
}
