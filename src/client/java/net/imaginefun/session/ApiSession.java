package net.imaginefun.session;

import java.net.URI;

public final class ApiSession {

    private static final String TRUSTED_HOST = "imaginefun.net";
    private static final String TRUSTED_HOST_SUFFIX = "." + TRUSTED_HOST;

    private static volatile String token;
    private static volatile String baseUrl;

    private ApiSession() {
    }

    public static boolean isActive() {
        return token != null;
    }

    public static String getToken() {
        return token;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static boolean isTrustedBaseUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"https".equals(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost();
        return host != null && (host.equals(TRUSTED_HOST) || host.endsWith(TRUSTED_HOST_SUFFIX));
    }

    public static void update(String newToken, String newBaseUrl) {
        token = newToken;
        baseUrl = newBaseUrl;
    }

    public static void clear() {
        token = null;
        baseUrl = null;
    }
}
