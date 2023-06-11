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

import java.util.regex.Pattern;

public final class IPRegexUtils {

    private IPRegexUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean vaildate(String ip) {
        String ipv4Regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        String ipv6Regex="((([0-9a-fA-F]){1,4})\\:){7}([0-9a-fA-F]){1,4}";
        Pattern p4 = Pattern.compile(ipv4Regex);
        Pattern p6 = Pattern.compile(ipv6Regex);
        if(p4.matcher(ip).matches()) {
            return true;
        } else if (p6.matcher(ip).matches()) {
            return true;
        }
        return false;
    }
    public static boolean vaildateHost(String ip) {
        String hostnameRegex = "^((?!-)[A-Za-z0-9-]"
                                + "{1,63}(?<!-)\\.)"
                                + "+[A-Za-z]{2,6}";
        Pattern host = Pattern.compile(hostnameRegex);
        return host.matcher(ip).matches();
    }
}
