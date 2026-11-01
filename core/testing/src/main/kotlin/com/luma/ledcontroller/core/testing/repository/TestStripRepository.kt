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

import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.model.LumaStrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [StripRepository] for tests. */
class TestStripRepository : StripRepository {
    private val store = MutableStateFlow<List<LumaStrip>>(emptyList())

    override val strips: Flow<List<LumaStrip>> = store

    override fun observeStripsForBoard(boardId: String): Flow<List<LumaStrip>> =
        store.map { strips -> strips.filter { it.boardId == boardId } }

    override fun observeStrip(stripId: String): Flow<LumaStrip?> =
        store.map { strips -> strips.firstOrNull { it.id == stripId } }

    override suspend fun getStrip(stripId: String): LumaStrip? =
        store.value.firstOrNull { it.id == stripId }

    override suspend fun upsertStrip(strip: LumaStrip) {
        store.value = store.value.filterNot { it.id == strip.id } + strip
    }

    override suspend fun setBrightness(stripId: String, brightness: Float) {
        store.value = store.value.map { if (it.id == stripId) it.copy(brightness = brightness) else it }
    }

    override suspend fun setMode(stripId: String, modeId: String?) {
        store.value = store.value.map { if (it.id == stripId) it.copy(modeId = modeId) else it }
    }

    override suspend fun deleteStrip(stripId: String) {
        store.value = store.value.filterNot { it.id == stripId }
    }
}
