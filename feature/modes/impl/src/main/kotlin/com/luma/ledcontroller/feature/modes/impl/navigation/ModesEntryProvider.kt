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

package com.luma.ledcontroller.feature.modes.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.core.navigation.Navigator
import com.luma.ledcontroller.feature.modes.api.navigation.ModeEditorNavKey
import com.luma.ledcontroller.feature.modes.api.navigation.ModesNavKey
import com.luma.ledcontroller.feature.modes.impl.ModeEditorScreen
import com.luma.ledcontroller.feature.modes.impl.ModesScreen

/** The Modes tab: the library plus the editor pushed above it. */
fun EntryProviderScope<NavKey>.modesEntry(navigator: Navigator) {
    entry<ModesNavKey> {
        ModesScreen(
            onModeClick = { modeId -> navigator.navigate(ModeEditorNavKey(modeId = modeId)) },
            onCreateMode = { navigator.navigate(ModeEditorNavKey(modeId = null)) },
        )
    }
    entry<ModeEditorNavKey> { key ->
        ModeEditorScreen(modeId = key.modeId)
    }
}
