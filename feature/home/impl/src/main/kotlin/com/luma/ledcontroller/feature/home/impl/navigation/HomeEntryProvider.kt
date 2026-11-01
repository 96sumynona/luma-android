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

package com.luma.ledcontroller.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.core.navigation.Navigator
import com.luma.ledcontroller.feature.home.api.navigation.HomeNavKey
import com.luma.ledcontroller.feature.home.api.navigation.StripDetailNavKey
import com.luma.ledcontroller.feature.home.impl.HomeScreen
import com.luma.ledcontroller.feature.home.impl.StripDetailScreen

/**
 * Home destinations.
 *
 * @param onNavigateToSettings used by the empty state, because a new install has no strips until a
 *   board is configured in Settings. The shell routes it through the same handler as a tab switch.
 */
fun EntryProviderScope<NavKey>.homeEntry(
    navigator: Navigator,
    onNavigateToSettings: () -> Unit,
) {
    entry<HomeNavKey> {
        HomeScreen(
            onStripClick = { stripId -> navigator.navigate(StripDetailNavKey(stripId)) },
            onOpenSettings = onNavigateToSettings,
        )
    }
    entry<StripDetailNavKey> { key ->
        StripDetailScreen(stripId = key.stripId)
    }
}
