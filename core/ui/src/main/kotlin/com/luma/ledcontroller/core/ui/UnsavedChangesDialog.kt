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

package com.luma.ledcontroller.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.luma.ledcontroller.core.designsystem.component.LumaActionDialog
import com.luma.ledcontroller.core.designsystem.component.LumaButton
import com.luma.ledcontroller.core.designsystem.component.LumaOutlinedButton
import com.luma.ledcontroller.core.designsystem.component.LumaTextButton

/**
 * The prompt shown whenever a dirty mode editor would otherwise be left.
 *
 * Every option is explicit: the draft is either discarded, saved, or saved as a new mode. There is
 * no path that silently drops it.
 */
@Composable
fun UnsavedChangesDialog(
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onSaveAsNewMode: () -> Unit,
    onKeepEditing: () -> Unit,
    showSaveAsNew: Boolean = true,
) {
    LumaActionDialog(
        title = stringResource(R.string.core_ui_unsaved_changes_title),
        message = stringResource(R.string.core_ui_unsaved_changes_message),
        onDismissRequest = onKeepEditing,
    ) {
        LumaButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.core_ui_save))
        }
        if (showSaveAsNew) {
            LumaOutlinedButton(onClick = onSaveAsNewMode, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.core_ui_save_as_new_mode))
            }
        }
        LumaTextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.core_ui_discard),
                color = MaterialTheme.colorScheme.error,
            )
        }
        LumaTextButton(onClick = onKeepEditing, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.core_ui_keep_editing),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
