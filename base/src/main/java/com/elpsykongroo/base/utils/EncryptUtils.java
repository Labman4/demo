/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.base.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class EncryptUtils {

    private EncryptUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] encrypt(String plaintext, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            byte[] iv = BytesUtils.generateRandomByte(12);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] ivAndEncryptedData = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, ivAndEncryptedData, 0, iv.length);
            System.arraycopy(encryptedData, 0, ivAndEncryptedData, iv.length, encryptedData.length);
            return ivAndEncryptedData;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(byte[] ciphertext, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, ciphertext, 0, 12);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] plaintextBytes = cipher.doFinal(ciphertext, 12, ciphertext.length - 12);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptAsByte(byte[] ciphertext, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, ciphertext, 0, 12);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] plaintextBytes = cipher.doFinal(ciphertext, 12, ciphertext.length - 12);
            return plaintextBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
