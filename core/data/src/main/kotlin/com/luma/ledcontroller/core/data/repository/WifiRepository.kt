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

import com.luma.ledcontroller.core.model.WifiCredentialsSummary
import kotlinx.coroutines.flow.Flow

/**
 * Wi-Fi credentials that will eventually be handed to the controller over BLE.
 *
 * The password is encrypted before it is stored and is never read back into the UI.
 */
interface WifiRepository {
    val credentials: Flow<WifiCredentialsSummary>

    /** Encrypts and stores the credentials. Returns `false` when encryption failed. */
    suspend fun saveCredentials(ssid: String, password: String): Boolean

    /** The stored, encrypted password, for a future provisioning step. */
    suspend fun encryptedPassword(): String?
}
