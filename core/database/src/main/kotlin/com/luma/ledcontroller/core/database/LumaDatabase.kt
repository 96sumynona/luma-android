/*
 * Copyright 2022 The Android Open Source Project
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

package com.luma.ledcontroller.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luma.ledcontroller.core.database.dao.BoardDao
import com.luma.ledcontroller.core.database.dao.ModeDao
import com.luma.ledcontroller.core.database.dao.ScheduleDao
import com.luma.ledcontroller.core.database.dao.StripDao
import com.luma.ledcontroller.core.database.model.BoardEntity
import com.luma.ledcontroller.core.database.model.ModeEntity
import com.luma.ledcontroller.core.database.model.ModeStopEntity
import com.luma.ledcontroller.core.database.model.ScheduleEntity
import com.luma.ledcontroller.core.database.model.StripEntity
import com.luma.ledcontroller.core.database.util.LumaTypeConverters

/**
 * The local, offline-first store for every piece of Luma configuration: boards, strips, modes and
 * schedules. Wi-Fi credentials live in DataStore instead, because the password is encrypted before
 * it is written.
 */
@Database(
    entities = [
        BoardEntity::class,
        StripEntity::class,
        ModeEntity::class,
        ModeStopEntity::class,
        ScheduleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LumaTypeConverters::class)
abstract class LumaDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun stripDao(): StripDao
    abstract fun modeDao(): ModeDao
    abstract fun scheduleDao(): ScheduleDao
}
