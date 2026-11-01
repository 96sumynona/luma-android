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

package com.luma.ledcontroller.core.data.di

import com.luma.ledcontroller.core.data.IdGenerator
import com.luma.ledcontroller.core.data.UuidIdGenerator
import com.luma.ledcontroller.core.data.repository.BoardRepository
import com.luma.ledcontroller.core.data.repository.DefaultBoardRepository
import com.luma.ledcontroller.core.data.repository.DefaultModeRepository
import com.luma.ledcontroller.core.data.repository.DefaultScheduleRepository
import com.luma.ledcontroller.core.data.repository.DefaultStripRepository
import com.luma.ledcontroller.core.data.repository.DefaultWifiRepository
import com.luma.ledcontroller.core.data.repository.ModeRepository
import com.luma.ledcontroller.core.data.repository.ScheduleRepository
import com.luma.ledcontroller.core.data.repository.StripRepository
import com.luma.ledcontroller.core.data.repository.WifiRepository
import com.luma.ledcontroller.core.data.security.AndroidKeystoreCredentialCipher
import com.luma.ledcontroller.core.data.security.CredentialCipher
import com.luma.ledcontroller.core.data.transfer.DefaultLumaConfigTransfer
import com.luma.ledcontroller.core.data.transfer.LumaConfigTransfer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindsBoardRepository(repository: DefaultBoardRepository): BoardRepository

    @Binds
    fun bindsStripRepository(repository: DefaultStripRepository): StripRepository

    @Binds
    fun bindsModeRepository(repository: DefaultModeRepository): ModeRepository

    @Binds
    fun bindsScheduleRepository(repository: DefaultScheduleRepository): ScheduleRepository

    @Binds
    fun bindsWifiRepository(repository: DefaultWifiRepository): WifiRepository

    @Binds
    fun bindsCredentialCipher(cipher: AndroidKeystoreCredentialCipher): CredentialCipher

    @Binds
    fun bindsIdGenerator(generator: UuidIdGenerator): IdGenerator

    @Binds
    fun bindsConfigTransfer(transfer: DefaultLumaConfigTransfer): LumaConfigTransfer

    companion object {
        @Provides
        @Singleton
        fun providesClock(): Clock = Clock.System
    }
}
