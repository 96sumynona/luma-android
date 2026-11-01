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

package com.luma.ledcontroller.core.database.util

import androidx.room.TypeConverter
import com.luma.ledcontroller.core.model.AnimationKind
import com.luma.ledcontroller.core.model.LedLayout

/** Stores enums by name so a future controller payload can use the same spelling. */
class LumaTypeConverters {
    @TypeConverter
    fun fromLayout(layout: LedLayout): String = layout.name

    @TypeConverter
    fun toLayout(value: String): LedLayout = LedLayout.valueOf(value)

    @TypeConverter
    fun fromAnimationKind(kind: AnimationKind): String = kind.name

    @TypeConverter
    fun toAnimationKind(value: String): AnimationKind = AnimationKind.valueOf(value)
}
