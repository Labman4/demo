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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public final class IPUtils {

    private IPUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean validate(String ip) {
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
    public static boolean validateHost(String ip) {
        String hostnameRegex = "^((?!-)[A-Za-z0-9-]"
                                + "{1,63}(?<!-)\\.)"
                                + "+[A-Za-z]{2,6}";
        if (isPrivate(ip)) {
            return true;
        }
        Pattern host = Pattern.compile(hostnameRegex);
        boolean result = host.matcher(ip).matches();
        return result;
    }

    public static boolean isPrivate(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress instanceof java.net.Inet6Address) {
                return inetAddress.isLoopbackAddress();
            } else {
                return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress();
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
