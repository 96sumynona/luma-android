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

package com.luma.ledcontroller.core.domain

import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.testing.repository.TestWifiRepository
import com.luma.ledcontroller.core.testing.util.requireSaved
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaveWifiCredentialsUseCaseTest {

    @Test
    fun blankCredentialsAreRejectedBeforeTheyReachStorage() = runTest {
        val repository = TestWifiRepository()
        val useCase = SaveWifiCredentialsUseCase(repository)

        val result = useCase(ssid = "", password = "")

        assertTrue(LumaError.SSID_BLANK in assertIs<SaveResult.Invalid>(result).errors)
        assertTrue(repository.lastSavedPassword == null)
    }

    @Test
    fun validCredentialsAreStored() = runTest {
        val repository = TestWifiRepository()
        val useCase = SaveWifiCredentialsUseCase(repository)

        val result = useCase(ssid = "Home", password = "correct horse")

        result.requireSaved()
        assertTrue(repository.lastSavedPassword == "correct horse")
    }

    @Test
    fun aStorageFailureIsReportedToTheUi() = runTest {
        val useCase = SaveWifiCredentialsUseCase(TestWifiRepository(failOnSave = true))

        val result = useCase(ssid = "Home", password = "correct horse")

        assertIs<SaveResult.Invalid>(result)
    }
}
