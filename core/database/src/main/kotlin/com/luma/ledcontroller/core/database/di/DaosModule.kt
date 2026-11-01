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

package com.luma.ledcontroller.core.database.di

import com.luma.ledcontroller.core.database.LumaDatabase
import com.luma.ledcontroller.core.database.dao.BoardDao
import com.luma.ledcontroller.core.database.dao.ModeDao
import com.luma.ledcontroller.core.database.dao.ScheduleDao
import com.luma.ledcontroller.core.database.dao.StripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesBoardDao(database: LumaDatabase): BoardDao = database.boardDao()

    @Provides
    fun providesStripDao(database: LumaDatabase): StripDao = database.stripDao()

    @Provides
    fun providesModeDao(database: LumaDatabase): ModeDao = database.modeDao()

    @Provides
    fun providesScheduleDao(database: LumaDatabase): ScheduleDao = database.scheduleDao()
}
