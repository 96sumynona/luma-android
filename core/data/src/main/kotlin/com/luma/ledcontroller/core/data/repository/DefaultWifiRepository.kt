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

import com.luma.ledcontroller.core.common.dispatchers.Dispatcher
import com.luma.ledcontroller.core.common.dispatchers.LumaDispatchers.IO
import com.luma.ledcontroller.core.data.security.CredentialCipher
import com.luma.ledcontroller.core.datastore.LumaPreferencesDataSource
import com.luma.ledcontroller.core.model.WifiCredentialsSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultWifiRepository @Inject constructor(
    private val preferencesDataSource: LumaPreferencesDataSource,
    private val credentialCipher: CredentialCipher,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : WifiRepository {

    override val credentials: Flow<WifiCredentialsSummary> = preferencesDataSource.wifiCredentials

    override suspend fun saveCredentials(ssid: String, password: String): Boolean =
        withContext(ioDispatcher) {
            // Keystore access and encryption are blocking, so they stay on the IO dispatcher and the
            // plaintext never reaches the data store.
            runCatching {
                val encrypted = credentialCipher.encrypt(password)
                preferencesDataSource.setWifiCredentials(ssid = ssid.trim(), encryptedPassword = encrypted)
            }.isSuccess
        }

    override suspend fun encryptedPassword(): String? =
        withContext(ioDispatcher) { preferencesDataSource.encryptedWifiPassword() }
}
