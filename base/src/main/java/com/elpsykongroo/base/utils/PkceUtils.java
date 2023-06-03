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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
public class PkceUtils {
    private static final SecureRandom random = new SecureRandom();

    private PkceUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] generateRandomByte(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static String generateChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(codeVerifier.getBytes());
            byte[] digest = md.digest();
            String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return codeChallenge;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateVerifier() {
        byte[] bytes = generateRandomByte(128);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return codeVerifier;
    }
}
