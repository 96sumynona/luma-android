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

package com.luma.ledcontroller.core.data.model

import com.luma.ledcontroller.core.database.model.BoardEntity
import com.luma.ledcontroller.core.database.model.ModeEntity
import com.luma.ledcontroller.core.database.model.ModeStopEntity
import com.luma.ledcontroller.core.database.model.PopulatedModeEntity
import com.luma.ledcontroller.core.database.model.ScheduleEntity
import com.luma.ledcontroller.core.database.model.StripEntity
import com.luma.ledcontroller.core.model.ColorRamp
import com.luma.ledcontroller.core.model.ColorStop
import com.luma.ledcontroller.core.model.LumaBoard
import com.luma.ledcontroller.core.model.LumaMode
import com.luma.ledcontroller.core.model.LumaSchedule
import com.luma.ledcontroller.core.model.LumaStrip
import com.luma.ledcontroller.core.model.ModeAnimation
import com.luma.ledcontroller.core.model.Weekday

/** Entity to model mapping, kept in one place so the data layer is the only place that knows SQL. */

fun BoardEntity.asExternalModel(): LumaBoard = LumaBoard(
    id = id,
    name = name,
    pixelLimit = pixelLimit,
    bufferSizeBytes = bufferSizeBytes,
    layout = layout,
)

fun LumaBoard.asEntity(): BoardEntity = BoardEntity(
    id = id,
    name = name,
    pixelLimit = pixelLimit,
    bufferSizeBytes = bufferSizeBytes,
    layout = layout,
)

fun StripEntity.asExternalModel(): LumaStrip = LumaStrip(
    id = id,
    boardId = boardId,
    name = name,
    gpio = gpio,
    activeLeds = activeLeds,
    brightness = brightness,
    modeId = modeId,
)

fun LumaStrip.asEntity(): StripEntity = StripEntity(
    id = id,
    boardId = boardId,
    name = name,
    gpio = gpio,
    activeLeds = activeLeds,
    brightness = brightness,
    modeId = modeId,
)

fun PopulatedModeEntity.asExternalModel(): LumaMode = LumaMode(
    id = mode.id,
    name = mode.name,
    ramp = ColorRamp(
        stops = stops
            .sortedBy { it.position }
            .map { ColorStop(id = it.stopId, position = it.position, color = it.color) },
    ),
    animation = ModeAnimation(
        kind = mode.animationKind,
        speed = mode.animationSpeed,
        spread = mode.animationSpread,
        reverse = mode.animationReverse,
    ),
    createdAtEpochMillis = mode.createdAtEpochMillis,
    updatedAtEpochMillis = mode.updatedAtEpochMillis,
)

fun LumaMode.asEntity(): ModeEntity = ModeEntity(
    id = id,
    name = name,
    animationKind = animation.kind,
    animationSpeed = animation.speed,
    animationSpread = animation.spread,
    animationReverse = animation.reverse,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun LumaMode.stopsAsEntities(): List<ModeStopEntity> = ramp.stops.map { stop ->
    ModeStopEntity(
        modeId = id,
        stopId = stop.id,
        position = stop.position,
        color = stop.color,
    )
}

fun ScheduleEntity.asExternalModel(): LumaSchedule = LumaSchedule(
    id = id,
    name = name,
    stripId = stripId,
    modeId = modeId,
    hour = hour,
    minute = minute,
    repeatDays = Weekday.fromBitMask(repeatDaysMask),
    transitionSeconds = transitionSeconds,
    enabled = enabled,
)

fun LumaSchedule.asEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    name = name,
    stripId = stripId,
    modeId = modeId,
    hour = hour,
    minute = minute,
    repeatDaysMask = Weekday.toBitMask(repeatDays),
    transitionSeconds = transitionSeconds,
    enabled = enabled,
)
