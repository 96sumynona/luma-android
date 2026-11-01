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

import com.luma.ledcontroller.core.data.repository.WifiRepository
import com.luma.ledcontroller.core.model.WifiCredentialsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [WifiRepository] for tests. */
class TestWifiRepository(
    private val failOnSave: Boolean = false,
) : WifiRepository {
    private val store = MutableStateFlow(WifiCredentialsSummary())
    var lastSavedPassword: String? = null

    override val credentials: Flow<WifiCredentialsSummary> = store

    override suspend fun saveCredentials(ssid: String, password: String): Boolean {
        if (failOnSave) return false
        lastSavedPassword = password
        store.value = WifiCredentialsSummary(ssid = ssid, hasStoredPassword = true)
        return true
    }

    override suspend fun encryptedPassword(): String? = lastSavedPassword?.let { "encrypted($it)" }
}
