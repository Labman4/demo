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

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4DecompressorWithLength;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;

public final class BytesUtils {
    private static final SecureRandom random = new SecureRandom();

    private BytesUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] generateRandomByte(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    public static byte[] compress(byte[] data) {
        try {
            LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
            int maxCompressedLength = compressor.maxCompressedLength(data.length);
            byte[] compressedData = new byte[maxCompressedLength];
            int compressedLength = compressor.compress(data, 0, data.length, compressedData, 0, maxCompressedLength);
            return java.util.Arrays.copyOf(compressedData, compressedLength);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decompress(byte[] compressedData) {
        try {
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4FastDecompressor decompressor = factory.fastDecompressor();
            LZ4DecompressorWithLength decompressorWithLength = new LZ4DecompressorWithLength(decompressor);
            return decompressorWithLength.decompress(compressedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] convertToByteArray(Object object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            objectOutputStream.close();

            byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return byteArray;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
