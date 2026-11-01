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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Repeat days are packed into [repeatDaysMask] so the same value can be handed to the controller.
 */
@Entity(
    tableName = "schedules",
    indices = [Index("stripId"), Index("modeId")],
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stripId: String,
    val modeId: String,
    val hour: Int,
    val minute: Int,
    val repeatDaysMask: Int,
    val transitionSeconds: Int,
    val enabled: Boolean,
)
