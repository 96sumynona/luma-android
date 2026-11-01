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

package com.luma.ledcontroller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.luma.ledcontroller.core.navigation.NavigationState
import com.luma.ledcontroller.core.navigation.rememberNavigationState
import com.luma.ledcontroller.navigation.TOP_LEVEL_NAV_ITEMS
import com.luma.ledcontroller.navigation.TopLevelDestination

/**
 * Remembers the app's navigation state.
 *
 * Each tab keeps its own back stack, which is what lets a mode editor stay open on the Modes tab
 * while the user checks something on Home.
 */
@Composable
fun rememberLumaAppState(): LumaAppState {
    val navigationState = rememberNavigationState(
        startKey = TopLevelDestination.HOME.navKey,
        topLevelKeys = TOP_LEVEL_NAV_ITEMS.keys,
    )
    return remember(navigationState) { LumaAppState(navigationState = navigationState) }
}

@Stable
class LumaAppState(val navigationState: NavigationState)
