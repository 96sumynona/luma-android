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

package com.luma.ledcontroller.core.data.security

/**
 * Encrypts secrets that are stored locally.
 *
 * The only implementation in the app is Keystore backed. Tests use a deterministic fake so the
 * storage layer can be verified without an Android Keystore.
 */
interface CredentialCipher {
    /** Encrypts [plaintext] and returns an opaque, storable string. */
    fun encrypt(plaintext: String): String

    /** Decrypts a value produced by [encrypt], or returns `null` when it cannot be read. */
    fun decrypt(ciphertext: String): String?
}
