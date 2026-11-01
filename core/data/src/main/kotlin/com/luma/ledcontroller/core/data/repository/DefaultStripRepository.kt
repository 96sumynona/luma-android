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
import com.luma.ledcontroller.core.database.dao.StripDao
import com.luma.ledcontroller.core.model.LumaStrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DefaultStripRepository @Inject constructor(
    private val stripDao: StripDao,
) : StripRepository {

    override val strips: Flow<List<LumaStrip>> =
        stripDao.observeStrips().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeStripsForBoard(boardId: String): Flow<List<LumaStrip>> =
        stripDao.observeStripsForBoard(boardId).map { entities -> entities.map { it.asExternalModel() } }

    override fun observeStrip(stripId: String): Flow<LumaStrip?> =
        stripDao.observeStrip(stripId).map { it?.asExternalModel() }

    override suspend fun getStrip(stripId: String): LumaStrip? =
        stripDao.getStrip(stripId)?.asExternalModel()

    override suspend fun upsertStrip(strip: LumaStrip) {
        stripDao.upsertStrip(strip.asEntity())
    }

    override suspend fun setBrightness(stripId: String, brightness: Float) {
        stripDao.updateBrightness(stripId, brightness)
    }

    override suspend fun setMode(stripId: String, modeId: String?) {
        stripDao.updateMode(stripId, modeId)
    }

    override suspend fun deleteStrip(stripId: String) {
        stripDao.deleteStrip(stripId)
    }
}
