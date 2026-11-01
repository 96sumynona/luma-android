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

import com.luma.ledcontroller.core.data.repository.WifiRepository
import com.luma.ledcontroller.core.model.LumaError
import com.luma.ledcontroller.core.model.LumaValidation
import javax.inject.Inject

/**
 * Validates and stores Wi-Fi credentials. The repository encrypts the password with a
 * Keystore-backed key before anything is written to disk.
 */
class SaveWifiCredentialsUseCase @Inject constructor(
    private val wifiRepository: WifiRepository,
) {
    suspend operator fun invoke(ssid: String, password: String): SaveResult<Unit> {
        val errors = LumaValidation.validateWifi(ssid = ssid, password = password)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)

        return if (wifiRepository.saveCredentials(ssid = ssid, password = password)) {
            SaveResult.Saved(Unit)
        } else {
            SaveResult.Invalid(setOf(LumaError.PASSWORD_BLANK))
        }
    }
}
