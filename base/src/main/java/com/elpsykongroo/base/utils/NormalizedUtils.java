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

public final class NormalizedUtils {
    private NormalizedUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String topicNormalize(String str) {
        String normalizedTopic = str.replaceAll("[^\\w.-]", "");
        normalizedTopic = normalizedTopic.replaceAll("-+", "-");
        normalizedTopic = normalizedTopic.replaceAll("\\.+", ".");
        normalizedTopic = normalizedTopic.replaceAll("^-+", "");
        normalizedTopic = normalizedTopic.replaceAll("-+$", "");
        normalizedTopic = normalizedTopic.replaceAll("^\\.+", "");
        normalizedTopic = normalizedTopic.replaceAll("\\.+$", "");
        if (normalizedTopic.length() > 255) {
            normalizedTopic = normalizedTopic.substring(0, 255);
        }
        return  normalizedTopic;
    }}
