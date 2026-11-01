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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luma.ledcontroller.core.designsystem.R
import com.luma.ledcontroller.core.designsystem.icon.LumaIcons
import com.luma.ledcontroller.core.designsystem.theme.LumaSurfaceContainer
import com.luma.ledcontroller.core.designsystem.theme.LumaTheme

/**
 * A form dropdown that reads as a dropdown at a glance.
 *
 * The closed control is an outlined field with the selected option's icon, the selection and a
 * trailing chevron, so it never looks like a text input or a chip. Every option carries its own icon
 * and optional supporting line, which makes long lists (modes, strips, boards) scannable.
 *
 * @param label field label, always visible above the control.
 * @param options the available values.
 * @param selected the current value, or `null` when nothing is chosen yet.
 * @param onSelected invoked with the chosen option.
 * @param optionLabel the primary text for an option.
 * @param optionIcon the icon for an option, shown in the field and in the menu.
 * @param optionSupportingText optional second line for an option.
 * @param selectedIcon icon shown in the closed field when an option is selected.
 */
@Composable
fun <T> LumaDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector? = null,
    optionIcon: (T) -> ImageVector? = { null },
    optionSupportingText: (T) -> String? = { null },
    enabled: Boolean = true,
    emptyText: String = stringResource(R.string.core_designsystem_nothing_to_choose),
    placeholder: String = stringResource(R.string.core_designsystem_choose_option),
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable(enabled = enabled) { expanded = true },
                shape = shape,
                color = LumaSurfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selectedIcon != null) {
                        Icon(
                            imageVector = selectedIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = when {
                            selected != null -> optionLabel(selected)
                            options.isEmpty() -> emptyText
                            else -> placeholder
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = LumaIcons.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = LumaSurfaceContainer,
            ) {
                options.forEach { option ->
                    val icon = optionIcon(option)
                    val supportingText = optionSupportingText(option)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(text = optionLabel(option), style = MaterialTheme.typography.bodyLarge)
                                if (supportingText != null) {
                                    Text(
                                        text = supportingText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                        enabled = enabled,
                        leadingIcon = if (icon != null) {
                            {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                        trailingIcon = if (option == selected) {
                            {
                                Icon(
                                    imageVector = LumaIcons.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
fun DropdownPreview() {
    LumaTheme {
        LumaDropdown(
            label = "Mode",
            options = listOf("Amethyst drift", "Sunset"),
            selected = "Amethyst drift",
            onSelected = {},
            optionLabel = { it },
            selectedIcon = LumaIcons.Palette,
            optionIcon = { LumaIcons.Palette },
            modifier = Modifier.padding(16.dp),
        )
    }
}
