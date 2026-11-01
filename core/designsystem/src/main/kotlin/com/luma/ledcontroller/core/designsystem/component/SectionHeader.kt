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

package com.luma.ledcontroller.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme

/**
 * A section heading inside a scrolling screen.
 *
 * Headings are for grouping, cards are for content: the heading itself is not tappable, so it can
 * never look like something that opens a screen.
 */
@Composable
fun LumaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Column(modifier = modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
fun SectionHeaderPreview() {
    LumaTheme {
        LumaSectionHeader(
            title = "Boards",
            supportingText = "One board drives one or more strips.",
        )
    }
}
