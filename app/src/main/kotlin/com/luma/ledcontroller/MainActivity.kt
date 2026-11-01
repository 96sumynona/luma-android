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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.metrics.performance.JankStats
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme
import com.luma.ledcontroller.ui.LumaApp
import com.luma.ledcontroller.ui.rememberLumaAppState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The app's single activity.
 *
 * Luma is a fixed dark-theme app, so edge-to-edge is configured once with dark system bars instead
 * of following the system theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Lazily injected so JankStats is only created when the frame tracking is first enabled. */
    @Inject
    lateinit var lazyStats: dagger.Lazy<JankStats>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        // The whole app state is local, so there is nothing to wait for before drawing.
        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            LumaTheme {
                Luma()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
    }
}

@Composable
private fun Luma() {
    val appState = rememberLumaAppState()
    LumaApp(appState = appState)
}
