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

import com.elpsykongroo.base.config.RequestConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Slf4j
public class IPUtils {

    private IPUtils() {
        throw new IllegalStateException("Utility class");
    }

    private RequestConfig requestConfig;

    public IPUtils(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
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

    public static boolean isIpv6(String ip) {
        return isValidIPv6(ip) || isValidIPv6CIDR(ip);
    }

    public static boolean isValidIPv6(String ipAddress) {
        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            return ip.getHostAddress().contains(":");
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isValidIPv6CIDR(String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress cidrAddress = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            return cidrAddress.getHostAddress().contains(":") && prefixLength >= 0 && prefixLength <= 128;
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    public static boolean validateHost(String ip) {
        String hostnameRegex = "^((?!-)[A-Za-z0-9-]"
                                + "{1,63}(?<!-)\\.)"
                                + "+[A-Za-z]{2,6}";
        if (isPrivate(ip)) {
            return true;
        }
        Pattern host = Pattern.compile(hostnameRegex);
        return host.matcher(ip).matches();
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
            return false;
        }
    }

    public String accessIP(HttpServletRequest request, String headerType) {
        String[] headers = splitHeader(headerType);
        String ip = getIp(request, headers);
        if (log.isTraceEnabled()) {
            log.trace("IPUtils:{}", ip);
        }
        return ip;
    }

    public static boolean filterByIpOrList(String ip, String accessIP) {
        if (StringUtils.isNotEmpty(ip)) {
            String[] ips = ip.split(",");
            for (String i: ips) {
                if(filterByIp(i, accessIP)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean filterByIp(String ip, String accessIP) {
        try {
            InetAddress inetAddress = InetAddress.getByName(accessIP);
            if (inetAddress.isSiteLocalAddress()) {
                if(log.isTraceEnabled()) {
                    log.trace("ignore private ip");
                }
                return inetAddress.isSiteLocalAddress();
            }
            if (ip.contains("/")) {
                if (IPUtils.isInRange(accessIP, ip)) {
                    return true;
                }
            } else if(validateHost(ip)) {
                InetAddress[] inetAddresses = InetAddress.getAllByName(ip);
                for (InetAddress addr: inetAddresses) {
                    if (accessIP.equals(addr.getHostAddress())) {
                        if(log.isTraceEnabled()) {
                            log.trace("host: {} match, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
                        }
                        return true;
                    } else {
                        if(log.isTraceEnabled()) {
                            log.trace("host: {} mismatch, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
                        }
                    }
                }
            } else if (accessIP.equals(ip)) {
                if(log.isTraceEnabled()) {
                    log.trace("ip match, ip:{}, accessIp:{}", ip, accessIP);
                }
                return true;
            }
        } catch (UnknownHostException e) {
            if (log.isErrorEnabled()) {
                log.error("UnknownHostException");
            }
        }
        return false;
    }

    public String[] splitHeader(String headerType) {
        RequestConfig.Header header = requestConfig.getHeader();
        switch(headerType){
            case "true":
                return header.getBlack().split(",");
            case "false":
                return header.getWhite().split(",");
            case "record":
                return header.getRecord().split(",");
            default:
                return header.getIp().split(",");
        }
    }

    public String getIp(HttpServletRequest request, String[] headers) {
        for (String head: headers) {
            if (StringUtils.isNotBlank(request.getHeader(head))) {
                return request.getHeader(head);
            }
        }
        return request.getRemoteAddr();
    }

    public static Long ipToBigInteger(String ipAddress) {
        long result = 0;
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            if (log.isErrorEnabled()) {
                log.error("UnknownHostException");
            }
            return result;
        }
        byte[] addressBytes = inetAddress.getAddress();
        for (byte b : addressBytes) {
            result = result << 8 | (b & 0xFF);
        }
        return result;
    }


    public static boolean isInRange(String ipAddress, String range) throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(ipAddress);
        String[] rangeParts = range.split("/");
        InetAddress start = InetAddress.getByName(rangeParts[0]);
        int prefixLen = Integer.parseInt(rangeParts[1]);
        byte[] ipBytes = ip.getAddress();
        byte[] startBytes = start.getAddress();

        int bytesToCheck = prefixLen / 8;
        for (int i = 0; i < bytesToCheck; i++) {
            if (ipBytes[i] != startBytes[i]) {
                return false;
            }
        }

        // If there are remaining bits, check them
        int remainingBits = prefixLen % 8;
        if (remainingBits > 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            if ((ipBytes[bytesToCheck] & mask) != (startBytes[bytesToCheck] & mask)) {
                return false;
            }
        }

        return true;
    }
}
