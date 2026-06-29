package ua.demo.agentlab.ui.discovery.identity;

import java.net.URI;
import java.util.Locale;

public final class RouteCanonicalizer {

    private RouteCanonicalizer() {
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            try {
                URI uri = URI.create(normalized);
                normalized = uri.getPath() == null ? "" : uri.getPath();
            } catch (Exception ignored) {
                return normalized;
            }
        }
        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart);
        }
        int hashStart = normalized.indexOf('#');
        if (hashStart >= 0) {
            normalized = normalized.substring(0, hashStart);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = stripFrontControllerPrefix(normalized);
        normalized = normalized.replaceAll("/+$", "");
        return normalized.isBlank() ? "/" : normalized;
    }

    public static boolean routeEqualsOrSuffix(String left, String right) {
        String normalizedLeft = canonicalize(left);
        String normalizedRight = canonicalize(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return false;
        }
        if (normalizedLeft.equalsIgnoreCase(normalizedRight)) {
            return true;
        }
        return endsWithSegment(normalizedLeft, normalizedRight)
                || endsWithSegment(normalizedRight, normalizedLeft);
    }

    private static String stripFrontControllerPrefix(String route) {
        String normalized = route;
        String lower = normalized.toLowerCase(Locale.ROOT);
        int indexPhp = lower.indexOf("/index.php/");
        if (indexPhp >= 0) {
            return normalized.substring(indexPhp + "/index.php".length());
        }
        if (lower.endsWith("/index.php")) {
            return "/";
        }
        return normalized;
    }

    private static boolean endsWithSegment(String value, String suffix) {
        if (value == null || suffix == null || suffix.isBlank() || "/".equals(suffix)) {
            return false;
        }
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        String normalizedSuffix = suffix.toLowerCase(Locale.ROOT);
        return normalizedValue.endsWith(normalizedSuffix)
                && value.charAt(value.length() - suffix.length()) == '/';
    }
}
