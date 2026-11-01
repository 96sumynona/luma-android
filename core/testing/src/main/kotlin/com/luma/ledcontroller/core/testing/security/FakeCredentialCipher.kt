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

package com.luma.ledcontroller.core.testing.security

import com.luma.ledcontroller.core.data.security.CredentialCipher

/**
 * A reversible stand-in for the Keystore cipher.
 *
 * It is deliberately obvious that the output is not real encryption, so a test can assert that the
 * value that reached storage went through the cipher at all.
 */
class FakeCredentialCipher(
    private val failOnEncrypt: Boolean = false,
) : CredentialCipher {
    val encryptedValues = mutableListOf<String>()

    override fun encrypt(plaintext: String): String {
        if (failOnEncrypt) error("Keystore unavailable")
        val encrypted = "fake-cipher($plaintext)"
        encryptedValues += encrypted
        return encrypted
    }

    override fun decrypt(ciphertext: String): String? =
        ciphertext.removePrefix("fake-cipher(").removeSuffix(")")
}
