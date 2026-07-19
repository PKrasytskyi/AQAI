package ua.demo.agentlab.ui.discovery.pagemodel;

import java.util.List;
import java.util.Locale;

public class PageFeatureClassifier {

    public String classify(List<String> capabilities, String url, String title) {
        String text = normalize(String.join(" ", capabilities == null ? List.of() : capabilities) + " " + url + " " + title);
        if (containsAny(text, "authenticated-area", "secure area", "/secure", "logout", "logged in")) {
            return "authenticated-area";
        }
        if (containsAny(text, "dashboard", "/dashboard", "dashboard/index")) {
            return "dashboard";
        }
        if (containsAny(text, "auth", "login", "signin", "password")) {
            return "authentication";
        }
        if (containsAny(text, "cart", "basket", "bag")) {
            return "container";
        }
        if (containsAny(text, "listing", "catalog", "collection", "/collections", "/recruitment", "/vacanc",
                "candidates", "search results")) {
            return "catalog";
        }
        if (containsAny(text, "details", "detail", "record", "profile", "view")) {
            return "details";
        }
        if (containsAny(text, "form", "submit", "create")) {
            return "form";
        }
        return "generic";
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
