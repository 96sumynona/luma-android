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

package com.luma.ledcontroller.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luma.ledcontroller.core.common.dispatchers.Dispatcher
import com.luma.ledcontroller.core.common.dispatchers.LumaDispatchers.IO
import com.luma.ledcontroller.core.model.WifiCredentialsSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Local, app-private preferences.
 *
 * Only the Wi-Fi credentials live here. Everything else is configuration that is structured for the
 * controller, so it lives in the Room database instead.
 *
 * The password is stored already encrypted: this data source never sees, and never writes, a
 * plaintext password.
 */
class LumaPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) {
    val wifiCredentials: Flow<WifiCredentialsSummary> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            WifiCredentialsSummary(
                ssid = preferences[Keys.WIFI_SSID].orEmpty(),
                hasStoredPassword = !preferences[Keys.WIFI_PASSWORD_CIPHER].isNullOrBlank(),
            )
        }

    suspend fun setWifiCredentials(ssid: String, encryptedPassword: String) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[Keys.WIFI_SSID] = ssid
                preferences[Keys.WIFI_PASSWORD_CIPHER] = encryptedPassword
            }
        }
    }

    /** The encrypted password, for a future provisioning step. Never exposed to the UI. */
    suspend fun encryptedWifiPassword(): String? = withContext(ioDispatcher) {
        dataStore.data.first()[Keys.WIFI_PASSWORD_CIPHER]
    }

    suspend fun clearWifiPassword() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences -> preferences.remove(Keys.WIFI_PASSWORD_CIPHER) }
        }
    }

    private object Keys {
        val WIFI_SSID = stringPreferencesKey("wifi_ssid")
        val WIFI_PASSWORD_CIPHER = stringPreferencesKey("wifi_password_cipher")
    }
}
