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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DomainUtils {

    private DomainUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getParentDomain(String domain) {
        Pattern pattern = Pattern.compile("^(?:https?://)?(?:[^@\\n]+@)?(?:www\\.)?([^:/\\n]+)");
        Matcher matcher = pattern.matcher(domain);
        if (matcher.find()) {
            String[] parts = matcher.group(1).split("\\.");
            if (parts.length < 2) {
                return matcher.group(1);
            } else if (parts.length == 2) {
                return matcher.group(1);
            } else {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
        }
        return null;
    }

    public static String getSubDomain(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost();
            return host.split("\\.")[0];
        } catch (MalformedURLException e) {
            return "";
        }
    }
}
