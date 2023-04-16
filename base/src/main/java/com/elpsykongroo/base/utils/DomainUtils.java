package com.elpsykongroo.base.utils;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainUtils {

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
