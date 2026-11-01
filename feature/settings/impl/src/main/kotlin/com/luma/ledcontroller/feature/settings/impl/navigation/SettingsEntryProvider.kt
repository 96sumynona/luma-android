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

package com.luma.ledcontroller.feature.settings.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.luma.ledcontroller.core.navigation.Navigator
import com.luma.ledcontroller.feature.settings.api.navigation.BoardEditorNavKey
import com.luma.ledcontroller.feature.settings.api.navigation.SettingsNavKey
import com.luma.ledcontroller.feature.settings.api.navigation.StripEditorNavKey
import com.luma.ledcontroller.feature.settings.impl.BoardEditorScreen
import com.luma.ledcontroller.feature.settings.impl.SettingsScreen
import com.luma.ledcontroller.feature.settings.impl.StripEditorScreen

/** The Settings tab: boards, then the strips that sit on them. */
fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsScreen(
            onBoardClick = { boardId -> navigator.navigate(BoardEditorNavKey(boardId = boardId)) },
            onStripClick = { stripId, boardId ->
                navigator.navigate(StripEditorNavKey(stripId = stripId, boardId = boardId))
            },
            onAddBoard = { navigator.navigate(BoardEditorNavKey(boardId = null)) },
            onAddStrip = { boardId ->
                navigator.navigate(StripEditorNavKey(stripId = null, boardId = boardId))
            },
        )
    }
    entry<BoardEditorNavKey> { key ->
        BoardEditorScreen(
            boardId = key.boardId,
            onStripClick = { stripId, boardId ->
                navigator.navigate(StripEditorNavKey(stripId = stripId, boardId = boardId))
            },
            onAddStrip = { boardId ->
                navigator.navigate(StripEditorNavKey(stripId = null, boardId = boardId))
            },
        )
    }
    entry<StripEditorNavKey> { key ->
        StripEditorScreen(stripId = key.stripId, boardId = key.boardId)
    }
}
