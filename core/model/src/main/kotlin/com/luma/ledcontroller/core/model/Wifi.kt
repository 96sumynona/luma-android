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

package com.luma.ledcontroller.core.model

import kotlinx.serialization.Serializable

/**
 * Wi-Fi credentials a user typed into the editor.
 *
 * This is a transient object: the password never leaves the editor unencrypted and is never stored
 * in this form. Only [WifiCredentialsSummary] is exposed back to the UI.
 */
@Serializable
data class WifiCredentialsDraft(
    val ssid: String = "",
    val password: String = "",
)

/**
 * What the app knows about the stored Wi-Fi credentials.
 *
 * @param ssid the stored network name. Empty when nothing is saved yet.
 * @param hasStoredPassword whether an encrypted password is on disk.
 */
@Serializable
data class WifiCredentialsSummary(
    val ssid: String = "",
    val hasStoredPassword: Boolean = false,
) {
    val isConfigured: Boolean
        get() = ssid.isNotBlank() && hasStoredPassword
}
