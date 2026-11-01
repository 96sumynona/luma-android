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

package com.luma.ledcontroller.core.data.transfer

import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Packages the local configuration for the controller.
 *
 * This build does not talk to a board, so nothing here is sent over BLE yet. The payload, its
 * validation and its encoding are implemented and tested because they define the contract the
 * firmware has to accept: the same JSON a future provisioning step would write to the board.
 */
interface LumaConfigTransfer {
    /** Reads the whole local configuration. */
    suspend fun exportConfig(): LumaConfigPayload

    /** Serialises to the transfer format. */
    fun encode(payload: LumaConfigPayload): String

    /** Reads the transfer format back, returning `null` when it cannot be parsed. */
    fun decode(json: String): LumaConfigPayload?
}

class DefaultLumaConfigTransfer @Inject constructor(
    private val boardRepository: BoardRepository,
    private val stripRepository: StripRepository,
    private val modeRepository: ModeRepository,
    private val scheduleRepository: ScheduleRepository,
) : LumaConfigTransfer {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportConfig(): LumaConfigPayload = LumaConfigPayload(
        boards = boardRepository.boards.first(),
        strips = stripRepository.strips.first(),
        modes = modeRepository.modes.first(),
        schedules = scheduleRepository.schedules.first(),
    )

    override fun encode(payload: LumaConfigPayload): String = jsonParser.encodeToString(payload)

    override fun decode(json: String): LumaConfigPayload? =
        runCatching { jsonParser.decodeFromString<LumaConfigPayload>(json) }.getOrNull()
}
