package ua.demo.agentlab.ui.discovery.runtime;

import java.net.URI;
import java.net.URISyntaxException;

public class RuntimeEventNormalizer {

    public String path(String url) {
        String normalized = clean(url);
        if (normalized.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(normalized);
            String path = uri.getPath();
            String query = uri.getQuery();
            if (path == null || path.isBlank()) {
                return "/";
            }
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (URISyntaxException exception) {
            int protocol = normalized.indexOf("://");
            int pathStart = protocol >= 0 ? normalized.indexOf('/', protocol + 3) : normalized.indexOf('/');
            return pathStart >= 0 ? normalized.substring(pathStart) : normalized;
        }
    }

    public String host(String url) {
        String normalized = clean(url);
        if (normalized.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(normalized);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (URISyntaxException exception) {
            return "";
        }
    }

    public boolean sameHost(String firstUrl, String secondUrl) {
        String first = host(firstUrl);
        String second = host(secondUrl);
        return !first.isBlank() && first.equalsIgnoreCase(second);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
