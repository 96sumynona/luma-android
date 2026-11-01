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

package com.luma.ledcontroller.core.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.luma.ledcontroller.core.model.AnimationKind

/**
 * A mode row. Colour stops live in [ModeStopEntity] rather than in a copy of the ramp for every
 * strip, which is what keeps modes independently identifiable and reusable.
 */
@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val animationKind: AnimationKind,
    val animationSpeed: Float,
    val animationSpread: Float,
    val animationReverse: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/** One colour stop of a mode's ramp. */
@Entity(
    tableName = "mode_stops",
    primaryKeys = ["modeId", "stopId"],
    foreignKeys = [
        ForeignKey(
            entity = ModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("modeId")],
)
data class ModeStopEntity(
    val modeId: String,
    val stopId: String,
    val position: Float,
    val color: Long,
)

/** A mode with its stops loaded, mirroring the populated-entity pattern used by the app. */
data class PopulatedModeEntity(
    @Embedded val mode: ModeEntity,
    @Relation(parentColumn = "id", entityColumn = "modeId")
    val stops: List<ModeStopEntity>,
)
