package com.elpsykongroo.base.utils;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainUtils {
    private static final Pattern parent = Pattern.compile("(?<=\\.)[^.]+\\.[^.]+$");

    public static String getParentDomain(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost();
            Matcher matcher = parent.matcher(host);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSubDomain(String url) {
        try {
            URL u = new URL(url);
            String host = u.getHost();
            return host.split("\\.")[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
