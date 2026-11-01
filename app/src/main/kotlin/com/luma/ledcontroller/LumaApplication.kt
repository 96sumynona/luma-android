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

package com.luma.ledcontroller

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import com.luma.ledcontroller.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [Application] class for Luma.
 *
 * There is no sync, network or notification setup: Luma stores its configuration locally and has no
 * background work in this UI-shell phase.
 */
@HiltAndroidApp
class LumaApplication : Application() {

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    override fun onCreate() {
        super.onCreate()
        setStrictModePolicy()
        profileVerifierLogger()
    }

    /**
     * Writes that happen on the main thread and leaked closeables are worth knowing about while
     * developing the configuration screens.
     */
    private fun setStrictModePolicy() {
        if (isDebuggable()) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    private fun isDebuggable(): Boolean =
        0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
}
